package com.intellij.dmserver.intention;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.QuickFixActionRegistrar;
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider;
import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.facet.OsmorcFacet;

import java.util.HashSet;
import java.util.Set;

final class DmServerUnresolvedReferenceQuickFixProvider extends UnresolvedReferenceQuickFixProvider<PsiJavaCodeReferenceElement> {
  @Override
  public void registerFixes(@NotNull PsiJavaCodeReferenceElement reference, @NotNull QuickFixActionRegistrar registrar) {
    Module module = ModuleUtilCore.findModuleForPsiElement(reference);
    if (module == null || OsmorcFacet.getInstance(module) == null || DMBundleFacet.getInstance(module) == null) {
      return;
    }

    doRegisterFixes(reference, registrar);
  }


  private void doRegisterFixes(PsiJavaCodeReferenceElement reference, QuickFixActionRegistrar registrar) {
    final PsiElement psiElement = reference.getElement();
    @NonNls final String referenceName = reference.getRangeInElement().substring(psiElement.getText());

    Project project = psiElement.getProject();
    PsiFile containingFile = psiElement.getContainingFile();
    if (containingFile == null) return;

    VirtualFile classVFile = containingFile.getVirtualFile();
    if (classVFile == null) return;

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final Module currentModule = fileIndex.getModuleForFile(classVFile);
    if (currentModule == null) return;


    if ("TestCase".equals(referenceName) || isAnnotation(psiElement) && isJunitAnnotationName(referenceName)) {
      return;
    }

    if (isAnnotation(psiElement) && AnnotationUtil.isJetbrainsAnnotation(referenceName)) {
      return;
    }

    Set<Object> librariesToAdd = new HashSet<>();
    final JavaPsiFacade facade = JavaPsiFacade.getInstance(psiElement.getProject());
    PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(referenceName, GlobalSearchScope.allScope(project));
    for (final PsiClass aClass : classes) {
      if (!facade.getResolveHelper().isAccessible(aClass, psiElement, aClass)) continue;
      PsiFile psiFile = aClass.getContainingFile();
      if (psiFile == null) continue;
      VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile == null) continue;

      ModuleFileIndex moduleFileIndex = ModuleRootManager.getInstance(currentModule).getFileIndex();
      for (OrderEntry orderEntry : fileIndex.getOrderEntriesForFile(virtualFile)) {
        if (orderEntry instanceof LibraryOrderEntry) {
          LibraryOrderEntry libraryEntry = (LibraryOrderEntry)orderEntry;
          Library library = libraryEntry.getLibrary();
          if (library == null) continue;
          VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
          if (files.length == 0) continue;


          VirtualFile jar = files[0];

          if (jar == null ||
              libraryEntry.isModuleLevel() && !librariesToAdd.add(jar) ||
              !librariesToAdd.add(library) ||
              moduleFileIndex.getOrderEntryForFile(virtualFile) != null) {
            continue;
          }
          registrar.register(new AddPackageIntentionAction(reference, aClass, libraryEntry));
        }
      }
    }
  }

  @Override
  @NotNull
  public Class<PsiJavaCodeReferenceElement> getReferenceClass() {
    return PsiJavaCodeReferenceElement.class;
  }

  private static boolean isAnnotation(final PsiElement psiElement) {
    return psiElement.getParent() instanceof PsiAnnotation && PsiUtil.isLanguageLevel5OrHigher(psiElement);
  }

  private static boolean isJunitAnnotationName(@NonNls final String referenceName) {
    return "Test".equals(referenceName) || "Ignore".equals(referenceName) || "RunWith".equals(referenceName) ||
           "Before".equals(referenceName) || "BeforeClass".equals(referenceName) ||
           "After".equals(referenceName) || "AfterClass".equals(referenceName);
  }
}
