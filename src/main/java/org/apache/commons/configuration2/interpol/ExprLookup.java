/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.interpol;

import java.util.ArrayList;
import java.util.Objects;

import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.io.ConfigurationLogger;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

/**
 * Lookup that allows expressions to be evaluated.
 *
 * <pre>
 * ExprLookup.Variables vars = new ExprLookup.Variables();
 * vars.add(new ExprLookup.Variable("String", org.apache.commons.lang.StringUtils.class));
 * vars.add(new ExprLookup.Variable("Util", new Utility("Hello")));
 * vars.add(new ExprLookup.Variable("System", "Class:java.lang.System"));
 * XMLConfiguration config = new XMLConfiguration(TEST_FILE);
 * config.setLogger(log);
 * ExprLookup lookup = new ExprLookup(vars);
 * lookup.setConfiguration(config);
 * String str = lookup.lookup("'$[element] ' + String.trimToEmpty('$[space.description]')");
 * </pre>
 *
 * In the example above TEST_FILE contains XML that looks like:
 *
 * <pre>
 * &lt;configuration&gt;
 *   &lt;element&gt;value&lt;/element&gt;
 *   &lt;space xml:space="preserve"&gt;
 *     &lt;description xml:space="default"&gt;     Some text      &lt;/description&gt;
 *   &lt;/space&gt;
 * &lt;/configuration&gt;
 * </pre>
 *
 * The result will be "value Some text".
 *
 * This lookup uses Apache Commons Jexl and requires that the dependency be added to any projects which use this.
 *
 * @since 1.7
 */
public class ExprLookup implements Lookup {
    /**
     * The key and corresponding object that will be made available to the JexlContext for use in expressions.
     */
    public static class Variable {
        /** The name to be used in expressions. */
        private String key;

        /** The object to be accessed in expressions. */
        private Object value;

        /**
         * Constructs a new instance.
         */
        public Variable() {
        }

        /**
         * Constructs a new instance.
         *
         * @param name The name to be used in expressions.
         * @param value The object to be accessed in expressions.
         */
        public Variable(final String name, final Object value) {
            setName(name);
            setValue(value);
        }

        /**
         * Gets the name to be used in expressions.
         *
         * @return the name to be used in expressions.
         */
        public String getName() {
            return key;
        }

        /**
         * Sets the value to be used in expressions.
         *
         * @return the value to be used in expressions.
         */
        public Object getValue() {
            return value;
        }

        /**
         * Sets the name to be used in expressions.
         *
         * @param name the name to be used in expressions.
         */
        public void setName(final String name) {
            this.key = name;
        }

        /**
         * Sets the value to be used in expressions.
         *
         * @param value The object to be accessed in expressions.
         * @throws ConfigurationRuntimeException Wraps an exception creating the value.
         */
        public void setValue(final Object value) throws ConfigurationRuntimeException {
            try {
                if (!(value instanceof String)) {
                    this.value = value;
                    return;
                }
                final String val = (String) value;
                final String name = StringUtils.removeStartIgnoreCase(val, CLASS);
                final Class<?> clazz = ClassUtils.getClass(name);
                if (name.length() == val.length()) {
                    this.value = clazz.getConstructor().newInstance();
                } else {
                    this.value = clazz;
                }
            } catch (final Exception e) {
                throw new ConfigurationRuntimeException("Unable to create " + value, e);
            }

        }
    }

    /**
     * List wrapper used to allow the Variables list to be created as beans in DefaultConfigurationBuilder.
     */
    public static class Variables extends ArrayList<Variable> {
        /**
         * The serial version UID.
         */
        private static final long serialVersionUID = 20111205L;

        /**
         * Creates a new empty instance of {@code Variables}.
         */
        public Variables() {
        }

        /**
         * Creates a new instance of {@code Variables} and copies the content of the given object.
         *
         * @param vars the {@code Variables} object to be copied
         */
        public Variables(final Variables vars) {
            super(vars);
        }

        /**
         * Gets the variable or null if empty.
         *
         * @return the variable or null if empty.
         */
        public Variable getVariable() {
            return !isEmpty() ? get(size() - 1) : null;
        }

    }

    /** Prefix to identify a Java Class object */
    private static final String CLASS = "Class:";

    /** The default prefix for subordinate lookup expressions */
    private static final String DEFAULT_PREFIX = "$[";

    /** The default suffix for subordinate lookup expressions */
    private static final String DEFAULT_SUFFIX = "]";

    /** The ConfigurationInterpolator used by this object. */
    private ConfigurationInterpolator interpolator;

    /** The StringSubstitutor for performing replace operations. */
    private StringSubstitutor substitutor;

    /** The logger used by this instance. */
    private ConfigurationLogger logger;

    /** The engine. */
    private final JexlEngine engine = new JexlEngine();

    /** The variables maintained by this object. */
    private Variables variables;

