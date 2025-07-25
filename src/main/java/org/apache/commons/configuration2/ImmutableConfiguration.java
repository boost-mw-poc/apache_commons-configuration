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
package org.apache.commons.configuration2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.configuration2.convert.PropertyConverter;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * <p>
 * The main interface for accessing configuration data in a read-only fashion.
 * </p>
 * <p>
 * The major part of the methods defined in this interface deals with accessing properties of various data types. There
 * is a generic {@code getProperty()} method, which returns the value of the queried property in its raw data type.
 * Other getter methods try to convert this raw data type into a specific data type. If this fails, a
 * {@code ConversionException} will be thrown.
 * </p>
 * <p>
 * For most of the property getter methods an overloaded version exists that allows to specify a default value, which
 * will be returned if the queried property cannot be found in the configuration. The behavior of the methods that do
 * not take a default value in case of a missing property is not defined by this interface and depends on a concrete
 * implementation. E.g. the {@link AbstractConfiguration} class, which is the base class of most configuration
 * implementations provided by this package, per default returns <strong>null</strong> if a property is not found, but provides
 * the {@link AbstractConfiguration#setThrowExceptionOnMissing(boolean) setThrowExceptionOnMissing()} method, with which
 * it can be configured to throw a {@code NoSuchElementException} exception in that case. (Note that getter methods for
 * primitive types in {@code AbstractConfiguration} always throw an exception for missing properties because there is no
 * way of overloading the return value.)
 * </p>
 *
 * @since 2.0
 */
public interface ImmutableConfiguration {
    /**
     * Checks if the configuration contains the specified key.
     *
     * @param key the key whose presence in this configuration is to be tested
     * @return {@code true} if the configuration contains a value for this key, {@code false} otherwise
     */
    boolean containsKey(String key);

    /**
     * Tests whether this configuration contains one or more matches to this value. This operation stops at first
     * match but may be more expensive than the {@link #containsKey containsKey} method.
     *
     * @param value value whose presence in this configuration is to be tested
     * @return {@code true} if this configuration maps one or more keys to the specified value, false otherwise.
     * @since 2.11.0
     */
    default boolean containsValue(final Object value) {
        final Iterator<String> keys = getKeys();
        while (keys.hasNext()) {
            if (Objects.equals(value, getProperty(keys.next()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this configuration.
     *
     * @return a set view of the mappings contained in this configuration.
     * @since 2.13.0
     */
    default Set<Map.Entry<String, Object>> entrySet() {
        final LinkedHashSet<Map.Entry<String, Object>> set = new LinkedHashSet<>(size());
        getKeys().forEachRemaining(k -> set.add(ImmutablePair.of(k, getProperty(k))));
        return set;
    }

    /**
     * Performs the given action for each entry in this configuration until all entries have been processed or the action throws an exception. Unless otherwise
     * specified by the implementing class, actions are performed in the order of entry set iteration (if an iteration order is specified.) Exceptions thrown by
     * the action are relayed to the caller.
     *
     * <pre> {@code
     * for (Map.Entry<K, V> entry : map.entrySet())
     *     action.accept(entry.getKey(), entry.getValue());
     * }</pre>
     * <p>
     * The default implementation makes no guarantees about synchronization or atomicity properties of this method. Any implementation providing atomicity
     * guarantees must override this method and document its concurrency properties.
     * </p>
     *
     * @param action The action to be performed for each entry.
     * @throws NullPointerException            if the specified action is null.
     * @throws ConcurrentModificationException if an entry is found to be removed during iteration.
     * @since 2.13.0
     */
    default void forEach(final BiConsumer<String, Object> action) {
        Objects.requireNonNull(action);
        for (final Map.Entry<String, Object> entry : entrySet()) {
            String k;
            Object v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (final IllegalStateException e) {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(e);
            }
            action.accept(k, v);
        }
    }

    /**
     * Gets an object of the specified type associated with the given configuration key. If the key doesn't map to an
     * existing object, the method returns null unless {@link AbstractConfiguration#isThrowExceptionOnMissing()} is set to
     * {@code true}.
     *
     * @param <T> the target type of the value
     * @param cls the target class of the value
     * @param key the key of the value
     * @return the value of the requested type for the key
     * @throws java.util.NoSuchElementException if the key doesn't map to an existing object and
     *         {@code throwExceptionOnMissing=true}
     * @throws org.apache.commons.configuration2.ex.ConversionException if the value is not compatible with the requested
     *         type
     * @since 2.0
     */
    <T> T get(Class<T> cls, String key);

    /**
     * Gets an object of the specified type associated with the given configuration key using a default value. If the key
     * doesn't map to an existing object, the default value is returned.
     *
     * @param <T> the target type of the value
     * @param cls the target class of the value
     * @param key the key of the value
     * @param defaultValue the default value
     * @return the value of the requested type for the key
     * @throws org.apache.commons.configuration2.ex.ConversionException if the value is not compatible with the requested
     *         type
     *
     * @since 2.0
     */
    <T> T get(Class<T> cls, String key, T defaultValue);

    /**
     * Gets an array of typed objects associated with the given configuration key. If the key doesn't map to an existing
     * object, an empty list is returned.
     *
     * @param cls the type expected for the elements of the array
     * @param key The configuration key.
     * @return The associated array if the key is found, and the value compatible with the type specified.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not
     *         compatible with a list of the specified class.
     *
     * @since 2.0
     */
    Object getArray(Class<?> cls, String key);

    /**
     * Gets an array of typed objects associated with the given configuration key. If the key doesn't map to an existing
     * object, the default value is returned.
     *
     * @param cls the type expected for the elements of the array
     * @param key the configuration key.
     * @param defaultValue the default value
     * @return The associated array if the key is found, and the value compatible with the type specified.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not
     *         compatible with an array of the specified class.
     * @throws IllegalArgumentException if the default value is not an array of the specified type
     * @since 2.0
     * @deprecated This method should not be used any more because its signature does not allow type-safe invocations; use
     *             {@link #get(Class, String, Object)} instead which offers the same functionality; for instance, to query
     *             for an array of ints use {@code int[] result = config.get(int[].class, "myArrayKey", someDefault);}.
     */
    @Deprecated
    Object getArray(Class<?> cls, String key, Object defaultValue);

    /**
     * Gets a {@link BigDecimal} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated BigDecimal if key is found and has valid format
     */
    BigDecimal getBigDecimal(String key);

    /**
     * Gets a {@link BigDecimal} associated with the given configuration key. If the key doesn't map to an existing object,
     * the default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated BigDecimal if key is found and has valid format, default value otherwise.
     */
    BigDecimal getBigDecimal(String key, BigDecimal defaultValue);

    /**
     * Gets a {@link BigInteger} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated BigInteger if key is found and has valid format
     */
    BigInteger getBigInteger(String key);

    /**
     * Gets a {@link BigInteger} associated with the given configuration key. If the key doesn't map to an existing object,
     * the default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated BigInteger if key is found and has valid format, default value otherwise.
     */
    BigInteger getBigInteger(String key, BigInteger defaultValue);

    /**
     * Gets a boolean associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated boolean.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Boolean.
     */
    boolean getBoolean(String key);

    /**
     * Gets a boolean associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated boolean.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Boolean.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Gets a {@link Boolean} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated boolean if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Boolean.
     */
    Boolean getBoolean(String key, Boolean defaultValue);

    /**
     * Gets a byte associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated byte.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Byte.
     */
    byte getByte(String key);

    /**
     * Gets a byte associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated byte.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Byte.
     */
    byte getByte(String key, byte defaultValue);

    /**
     * Gets a {@link Byte} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated byte if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Byte.
     */
    Byte getByte(String key, Byte defaultValue);

    /**
     * Gets a collection of typed objects associated with the given configuration key. This method works like
     * {@link #getCollection(Class, String, Collection, Collection)} passing in <strong>null</strong> as default value.
     *
     * @param <T> the element type of the result list
     * @param cls the element class of the result list
     * @param key the configuration key
     * @param target the target collection (may be <strong>null</strong>)
     * @return the collection to which data was added
     * @throws org.apache.commons.configuration2.ex.ConversionException if the conversion is not possible
     * @since 2.0
     */
    <T> Collection<T> getCollection(Class<T> cls, String key, Collection<T> target);

    /**
     * Gets a collection of typed objects associated with the given configuration key using the values in the specified
     * default collection if the key does not map to an existing object. This method is similar to {@code getList()},
     * however, it allows specifying a target collection. Results are added to this collection. This is useful if the data
     * retrieved should be added to a specific kind of collection, for example a set to remove duplicates. The return value is as
     * follows:
     * <ul>
     * <li>If the key does not map to an existing object and the default value is <strong>null</strong>, the method returns
     * <strong>null</strong>.</li>
     * <li>If the target collection is not <strong>null</strong> and data has been added (either from the resolved property value or
     * from the default collection), the target collection is returned.</li>
     * <li>If the target collection is <strong>null</strong> and data has been added (either from the resolved property value or from
     * the default collection), return value is the target collection created by this method.</li>
     * </ul>
     *
     * @param <T> the element type of the result list
     * @param cls the element class of the result list
     * @param key the configuration key
     * @param target the target collection (may be <strong>null</strong>)
     * @param defaultValue the default value (may be <strong>null</strong>)
     * @return the collection to which data was added
     * @throws org.apache.commons.configuration2.ex.ConversionException if the conversion is not possible
     * @since 2.0
     */
    <T> Collection<T> getCollection(Class<T> cls, String key, Collection<T> target, Collection<T> defaultValue);

    /**
     * Gets a double associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated double.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Double.
     */
    double getDouble(String key);

    /**
     * Gets a double associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated double.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Double.
     */
    double getDouble(String key, double defaultValue);

    /**
     * Gets a {@link Double} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated double if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Double.
     */
    Double getDouble(String key, Double defaultValue);

    /**
     * Gets a {@link Duration} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated Duration if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Duration.
     * @since 2.8.0
     */
    default Duration getDuration(final String key) {
        final String string = getString(key);
        if (string == null) {
            throw new NoSuchElementException(key);
        }
        return PropertyConverter.toDuration(string);
    }

    /**
     * Gets a {@link Duration} associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated Duration if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Duration.
     * @since 2.8.0
     */
    default Duration getDuration(final String key, final Duration defaultValue) {
        final Object value = getProperty(key);
        return value == null ? defaultValue : PropertyConverter.toDuration(value);
    }

    /**
     * Gets the value of a string property that is stored in encoded form in this configuration using a default
     * {@code ConfigurationDecoder}. This method works like the method with the same name, but it uses a default
     * {@code ConfigurationDecoder} associated with this configuration. It depends on a specific implementation how this
     * default decoder is obtained.
     *
     * @param key the configuration key
     * @return the plain string value of the specified encoded property
     */
    String getEncodedString(String key);

    /**
     * Gets the value of a string property that is stored in encoded form in this configuration. This method obtains the
     * value of the string property identified by the given key. This value is then passed to the provided
     * {@code ConfigurationDecoder}. The value returned by the {@code ConfigurationDecoder} is passed to the caller. If the
     * key is not associated with a value, the decoder is not invoked; depending on this configuration's settings either
     * <strong>null</strong> is returned or an exception is thrown.
     *
     * @param key the configuration key
     * @param decoder the {@code ConfigurationDecoder} (must not be <strong>null</strong>)
     * @return the plain string value of the specified encoded property
     * @throws IllegalArgumentException if a <strong>null</strong> decoder is passed
     */
    String getEncodedString(String key, ConfigurationDecoder decoder);

    /**
     * Gets an enum associated with the given configuration key.
     *
     * @param <T> The enum type whose constant is to be returned.
     * @param enumType the {@code Class} object of the enum type from which to return a constant
     * @param key The configuration key.
     * @return The associated enum.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         String.
     * @since 2.8.0
     */
    default <T extends Enum<T>> T getEnum(final String key, final Class<T> enumType) {
        try {
            return Enum.valueOf(enumType, getString(key));
        } catch (final IllegalArgumentException e) {
            throw new ConversionException(e);
        }
    }

    /**
     * Gets the enum associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param <T> The enum type whose constant is to be returned.
     * @param key The configuration key.
     * @param enumType the {@code Class} object of the enum type from which to return a constant
     * @param defaultValue The default value.
     * @return The associated enum if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Enum.
     * @since 2.8.0
     */
    default <T extends Enum<T>> T getEnum(final String key, final Class<T> enumType, final T defaultValue) {
        final String strValue = getString(key, null);
        if (strValue == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, strValue);
        } catch (final IllegalArgumentException e) {
            throw new ConversionException(e);
        }
    }

    /**
     * Gets a float associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated float.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Float.
     */
    float getFloat(String key);

    /**
     * Gets a float associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated float.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Float.
     */
    float getFloat(String key, float defaultValue);

    /**
     * Gets a {@link Float} associated with the given configuration key. If the key doesn't map to an existing object, the
     * default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated float if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Float.
     */
    Float getFloat(String key, Float defaultValue);

    /**
     * Gets a int associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated int.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Integer.
     */
    int getInt(String key);

    /**
     * Gets a int associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated int.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Integer.
     */
    int getInt(String key, int defaultValue);

    /**
     * Gets an {@link Integer} associated with the given configuration key. If the key doesn't map to an existing object,
     * the default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated int if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Integer.
     */
    Integer getInteger(String key, Integer defaultValue);

    /**
     * Gets the list of the keys contained in the configuration. The returned iterator can be used to obtain all defined
     * keys. It does not allow removing elements from this configuration via its {@code remove()} method. Note that the keys
     * of this configuration are returned in a form, so that they can be directly evaluated; escaping of special characters
     * (if necessary) has already been performed.
     *
     * @return An Iterator.
     */
    Iterator<String> getKeys();

    /**
     * Gets the list of the keys contained in the configuration that match the specified prefix. For instance, if the
     * configuration contains the following keys:<br>
     * {@code db.user, db.pwd, db.url, window.xpos, window.ypos},<br>
     * an invocation of {@code getKeys("db");}<br>
     * will return the keys below:<br>
     * {@code db.user, db.pwd, db.url}.<br>
     * Note that the prefix itself is included in the result set if there is a matching key. The exact behavior - how the
     * prefix is actually interpreted - depends on a concrete implementation.
     *
     * @param prefix The prefix to test against.
     * @return An Iterator of keys that match the prefix.
     * @see #getKeys()
     */
    Iterator<String> getKeys(String prefix);

    /**
     * Gets the list of the keys contained in the configuration that match the specified prefix. For instance, if the
     * configuration contains the following keys:<br>
     * {@code db@user, db@pwd, db@url, window.xpos, window.ypos},<br>
     * an invocation of {@code getKeys("db","@");}<br>
     * will return the keys below:<br>
     * {@code db@user, db@pwd, db@url}.<br>
     * Note that the prefix itself is included in the result set if there is a matching key. The exact behavior - how the
     * prefix is actually interpreted - depends on a concrete implementation.
     *
     * @param prefix The prefix to test against.
     * @param delimiter The prefix delimiter.
     * @return An Iterator of keys that match the prefix.
     * @see #getKeys()
     * @since 2.10.0
     */
    default Iterator<String> getKeys(final String prefix, final String delimiter) {
        return null;
    }

    /**
     * Gets a list of typed objects associated with the given configuration key returning a null if the key doesn't map to
     * an existing object.
     *
     * @param <T> the type expected for the elements of the list
     * @param cls the class expected for the elements of the list
     * @param key The configuration key.
     * @return The associated list if the key is found.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not
     *         compatible with a list of the specified class.
     *
     * @since 2.0
     */
    <T> List<T> getList(Class<T> cls, String key);

    /**
     * Gets a list of typed objects associated with the given configuration key returning the specified default value if the
     * key doesn't map to an existing object. This method recursively retrieves all values stored for the passed in key,
     * i.e. if one of these values is again a complex object like an array or a collection (which may be the case for some
     * concrete subclasses), all values are extracted and added to the resulting list - performing a type conversion if
     * necessary.
     *
     * @param <T> the type expected for the elements of the list
     * @param cls the class expected for the elements of the list
     * @param key the configuration key.
     * @param defaultValue the default value.
     * @return The associated List.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not
     *         compatible with a list of the specified class.
     *
     * @since 2.0
     */
    <T> List<T> getList(Class<T> cls, String key, List<T> defaultValue);

    /**
     * Gets a List of the values associated with the given configuration key. This method is different from the generic
     * {@code getList()} method in that it does not recursively obtain all values stored for the specified property key.
     * Rather, only the first level of the hierarchy is processed. So the resulting list may contain complex objects like
     * arrays or collections - depending on the storage structure used by a concrete subclass. If the key doesn't map to an
     * existing object, an empty List is returned.
     *
     * @param key The configuration key.
     * @return The associated List.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         List.
     */
    List<Object> getList(String key);

    /**
     * Gets a List of strings associated with the given configuration key. If the key doesn't map to an existing object, the
     * default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated List of strings.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         List.
     * @see #getList(Class, String, List)
     */
    List<Object> getList(String key, List<?> defaultValue);

    /**
     * Gets a long associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated long.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Long.
     */
    long getLong(String key);

    /**
     * Gets a long associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated long.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Long.
     */
    long getLong(String key, long defaultValue);

    /**
     * Gets a {@link Long} associated with the given configuration key. If the key doesn't map to an existing object, the
     * default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated long if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Long.
     */
    Long getLong(String key, Long defaultValue);

    /**
     * Gets a list of properties associated with the given configuration key. This method expects the given key to have an
     * arbitrary number of String values, each of which is of the form {@code key=value}. These strings are split at the
     * equals sign, and the key parts will become keys of the returned {@code Properties} object, the value parts become
     * values.
     *
     * @param key The configuration key.
     * @return The associated properties if key is found.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         String/List.
     * @throws IllegalArgumentException if one of the tokens is malformed (does not contain an equals sign).
     */
    Properties getProperties(String key);

    /**
     * Gets a property from the configuration. This is the most basic get method for retrieving values of properties. In a
     * typical implementation of the {@code Configuration} interface the other get methods (that return specific data types)
     * will internally make use of this method. On this level variable substitution is not yet performed. The returned
     * object is an internal representation of the property value for the passed in key. It is owned by the
     * {@code Configuration} object. So a caller should not modify this object. It cannot be guaranteed that this object
     * will stay constant over time (i.e. further update operations on the configuration may change its internal state).
     *
     * @param key property to retrieve
     * @return the value to which this configuration maps the specified key, or null if the configuration contains no
     *         mapping for this key.
     */
    Object getProperty(String key);

    /**
     * Gets a short associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated short.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Short.
     */
    short getShort(String key);

    /**
     * Gets a short associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated short.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Short.
     */
    short getShort(String key, short defaultValue);

    /**
     * Gets a {@link Short} associated with the given configuration key. If the key doesn't map to an existing object, the
     * default value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated short if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         Short.
     */
    Short getShort(String key, Short defaultValue);

    /**
     * Gets a string associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated string.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         String.
     */
    String getString(String key);

    /**
     * Gets a string associated with the given configuration key. If the key doesn't map to an existing object, the default
     * value is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated string if key is found and has valid format, default value otherwise.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         String.
     */
    String getString(String key, String defaultValue);

    /**
     * Gets an array of strings associated with the given configuration key. If the key doesn't map to an existing object an
     * empty array is returned
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     * @throws org.apache.commons.configuration2.ex.ConversionException is thrown if the key maps to an object that is not a
     *         String/List of Strings.
     */
    String[] getStringArray(String key);

    /**
     * Return a decorator immutable Configuration containing every key from the current Configuration that starts with the
     * specified prefix. The prefix is removed from the keys in the subset. For example, if the configuration contains the
     * following properties:
     *
     * <pre>
     *    prefix.number = 1
     *    prefix.string = Apache
     *    prefixed.foo = bar
     *    prefix = Jakarta
     * </pre>
     *
     * the immutable Configuration returned by {@code subset("prefix")} will contain the properties:
     *
     * <pre>
     *    number = 1
     *    string = Apache
     *    = Jakarta
     * </pre>
     *
     * (The key for the value "Jakarta" is an empty string)
     *
     * @param prefix The prefix used to select the properties.
     * @return a subset immutable configuration
     */
    ImmutableConfiguration immutableSubset(String prefix);

    /**
     * Checks if the configuration is empty.
     *
     * @return {@code true} if the configuration contains no property, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Returns the number of keys stored in this configuration. Note that a concrete implementation is not guaranteed to be
     * efficient; for some implementations it may be expensive to determine the size. Especially, if you just want to check
     * whether a configuration is empty, it is preferable to use the {@link #isEmpty()} method.
     *
     * @return the number of keys stored in this configuration
     */
    int size();

}
