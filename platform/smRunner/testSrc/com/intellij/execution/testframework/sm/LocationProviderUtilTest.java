/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.testframework.sm;

import com.intellij.testFramework.UsefulTestCase;

/**
 * @author Roman Chernyatchik
 */
public class LocationProviderUtilTest extends UsefulTestCase {
  public void testExtractProtocol() {
    assertEquals(null,
                 LocationProviderUtil.extractProtocol(""));
    assertEquals(null,
                 LocationProviderUtil.extractProtocol("file:/"));

    assertEquals("file",
                 LocationProviderUtil.extractProtocol("file://"));
    assertEquals("file",
                 LocationProviderUtil.extractProtocol("file:///some/path/file.rb:24"));
    assertEquals("file",
                 LocationProviderUtil.extractProtocol("file://./some/path/file.rb:24"));

    assertEquals("ruby_qn",
                 LocationProviderUtil.extractProtocol("ruby_qn://"));
    assertEquals("ruby_qn",
                 LocationProviderUtil.extractProtocol("ruby_qn://A::B.method"));
  }

  public void testExtractPath() {
    assertEquals(null,
                 LocationProviderUtil.extractPath(""));
    assertEquals(null,
                 LocationProviderUtil.extractPath("file:/"));

    assertEquals("",
                 LocationProviderUtil.extractPath("file://"));
    assertEquals("/some/path/file.rb:24",
                 LocationProviderUtil.extractPath("file:///some/path/file.rb:24"));
    assertEquals("./some/path/file.rb:24",
                 LocationProviderUtil.extractPath("file://./some/path/file.rb:24"));

    assertEquals("",
                 LocationProviderUtil.extractPath("ruby_qn://"));
    assertEquals("A::B.method", 
                 LocationProviderUtil.extractPath("ruby_qn://A::B.method"));
  }
}
