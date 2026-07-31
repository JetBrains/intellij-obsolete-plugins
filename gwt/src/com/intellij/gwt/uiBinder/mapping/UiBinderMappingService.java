package com.intellij.gwt.uiBinder.mapping;

import com.intellij.ide.hierarchy.JavaHierarchyUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_XML_SUFFIX;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;

public abstract class UiBinderMappingService {
  public static UiBinderMappingService getInstance(@NotNull Module module) {
    return UiBinderMappingServiceFactory.getInstance(module.getProject()).getService(module);
  }

  public abstract @NotNull List<PsiClass> getBoundClasses(@NotNull PsiFile uiXmlFile);

  public abstract @NotNull List<XmlFile> getUiXmlFiles(@NotNull PsiClass aClass);

  public abstract @Nullable XmlFile getUiXmlFile(@NotNull PsiClass uiBinderInheritor);

  public abstract boolean isUiRendererComponent(@NotNull PsiClass psiClass);

  public static @NotNull List<PsiClass> getBoundClassesForFile(@NotNull PsiFile file) {
    final Module module = findModuleForPsiElement(file);
    return module != null ? getInstance(module).getBoundClasses(file) : Collections.emptyList();
  }

  public static @NotNull List<XmlFile> getUiXmlFilesForClass(@NotNull PsiClass psiClass) {
    final Module module = findModuleForPsiElement(psiClass);
    return module != null ? getInstance(module).getUiXmlFiles(psiClass) : Collections.emptyList();
  }

  public static @Nullable String deduceTemplateFileUrl(@NotNull PsiClass binderClass) {
    UiTemplateInterfaceJamElement element = UiTemplateInterfaceJamElement.getElement(binderClass);
    XmlFile xmlFile = (element == null)
                    ? getDefaultTemplateFile(binderClass)
                    : getAnnotationTemplateFile(element.getUiTemplateValue(), binderClass);
    VirtualFile virtualFile = xmlFile == null ? null : xmlFile.getVirtualFile();
    return virtualFile == null ? null : virtualFile.getUrl();
  }

  public static @Nullable XmlFile getDefaultTemplateFile(@NotNull PsiClass aClass) {
    PsiClass containingClass;
    if ((containingClass = aClass.getContainingClass()) != null) {
      aClass = containingClass;
    }

    Project project = aClass.getProject();
    String packageName = JavaHierarchyUtil.getPackageName(aClass);
    if (packageName == null) return null;

    PsiPackage psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName);
    if (psiPackage == null) return null;

    String fileName = aClass.getName() + UI_XML_SUFFIX;
    return findTemplateXmlFile(psiPackage, fileName, aClass.getResolveScope());
  }

  private static @Nullable XmlFile getAnnotationTemplateFile(@Nullable String uiTemplateValue, @NotNull PsiClass binderClass) {
    if (uiTemplateValue == null) return null;
    if (!uiTemplateValue.endsWith(UI_XML_SUFFIX)) return null;

    String unsuffixed = StringUtil.trimEnd(uiTemplateValue, UI_XML_SUFFIX);
    String packageName, fileName;
    if (unsuffixed.contains(".")) {
      packageName = StringUtil.getPackageName(unsuffixed);
      fileName = StringUtil.getShortName(unsuffixed);
    } else {
      String templateName = JavaHierarchyUtil.getPackageName(binderClass) + "." + unsuffixed.replace('/', '.');
      packageName = StringUtil.getPackageName(templateName);
      fileName = StringUtil.getShortName(templateName);
    }

    fileName = fileName.replace('$', '.');

    Project project = binderClass.getProject();
    PsiPackage psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName);
    if (psiPackage == null) return null;

    return findTemplateXmlFile(psiPackage, fileName, binderClass.getResolveScope());
  }

  private static @Nullable XmlFile findTemplateXmlFile(@NotNull PsiPackage psiPackage, @NotNull String fileName,
                                                       @NotNull GlobalSearchScope searchScope) {
    if (!fileName.endsWith(UI_XML_SUFFIX)) {
      fileName += UI_XML_SUFFIX;
    }
    for (PsiDirectory psiDirectory : psiPackage.getDirectories(searchScope)) {
      final PsiFile xmlFile = psiDirectory.findFile(fileName);
      if (xmlFile instanceof XmlFile) return (XmlFile) xmlFile;
    }
    return null;
  }
}
