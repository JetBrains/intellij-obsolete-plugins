/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.references;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.refactoring.rename.RenameProcessor;

public class GwtRenameTest extends GwtTestCase {
  public void testRenameHtmlTagId() {
    final VirtualFile root = addGwtModule("references/getRootPanelRef");
    final VirtualFile file = root.findFileByRelativePath("public/MyModule.html");
    assertNotNull(file);

    final XmlAttributeValue attribute = findElementByString(file, "aaaaa", XmlAttributeValue.class);
    rename(attribute, "ccc");

    findElementByString(file, "ccc", XmlAttributeValue.class);
    findElementByString(root.findFileByRelativePath("client/GwtGetRootPanel.java"), "ccc", PsiLiteralExpression.class);
  }

  public void testRenamePropertiesClass() {
    final VirtualFile root = addGwtModule("i18n/manager", "src");
    final PsiClass aClass = findConstantsClass();
    rename(aClass, "Props");
    assertNotNull(root.findFileByRelativePath("src/client/Props.properties"));
    assertNotNull(root.findFileByRelativePath("src/client/Props_ru.properties"));
  }

  public void testRenameGetPropertyMethod() {
    addGwtModule("i18n/manager", "src");
    final PsiClass aClass = findConstantsClass();
    final PsiMethod method = assertOneElement(aClass.findMethodsByName("prop1", false));
    final PropertiesFile[] files = GwtI18nManager.getInstance(myProject).getPropertiesFiles(aClass);
    assertEquals(2, files.length);

    rename(method, "newProp");

    assertEquals("newProp", files[0].getProperties().get(0).getKey());
    assertEquals("newProp", files[1].getProperties().get(0).getKey());

    final PsiMethod method2 = assertOneElement(aClass.findMethodsByName("prop2", false));

    rename(method2, "newProp2");
    assertNull(files[0].findPropertyByKey("newProp2"));
    assertNull(files[1].findPropertyByKey("newProp2"));
  }

  public void testRenameProperty() {
    addGwtModule("i18n/manager", "src");
    final PsiClass aClass = findConstantsClass();
    final PropertiesFile[] files = GwtI18nManager.getInstance(myProject).getPropertiesFiles(aClass);
    assertEquals(2, files.length);
    PropertiesFile file = files[0].getName().contains("ru") ? files[1] : files[0];
    Property prop1 = (Property)file.findPropertyByKey("prop1");
    assertNotNull(prop1);

    rename(prop1, "newProp");
    assertEquals(1, aClass.findMethodsByName("newProp", false).length);

    Property prop3 = (Property)file.findPropertyByKey("prop3");
    assertNotNull(prop3);

    rename(prop3, "newProp2");
    assertEquals(0, aClass.findMethodsByName("newProp2", false).length);
  }

  private PsiClass findConstantsClass() {
    return findClass("client.MyConstants");
  }

  private PsiClass findClass(final String className) {
    final PsiClass aClass = myJavaFacade.findClass(className, GlobalSearchScope.allScope(myProject));
    assertNotNull(aClass);
    return aClass;
  }

  public void testRenameServiceClass() {
    addGwtModule("references/service/src");
    PsiClass psiClass = findClass("client.MyService");
    rename(psiClass, "MyService239");
    PsiClass async = findClass("client.MyService239Async");
    assertEquals(1, async.getMethods().length);
  }

  public void testRenameServiceMethod() {
    addGwtModule("references/service/src");
    PsiClass psiClass = findClass("client.MyService");
    PsiMethod method = psiClass.getMethods()[0];
    rename(method, "newMethod");

    PsiClass async = findClass("client.MyServiceAsync");
    PsiMethod asyncMethod = async.getMethods()[0];
    assertEquals("newMethod", asyncMethod.getName());
    assertEquals(2, asyncMethod.getParameterList().getParametersCount());
  }

  private void rename(final PsiElement element, final String newName) {
    new RenameProcessor(getProject(), element, newName, false, false).run();
  }

}
