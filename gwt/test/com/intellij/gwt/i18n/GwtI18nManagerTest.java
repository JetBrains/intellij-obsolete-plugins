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

package com.intellij.gwt.i18n;

import com.intellij.gwt.GwtTestCase;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.search.GlobalSearchScope;

public class GwtI18nManagerTest extends GwtTestCase {
  private GwtI18nManager myManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myManager = new GwtI18nManagerImpl(myProject);
  }


  @Override
  protected void tearDown() throws Exception {
    myManager = null;
    super.tearDown();
  }

  public void testSingleSourceRoot() {
    addGwtModule("/i18n/manager", "src");
    final PsiClass aClass = myJavaFacade.findClass("client.MyConstants", GlobalSearchScope.allScope(myProject));
    assertNotNull(aClass);
    final PropertiesFile[] files = myManager.getPropertiesFiles(aClass);
    assertEquals(2, files.length);
    assertSame(aClass, myManager.getPropertiesInterface(files[0]));
    assertSame(aClass, myManager.getPropertiesInterface(files[1]));

    final PsiMethod method1 = assertOneElement(aClass.findMethodsByName("prop1", false));
    final IProperty[] properties1 = myManager.getProperties(method1);
    assertEquals(2, properties1.length);
    assertSame(method1, myManager.getMethod(properties1[0]));
    assertSame(method1, myManager.getMethod(properties1[1]));

    final PsiMethod method2 = assertOneElement(aClass.findMethodsByName("prop2", false));
    final IProperty[] properties2 = myManager.getProperties(method2);
    assertEquals("value3", assertOneElement(properties2).getValue());
    assertSame(method2, myManager.getMethod(properties2[0]));
  }

  public void testTwoSourceRoots() {
    addGwtModule("/i18n/manager", "src", "resources");
    final PsiClass aClass = myJavaFacade.findClass("client.Res", GlobalSearchScope.allScope(myProject));
    assertNotNull(aClass);
    final PropertiesFile file = assertOneElement(myManager.getPropertiesFiles(aClass));
    assertEquals("Res.properties", file.getName());
    assertSame(aClass, myManager.getPropertiesInterface(file));

    final IProperty property = assertOneElement(file.getProperties());
    final PsiMethod method = assertOneElement(aClass.getMethods());
    assertSame(method, myManager.getMethod(property));
    assertSame(property, assertOneElement(myManager.getProperties(method)));
  }

  public void testImplicitPropertyUsage() {
    addGwtModule("/i18n/manager", "src");
    final PsiClass aClass = myJavaFacade.findClass("client.MyConstants", GlobalSearchScope.allScope(myProject));
    assertNotNull(aClass);
    final PsiMethod method1 = assertOneElement(aClass.findMethodsByName("prop1", false));
    final IProperty[] properties1 = myManager.getProperties(method1);
    assertTrue(properties1.length > 0);

    // Properties backing a Constants/Messages interface method must be treated as implicitly used (IDEA-109938).
    final GwtImplicitPropertyUsageProvider provider = new GwtImplicitPropertyUsageProvider();
    for (IProperty property : properties1) {
      assertTrue("Property '" + property.getKey() + "' should be reported as used",
                 provider.isUsed((Property)property));
    }
  }

  public void testConvertPropertyName2Method() {
    assertConverts("A", "A");
    assertConverts("a", "a");
    assertConverts("aaa.bac", "aaaBac");
    assertConverts("aaa..bac", "aaaBac");
    assertConverts("throw", "getPropertyThrow");
    assertConverts("0", "getProperty");
    assertConverts("a.1.b", "aB");
    assertConverts("", "getProperty");
  }

  public void testSuggestPropertyKey() {
    assertSuggest("A", "a");
    assertSuggest("b", "b");
    assertSuggest("button text", "buttonText");
    assertSuggest("Customer Id", "customerId");
    assertSuggest("some,,, very!!! long: : :... text", "someVeryLongText");
    assertSuggest("label.text", "labelText");
    assertSuggest("0 items", "property0Items");
    assertSuggest("new", "propertyNew");
    assertSuggest("", "property");
  }

  private void assertConverts(String propertyName, String methodName) {
    assertEquals(methodName, GwtI18nUtil.convertPropertyName2MethodName(propertyName,
                                                                        PsiNameHelper
                                                                          .getInstance(myPsiManager.getProject()), LanguageLevel.JDK_1_5));
  }

  private void assertSuggest(final String value, final String expectedKey) {
    assertEquals(expectedKey, GwtI18nUtil.suggestPropertyKey(value, PsiNameHelper.getInstance(myProject),
                                                             LanguageLevel.HIGHEST));
  }
}
