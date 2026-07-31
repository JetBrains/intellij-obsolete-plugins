package com.intellij.gwt.clientBundle;

import com.intellij.gwt.clientBundle.jam.ClientBundleMethodJamElement;
import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.gwt.uiBinder.declarations.UiXmlDeclarationsManager;
import com.intellij.gwt.uiBinder.declarations.UiXmlVariableDeclaration;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_XML_FILES_SCOPE;

public final class ClientBundleUtil {
  public static final @NonNls String DEFAULT_EXTENSIONS_ANNOTATION = "com.google.gwt.resources.ext.DefaultExtensions";
  public static final @NonNls String CLIENT_BUNDLE_INTERFACE = "com.google.gwt.resources.client.ClientBundle";
  public static final @NonNls String SOURCE_ANNOTATION = "com.google.gwt.resources.client.ClientBundle.Source";
  public static final @NonNls String CSS_RESOURCE_INTERFACE = "com.google.gwt.resources.client.CssResource";
  public static final @NonNls String IMAGE_RESOURCE_INTERFACE = "com.google.gwt.resources.client.ImageResource";
  public static final @NonNls String CLASS_NAME_ANNOTATION = "com.google.gwt.resources.client.CssResource.ClassName";

  private ClientBundleUtil() {
  }

  public static Set<PsiClass> getCssInterfaces(final @NotNull StylesheetFile stylesheetFile) {
    final PsiClass sourceAnnotation = findSourceAnnotation(stylesheetFile);
    if (sourceAnnotation == null) return Collections.emptySet();

    final Set<PsiClass> result = new HashSet<>();
    AnnotatedElementsSearch.searchPsiMethods(sourceAnnotation, stylesheetFile.getUseScope()).forEach(psiMethod -> {
      final ClientBundleMethodJamElement element = ClientBundleMethodJamElement.getElement(psiMethod);
      if (element != null) {
        final PsiType returnType = psiMethod.getReturnType();
        if (element.getSourceFiles(true).contains(stylesheetFile) && returnType instanceof PsiClassType) {
          ContainerUtil.addIfNotNull(result, ((PsiClassType)returnType).resolve());
        }
      }
      return true;
    });
    return result;
  }

  public static Set<StylesheetFile> getStylesheetFiles(final @NotNull PsiClass cssResourceClass,
                                                       final boolean addLocalized,
                                                       final boolean processSubClasses) {
    final Set<StylesheetFile> result = new HashSet<>();

    SearchScope scope = cssResourceClass.getUseScope();
    if (!(scope instanceof GlobalSearchScope)) {
      scope = GlobalSearchScope.fileScope(cssResourceClass.getContainingFile());
    }

    ReferencesSearch.search(cssResourceClass, scope.intersectWith(UI_XML_FILES_SCOPE)).asIterable().forEach(reference -> {
      PsiFile file = reference.getElement().getContainingFile();
      for (UiXmlVariableDeclaration element : UiXmlDeclarationsManager.getDeclarations((XmlFile)file)) {
        if (element instanceof UiStyleElement styleElement) {
          final String type = styleElement.getType();
          if (type != null && type.equals(cssResourceClass.getQualifiedName())) {
            result.addAll(styleElement.getStylesheetFiles());
          }
        }
      }
    });

    final PsiClass sourceAnnotation = findSourceAnnotation(cssResourceClass);
    if (sourceAnnotation == null) {
      return result;
    }
    AnnotatedElementsSearch.searchPsiMethods(sourceAnnotation, scope).forEach(psiMethod -> {
      final ClientBundleMethodJamElement element = ClientBundleMethodJamElement.getElement(psiMethod);
      if (element != null) {
        final PsiType type = psiMethod.getReturnType();
        if (type instanceof PsiClassType) {
          final PsiClass psiClass = ((PsiClassType)type).resolve();
          if (psiClass != null && (psiClass.getManager().areElementsEquivalent(psiClass, cssResourceClass)
                                   || processSubClasses && psiClass.isInheritor(cssResourceClass, true))) {
            for (PsiFile psiFile : element.getSourceFiles(addLocalized)) {
              if (psiFile instanceof StylesheetFile) {
                result.add((StylesheetFile)psiFile);
              }
            }
          }
        }
      }
      return true;
    });
    return result;
  }

  private static @Nullable PsiClass findSourceAnnotation(@NotNull PsiElement context) {
    return JavaPsiFacade.getInstance(context.getProject()).findClass(SOURCE_ANNOTATION, context.getResolveScope());
  }

  public static boolean isImplementationProvidedByGwt(@NotNull PsiClass psiClass) {
    if (!psiClass.isInterface()) {
      return false;
    }
    return InheritanceUtil.isInheritor(psiClass, "com.google.gwt.resources.client.ResourcePrototype")
           || InheritanceUtil.isInheritor(psiClass, "com.google.gwt.i18n.shared.Localizable");
  }
}
