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
package org.apache.commons.configuration2.io;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.net.URL;

import org.apache.commons.configuration2.ConfigurationAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@code ProvidedURLLocationStrategy}.
 */
public class TestProvidedURLLocationStrategy {
    /** The strategy to be tested. */
    private ProvidedURLLocationStrategy strategy;

    @BeforeEach
    public void setUp() throws Exception {
        strategy = new ProvidedURLLocationStrategy();
    }

    /**
     * Tests a failed locate() operation.
     */
    @Test
    void testLocateFail() {
        final FileSystem fs = mock(FileSystem.class);
        final FileLocator locator = FileLocatorUtils.fileLocator().basePath("somePath").fileName("someFile.xml").create();
        assertNull(strategy.locate(fs, locator));
    }

    /**
     * Tests a successful locate() operation.
     */
    @Test
    void testLocateSuccess() {
        final FileSystem fs = mock(FileSystem.class);
        final URL url = ConfigurationAssert.getTestURL("test.xml");
        final FileLocator locator = FileLocatorUtils.fileLocator().sourceURL(url).create();
        assertSame(url, strategy.locate(fs, locator));
    }
}
