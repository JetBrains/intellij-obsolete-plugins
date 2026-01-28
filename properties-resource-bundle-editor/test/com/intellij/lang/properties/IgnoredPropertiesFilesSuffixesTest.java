// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.properties;

import com.intellij.lang.properties.editor.inspections.incomplete.IncompletePropertyInspection;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Collections;

/**
 * @author Dmitry Batkovich
 */
public class IgnoredPropertiesFilesSuffixesTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(IncompletePropertyInspection.class);
  }

  public void testPropertyIsComplete() {
    myFixture.addFileToProject("p.properties", "key=value");
    myFixture.addFileToProject("p_en.properties", "key=value eng");
    final PsiFile file = myFixture.addFileToProject("p_ru.properties", "");
    final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
    assertNotNull(propertiesFile);
    final ResourceBundle resourceBundle = propertiesFile.getResourceBundle();
    assertSize(3, resourceBundle.getPropertiesFiles());
    final IncompletePropertyInspection incompletePropertyInspection = IncompletePropertyInspection.getInstance(propertiesFile.getContainingFile());
    incompletePropertyInspection.addSuffixes(Collections.singleton("ru"));
    assertTrue(incompletePropertyInspection.isPropertyComplete("key", resourceBundle));
  }

  public void testPropertyIsComplete2() {
    myFixture.addFileToProject("p.properties", "key=value");
    myFixture.addFileToProject("p_en.properties", "key=value eng");
    final PsiFile file = myFixture.addFileToProject("p_ru.properties", "key=value rus");
    final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
    assertNotNull(propertiesFile);
    final ResourceBundle resourceBundle = propertiesFile.getResourceBundle();
    assertSize(3, resourceBundle.getPropertiesFiles());
    final IncompletePropertyInspection incompletePropertyInspection = IncompletePropertyInspection.getInstance(propertiesFile.getContainingFile());
    assertTrue(incompletePropertyInspection.isPropertyComplete("key", resourceBundle));
  }

  public void testPropertyIsIncomplete() {
    myFixture.addFileToProject("p.properties", "key=value");
    myFixture.addFileToProject("p_en.properties", "key=value eng");
    myFixture.addFileToProject("p_fr.properties", "");
    final PsiFile file = myFixture.addFileToProject("p_ru.properties", "");
    final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
    assertNotNull(propertiesFile);
    final ResourceBundle resourceBundle = propertiesFile.getResourceBundle();
    assertSize(4, resourceBundle.getPropertiesFiles());
    final IncompletePropertyInspection incompletePropertyInspection = IncompletePropertyInspection.getInstance(propertiesFile.getContainingFile());
    incompletePropertyInspection.addSuffixes(Collections.singleton("ru"));
    assertFalse(incompletePropertyInspection.isPropertyComplete("key", resourceBundle));
  }

  public void testPropertyIsIncomplete2() {
    myFixture.addFileToProject("p.properties", "key=value");
    myFixture.addFileToProject("p_en.properties", "");
    myFixture.addFileToProject("p_en_EN.properties", "");
    myFixture.addFileToProject("p_en_GB.properties", "");
    final PsiFile file = myFixture.addFileToProject("p_en_US.properties", "");
    final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
    assertNotNull(propertiesFile);
    final ResourceBundle resourceBundle = propertiesFile.getResourceBundle();
    assertSize(5, resourceBundle.getPropertiesFiles());
    final IncompletePropertyInspection incompletePropertyInspection = IncompletePropertyInspection.getInstance(propertiesFile.getContainingFile());
    incompletePropertyInspection.addSuffixes(Collections.singleton("en"));
    assertFalse(incompletePropertyInspection.isPropertyComplete("key", resourceBundle));
  }
}
