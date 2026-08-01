/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.references;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.css.CssFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public class MiscReferencesTest extends GwtReferencesTestCase {

  public void testRootPanelGetReferences() {
    final VirtualFile moduleRoot = addGwtModule("references/getRootPanelRef");
    final VirtualFile file = moduleRoot.findFileByRelativePath("client/GwtGetRootPanel.java");
    assertNotNull(file);

    final PsiLiteralExpression stringLiteral = findElementByString(file, "aaaaa", PsiLiteralExpression.class);
    final PsiElement resolved = assertOneElement(getResolveResults(stringLiteral));
    assertTrue(resolved.getContainingFile() instanceof XmlFile);
    assertVariantsContains(stringLiteral, "aaaaa", "slot2");

    assertUnresolvable(findElementByString(file, "239239", PsiLiteralExpression.class));

    PsiLiteralExpression unresolvable = findElementByString(file, "bbbbbb", PsiLiteralExpression.class);
    assertUnresolvable(unresolvable);
    assertVariantsContains(unresolvable, "aaaaa", "slot2");
  }

  public void testClientBundleSourceByAbsolutePath() {
    final VirtualFile root = addGwtModule("references/clientBundle");
    final VirtualFile file = root.findFileByRelativePath("client/MyClientBundle.java");
    assertNotNull(file);

    // A package-absolute @Source path (relative to the source root) must resolve to the CSS file (IDEA-61461).
    PsiLiteralExpression source = findElementByString(file, "client/app.css", PsiLiteralExpression.class);
    assertTrue(ContainerUtil.exists(getResolveResults(source),
                                    e -> e.getContainingFile() instanceof CssFile && "app.css".equals(e.getContainingFile().getName())));
  }

  public void testAddStyleReferences() {
    final VirtualFile root = addGwtModule("references/addStyleRef");
    final VirtualFile file = root.findFileByRelativePath("client/MyClientClass.java");
    assertNotNull(file);

    PsiLiteralExpression string = findElementByString(file, "MyButtonStyle", PsiLiteralExpression.class);
    final List<PsiElement> resolvedList = getResolveResults(string);
    final PsiElement resolved = assertOneElement(resolvedList);
    assertTrue(resolved.getContainingFile() instanceof CssFile);
    assertVariantsContains(string, "MyButtonStyle", "MyButtonStyle2", "MyButtonStyle3", "MyAdditionalStyle");

    PsiLiteralExpression string2 = findElementByString(file, "MyAdditionalStyle", PsiLiteralExpression.class);
    assertEquals("Additional.css", assertOneElement(getResolveResults(string2)).getContainingFile().getName());

    PsiLiteralExpression string3 = findElementByString(file, "MyInheritedStyle", PsiLiteralExpression.class);
    assertEquals("Inherited.css", assertOneElement(getResolveResults(string3)).getContainingFile().getName());

    assertUnresolvable(findElementByString(file, "MyButtonStyle2", PsiLiteralExpression.class));

    assertUnresolvable(findElementByString(file, "239239239", PsiLiteralExpression.class));

    final PsiReference[] references =
        ContainerUtil.findAllAsArray(findElementByString(file, "MyButtonStyle MyButtonStyle2", PsiLiteralExpression.class).getReferences(), GwtToCssClassReference.class);
    assertEquals(2, references.length);
    assertEquals("MyButtonStyle", references[0].getCanonicalText());
    assertEquals("MyButtonStyle2", references[1].getCanonicalText());
    assertNotNull(references[0].resolve());
    assertNotNull(references[1].resolve());
  }

  public void testCssClassReferencesToWebRoots() {
    addGwtModule("references/cssRefToWebRoot");
  }

  public void testModuleReference() {
    addGwtModule("references/getRootPanelRef");
    final VirtualFile root = addGwtModule("model/complexModule");
    final VirtualFile file = root.findFileByRelativePath("pack/ComplexModule.gwt.xml");
    assertNotNull(file);

    final PsiElement element = findElementByString(file, "MyModule", XmlAttributeValue.class);
    assertEquals("MyModule.gwt.xml", assertResolvesTo(element, XmlFile.class).getName());
  }

  public void testModuleReferenceInTestCase() {
    VirtualFile dir = addGwtModule("references/gwtTestCase", myModule, GwtVersionImpl.VERSION_1_5);
    VirtualFile file = dir.findFileByRelativePath("ppp/client/MyGwtTest.java");
    assertNotNull(file);

    PsiLiteralExpression element = findElementByString(file, "ppp.MyModule", PsiLiteralExpression.class);
    assertEquals("MyModule.gwt.xml", assertResolvesTo(element, XmlFile.class).getName());

    assertVariantsContains(element, "ppp.MyModule", "ppp.MyModule2");
  }

  public void testServletPathReference() {
    doTestServletReferences("MyModule.gwt.xml", GwtVersionImpl.VERSION_1_5);
  }

  public void testServletPathReference16() {
    doTestServletReferences("web.xml", GwtVersionImpl.VERSION_1_6);
  }

  private void doTestServletReferences(final String fileName, GwtVersion version) {
    final VirtualFile dir = addGwtModule("references/service/src", myModule, version);
    addWebFacet(GwtFacet.getInstance(getModule()), getGwtTestDataPath(), "references/service/web/WEB-INF/web.xml");
    final VirtualFile file = dir.findFileByRelativePath("client/MyService.java");
    assertNotNull(file);

    PsiLiteralExpression[] elements = {
        findElementByString(file, "MyService1", PsiLiteralExpression.class),
        findElementByString(file, "MyService2", PsiLiteralExpression.class)
    };
    for (PsiLiteralExpression element : elements) {
      final XmlTag tag = assertResolvesTo(element, XmlTag.class);
      assertEquals("servlet", tag.getName());
      assertEquals(fileName, tag.getContainingFile().getName());
    }
  }
}