    /** The String to use to start subordinate lookup expressions */
    private String prefixMatcher = DEFAULT_PREFIX;

    /** The String to use to terminate subordinate lookup expressions */
    private String suffixMatcher = DEFAULT_SUFFIX;

    /**
     * Constructs a new instance. Will get used when the Lookup is constructed via configuration.
     */
    public ExprLookup() {
    }

    /**
     * Constructor for use by applications.
     *
     * @param list The list of objects to be accessible in expressions.
     */
    public ExprLookup(final Variables list) {
        setVariables(list);
    }

    /**
     * Constructor for use by applications.
     *
     * @param list The list of objects to be accessible in expressions.
     * @param prefix The prefix to use for subordinate lookups.
     * @param suffix The suffix to use for subordinate lookups.
     */
    public ExprLookup(final Variables list, final String prefix, final String suffix) {
        this(list);
        setVariablePrefixMatcher(prefix);
        setVariableSuffixMatcher(suffix);
    }

    /**
     * Creates a new {@code JexlContext} and initializes it with the variables managed by this Lookup object.
     *
     * @return the newly created context
     */
    private JexlContext createContext() {
        final JexlContext ctx = new MapContext();
        initializeContext(ctx);
        return ctx;
    }

    /**
     * Gets the {@code ConfigurationInterpolator} used by this object.
     *
     * @return the {@code ConfigurationInterpolator}
     * @since 2.0
     */
    public ConfigurationInterpolator getInterpolator() {
        return interpolator;
    }

    /**
     * Gets the logger used by this object.
     *
     * @return the {@code Log}
     * @since 2.0
     */
    public ConfigurationLogger getLogger() {
        return logger;
    }

    /**
     * Gets the list of Variables that are accessible within expressions. This method returns a copy of the variables
     * managed by this lookup; so modifying this object has no impact on this lookup.
     *
     * @return the List of Variables that are accessible within expressions.
     */
    public Variables getVariables() {
        return new Variables(variables);
    }

    /**
     * Initializes the specified context with the variables managed by this Lookup object.
     *
     * @param ctx the context to be initialized
     */
    private void initializeContext(final JexlContext ctx) {
        variables.forEach(var -> ctx.set(var.getName(), var.getValue()));
    }

    /**
     * Creates a {@code StringSubstitutor} object which uses the passed in {@code ConfigurationInterpolator} as lookup
     * object.
     *
     * @param ip the {@code ConfigurationInterpolator} to be used
     */
    private void installSubstitutor(final ConfigurationInterpolator ip) {
        if (ip == null) {
            substitutor = null;
        } else {
            final StringLookup variableResolver = key -> Objects.toString(ip.resolve(key), null);
            substitutor = new StringSubstitutor(variableResolver, prefixMatcher, suffixMatcher, StringSubstitutor.DEFAULT_ESCAPE);
        }
    }

    /**
     * Evaluates the expression.
     *
     * @param var The expression.
     * @return The String result of the expression.
     */
    @Override
    public String lookup(final String var) {
        if (substitutor == null) {
            return var;
        }

        String result = substitutor.replace(var);
        try {
            final Expression exp = engine.createExpression(result);
            final Object exprResult = exp.evaluate(createContext());
            result = exprResult != null ? String.valueOf(exprResult) : null;
        } catch (final Exception e) {
            final ConfigurationLogger l = getLogger();
            if (l != null) {
                l.debug("Error encountered evaluating " + result + ": " + e);
            }
        }

        return result;
    }

    /**
     * Sets the {@code ConfigurationInterpolator} to be used by this object.
     *
     * @param interpolator the {@code ConfigurationInterpolator} (may be <strong>null</strong>)
     * @since 2.0
     */
    public void setInterpolator(final ConfigurationInterpolator interpolator) {
        this.interpolator = interpolator;
        installSubstitutor(interpolator);
    }

    /**
     * Sets the logger to be used by this object. If no logger is passed in, no log output is generated.
     *
     * @param logger the {@code Log}
     * @since 2.0
     */
    public void setLogger(final ConfigurationLogger logger) {
        this.logger = logger;
    }

    /**
     * Sets the prefix to use to identify subordinate expressions. This cannot be the same as the prefix used for the primary
     * expression.
     *
     * @param prefix The String identifying the beginning of the expression.
     */
    public void setVariablePrefixMatcher(final String prefix) {
        prefixMatcher = prefix;
    }

    /**
     * Add the Variables that will be accessible within expressions.
     *
     * @param list The list of Variables.
     */
    public void setVariables(final Variables list) {
        variables = new Variables(list);
    }

    /**
     * Sets the suffix to use to identify subordinate expressions. This cannot be the same as the suffix used for the primary
     * expression.
     *
     * @param suffix The String identifying the end of the expression.
     */
    public void setVariableSuffixMatcher(final String suffix) {
        suffixMatcher = suffix;
    }
}
