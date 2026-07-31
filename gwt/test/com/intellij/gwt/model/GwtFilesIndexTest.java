/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.model;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.index.GwtHtmlFileIndex;
import com.intellij.gwt.module.index.GwtModuleRenameToIndex;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.gwt.superSource.GwtModuleSuperSourceIndex;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GwtFilesIndexTest extends GwtTestCase {
  @NonNls private static final String GET_ROOT_PANEL_REF_MODULE = "references/getRootPanelRef";


  public void testAddRemoveGwtXmlFile() {
    assertEquals(0, getGwtXmlFiles().length);
    addGwtModule(GET_ROOT_PANEL_REF_MODULE);
    final VirtualFile file = assertOneElement(getGwtXmlFiles());
    assertEquals("MyModule.gwt.xml", file.getName());
    delete(file);
    assertEquals(0, getGwtXmlFiles().length);
  }

  public void testRenameGwtXmlFile() {
    addGwtModule(GET_ROOT_PANEL_REF_MODULE);
    final VirtualFile file = assertOneElement(getGwtXmlFiles());
    final PsiFile psiFile = getPsiManager().findFile(file);
    assertNotNull(psiFile);

    new RenameProcessor(getProject(), psiFile, "NewName.gwt.xml", false, false).run();

    assertEquals("NewName.gwt.xml", assertOneElement(getGwtXmlFiles()).getName());
  }

  public void testMoveGwtXmlFile() {
    addGwtModule(GET_ROOT_PANEL_REF_MODULE);
    final VirtualFile file = assertOneElement(getGwtXmlFiles());
    final PsiFile psiFile = getPsiManager().findFile(file);
    assertNotNull(psiFile);
    final PsiDirectory psiDirectory = psiFile.getContainingDirectory();
    assertNotNull(psiDirectory);
    final PsiDirectory dir = WriteAction.compute(() -> psiDirectory.createSubdirectory("newDir"));
    new MoveFilesOrDirectoriesProcessor(getProject(), new PsiElement[]{psiFile}, dir, false, false, null, null).run();


    final VirtualFile htmlFile = assertOneElement(getGwtXmlFiles());
    final VirtualFile parent = htmlFile.getParent();
    assertNotNull(parent);
    assertEquals("newDir", parent.getName());
  }

  public void testSuperSourceIndex() {
    final VirtualFile moduleDir = addGwtModule("model/superSource", myModule, getLatestVersion());
    final Collection<VirtualFile> moduleRoots = GwtModuleSuperSourceIndex.getSuperSourceRoots(GlobalSearchScope.projectScope(myProject));
    assertEquals(moduleDir.getUrl() + "/ppp/sup", assertOneElement(moduleRoots).getUrl());

    final Collection<VirtualFile> allRoots = GwtModuleSuperSourceIndex.getSuperSourceRoots(GlobalSearchScope.allScope(myProject));
    final VirtualFile emulRoot = JarFileSystem.getInstance().findFileByPath(getMockGwtUserJarPath(getLatestVersion()) + GwtSdkUtil.EMUL_ROOT);
    assertNotNull(emulRoot);
    assertTrue(toString(allRoots), allRoots.contains(emulRoot));
  }

  private VirtualFile[] getGwtXmlFiles() {
    final List<GwtModule> modules = GwtModulesManager.getInstance(myProject).getGwtModules(myModule, true);
    List<VirtualFile> files = new ArrayList<>();
    for (GwtModule module : modules) {
      files.add(module.getModuleFile());
    }
    return VfsUtilCore.toVirtualFileArray(files);
  }

  public void testHtmlFileOutsideSourceRoots() {
    final VirtualFile baseDir = addGwtModule("model/gwt16Module", myModule, GwtVersionImpl.VERSION_1_6, "src");
    final VirtualFile html1 = baseDir.findFileByRelativePath("war/MyModule.html");
    final VirtualFile html2 = baseDir.findFileByRelativePath("src/ppp/public/MyModule.html");
    assertSameElements(getHtmlFiles("ShortName"), html1, html2);
  }

  public void testGwtXmlFileIndex() {
    final VirtualFile baseDir = addGwtModule("model/gwt16Module", myModule, GwtVersionImpl.VERSION_1_6, "src");
    final Collection<VirtualFile> files = GwtModuleRenameToIndex.getGwtXmlFiles("ShortName", GlobalSearchScope.allScope(myProject));
    assertEquals(baseDir.findFileByRelativePath("src/ppp/MyModule.gwt.xml"), assertOneElement(files));
  }

  public void testAddRemoveReferenceToGwtModule() {
    addGwtModule(GET_ROOT_PANEL_REF_MODULE);
    final String moduleName = "MyModule";
    final VirtualFile file = assertOneElement(getHtmlFiles(moduleName));
    assertEquals("MyModule.html", file.getName());
    final PsiFile psiFile = getPsiManager().findFile(file);
    assertNotNull(psiFile);

    final Document document = getDocument(psiFile);
    final String oldText = document.getText();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.deleteString(0, document.getTextLength()));
    PsiDocumentManager.getInstance(getProject()).commitDocument(document);

    assertEmpty(getHtmlFiles(moduleName));

    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(0, oldText));
    PsiDocumentManager.getInstance(getProject()).commitDocument(document);

    final VirtualFile movedFile = assertOneElement(getHtmlFiles(moduleName));
    assertEquals("MyModule.html", movedFile.getName());
    delete(movedFile);
    assertEmpty(getHtmlFiles(moduleName));
  }

  private Collection<VirtualFile> getHtmlFiles(String moduleName) {
    return GwtHtmlFileIndex.getHtmlFilesByModule(myProject, moduleName);
  }

}
