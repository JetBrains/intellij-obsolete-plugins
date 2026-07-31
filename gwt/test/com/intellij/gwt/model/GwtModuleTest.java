/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.model;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public class GwtModuleTest extends GwtTestCase {

  public void testDefaultModule() {
    final VirtualFile dir = addGwtModule("references/getRootPanelRef");
    final List<GwtModule> modules = getGwtManager().getGwtModules(myModule, true);
    final GwtModule module = assertOneElement(modules);
    assertEquals("MyModule", module.getQualifiedName());
    assertEquals(dir, module.getModuleDirectory());
    assertEquals("client", assertOneElement(module.getSourceRoots(true)).getName());
    assertEquals("client", assertOneElement(module.getSourceRoots(false)).getName());
    assertEquals("public", assertOneElement(module.getPublicRoots()).getName());
    assertEquals("public", assertOneElement(module.getPublicRoots(false)).getName());

    final VirtualFile file = dir.findFileByRelativePath("client/GwtGetRootPanel.java");
    assertNotNull(file);
    final GwtModule module2 = getGwtManager().findGwtModuleByClientSourceFile(file);
    assertEquals(module, module2);
  }

  public void testComplexModule() {
    final VirtualFile dir = addGwtModule("model/complexModule");
    addGwtModule("references/getRootPanelRef");
    GlobalSearchScope scope = GlobalSearchScope.allScope(myProject);
    GwtModule module = getGwtManager().findGwtModuleByQualifiedName("pack.ComplexModule", scope);
    assertNotNull(module);
    assertEquals("pack.ComplexModule", module.getQualifiedName());
    assertEquals("ComplexModule", module.getShortName());
    assertEquals(dir.findChild("pack"), module.getModuleDirectory());

    assertEquals("client1", assertOneElement(module.getSourceRoots(true)).getName());
    assertEquals("client1", assertOneElement(module.getSourceRoots(false)).getName());
    assertEquals("public2", assertOneElement(module.getPublicRoots()).getName());
    final GwtServlet servlet = assertOneElement(module.getServlets());
    assertEquals("/path", servlet.getPath().getValue());
    assertEquals("MyClass", servlet.getServletClass().getValue());
    assertEquals(2, module.getInheritses().size());
    assertEquals("com.google.gwt.user.User", module.getInheritses().get(0).getName().getValue());
    assertEquals("MyModule", module.getInheritses().get(1).getName().getValue());
    assertEquals(2, module.getInherited(scope).size());
    assertEquals("com.google.gwt.user.User", module.getInherited(scope).get(0).getQualifiedName());
    assertEquals("MyModule", module.getInherited(scope).get(1).getQualifiedName());

    assertTrue(getGwtManager().isInheritedOrSelf(module, module));
    assertTrue(getGwtManager().isInheritedOrSelf(module, module.getInherited(scope).get(0)));
    assertTrue(getGwtManager().isInheritedOrSelf(module, module.getInherited(scope).get(1)));
    assertFalse(getGwtManager().isInheritedOrSelf(module.getInherited(scope).get(1), module));

    final VirtualFile file = dir.findFileByRelativePath("pack/client1/client2/ClientFile.java");
    assertNotNull(file);
    final GwtModule module2 = getGwtManager().findGwtModuleByClientSourceFile(file);
    assertEquals(module, module2);
    assertNotNull(getGwtManager().findHtmlFilesByModule(module));
  }

  public void testModifyModule() {
    addGwtModule("model/complexModule");
    final GwtModule module = assertOneElement(getGwtManager().getGwtModules(myModule, true));

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      assertEquals(0, module.getEntryPoints().size());
      module.addEntryPoint();
      assertEquals(1, module.getEntryPoints().size());

      assertEquals(1, module.getPublics().size());
      module.addPublic();
      assertEquals(2, module.getPublics().size());

      assertEquals(1, module.getSources().size());
      module.addSource();
      assertEquals(2, module.getSources().size());

      assertEquals(1, module.getServlets().size());
      module.addServlet();
      assertEquals(2, module.getServlets().size());

      assertEquals(2, module.getInheritses().size());
      module.addInherits();
      assertEquals(3, module.getInheritses().size());
    });
  }

  public void testMultiSourceRootsModule() {
    VirtualFile dir = addGwtModule("model/multiSourceRootsModule", "src1", "src2");

    GwtModule gwtModule = findGwtModule("ppp.MyModule");
    assertNotNull(gwtModule);

    VirtualFile file1 = dir.findFileByRelativePath("src1/ppp/client/ClientFile.java");
    assertNotNull(file1);
    assertSame(gwtModule, getGwtManager().findGwtModuleByClientSourceFile(file1));

    VirtualFile file2 = dir.findFileByRelativePath("src2/ppp/client/ClientFile2.java");
    assertNotNull(file2);
    assertSame(gwtModule, getGwtManager().findGwtModuleByClientSourceFile(file2));
  }

  public void testDependentModule() {
    VirtualFile baseDir = addGwtModule("model/dependentModules/base");
    final Module module = doCreateRealModule("dep");
    addGwtModule("model/dependentModules/dependent", module);

    ModuleRootModificationUtil.addDependency(module, myModule);

    GwtModule gwtModule = getGwtManager().findGwtModuleByQualifiedName("ppp.MyModule", GlobalSearchScope.allScope(myProject));
    assertNotNull(gwtModule);
    VirtualFile file = baseDir.findFileByRelativePath("ppp/client/BaseFile.java");
    assertNotNull(file);
    assertSame(gwtModule, getGwtManager().findGwtModuleByClientSourceFile(file));
  }

  public void testGwt16Module() {
    final VirtualFile baseDir = addGwtModule("model/gwt16Module", myModule, GwtVersionImpl.VERSION_1_6, "src");
    final VirtualFile moduleFile = baseDir.findFileByRelativePath("src/ppp/MyModule.gwt.xml");
    assertNotNull(moduleFile);
    final PsiFile psiFile = getPsiManager().findFile(moduleFile);
    assertNotNull(psiFile);
    final GwtModule gwtModule = getGwtManager().getGwtModuleByXmlFile(psiFile);
    assertNotNull(gwtModule);

    assertEquals("ShortName", gwtModule.getRenameTo().getValue());
    assertEquals(gwtModule,
                 assertOneElement(getGwtManager().findGwtModulesByOutputName("ShortName", GlobalSearchScope.allScope(myProject))));
  }
}
