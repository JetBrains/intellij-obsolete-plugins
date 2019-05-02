/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.struts.dom;

import com.intellij.struts.dom.converters.MemSizeConverter;
import junit.framework.TestCase;

/**
 * @author Dmitry Avdeev
 */
public class ConvertersTest extends TestCase {

  public void testMemSizeConverter() {
    final MemSizeConverter converter = new MemSizeConverter();
    assertNull(converter.fromString(null, null));
    assertNull(converter.fromString("", null));
    assertNull(converter.fromString("a", null));
    assertNull(converter.fromString("ba", null));
    assertEquals(new Long(1), converter.fromString("1", null));
    assertEquals(new Long(10000), converter.fromString("10K", null));
    assertEquals(new Long(3000000), converter.fromString("3M", null));
    assertEquals(new Long(34353000000000L), converter.fromString("34353G", null));
  }
}
