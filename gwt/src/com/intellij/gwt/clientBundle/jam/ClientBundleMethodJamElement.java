package com.intellij.gwt.clientBundle.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.gwt.clientBundle.ClientBundleUtil;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.javaee.model.annotations.AnnotationGenericValue;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ClientBundleMethodJamElement extends JamBaseElement<PsiMethod> {
  private static final Set<String> DEFAULT_EXTENSIONS_ANNOTATION_SET = Collections.singleton(ClientBundleUtil.DEFAULT_EXTENSIONS_ANNOTATION);
  private static final JamStringAttributeMeta.Collection<PsiFile> SOURCE_PATH_META =
    JamAttributeMeta.collectionString("value", new SourceFileJamConverter(Conditions.alwaysTrue()));
  private static final JamAnnotationMeta SOURCE_META = new JamAnnotationMeta(ClientBundleUtil.SOURCE_ANNOTATION)
    .addAttribute(SOURCE_PATH_META);
  public static final JamMethodMeta<ClientBundleMethodJamElement> META =
    new JamMethodMeta<>(ClientBundleMethodJamElement.class, ClientBundleMethodJamElement::new)
    .addAnnotation(SOURCE_META);

  public ClientBundleMethodJamElement(PsiElementRef<?> ref) {
    super(ref);
  }

  public @NotNull List<PsiFile> getSourceFiles(boolean addLocalized) {
    List<PsiFile> result = new SmartList<>();
    final PsiMethod method = getPsiElement();
    final List<JamStringAttributeElement<PsiFile>> sourceAnnotation = SOURCE_META.getAttribute(method, SOURCE_PATH_META);
    for (JamStringAttributeElement<PsiFile> element : sourceAnnotation) {
      final PsiFile mainFile = element.getValue();
      if (mainFile != null) {
        result.add(mainFile);
        if (addLocalized) {
          addLocalizedFiles(mainFile, result);
        }
      }
    }

    if (sourceAnnotation.isEmpty()) {
      final PsiType type = method.getReturnType();
      if (type instanceof PsiClassType) {
        final PsiClass resourceType = ((PsiClassType)type).resolve();
        if (resourceType != null) {
          final PsiModifierList modifierList = resourceType.getModifierList();
          if (modifierList != null) {
            final PsiAnnotation annotation = AnnotationUtil.findAnnotationInHierarchy(resourceType, DEFAULT_EXTENSIONS_ANNOTATION_SET);
            final PsiDirectory directory = method.getContainingFile().getContainingDirectory();
            if (annotation != null && directory != null) {
              for (AnnotationGenericValue<String> value : AnnotationModelUtil.getStringArrayValue(annotation, "value")) {
                final String extension = value.getValue();
                if (extension != null) {
                  final PsiFile file = directory.findFile(method.getName() + extension);
                  if (file != null) {
                    result.add(file);
                    if (addLocalized) {
                      addLocalizedFiles(file, result);
                    }
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }
    return result;
  }

  private static void addLocalizedFiles(@NotNull PsiFile mainFile, @NotNull List<PsiFile> result) {
    final PsiDirectory parent = mainFile.getParent();
    if (parent == null) return;

    final String baseName = FileUtilRt.getNameWithoutExtension(mainFile.getName()) + "_";
    final String extension = FileUtilRt.getExtension(mainFile.getName());
    for (PsiFile file : parent.getFiles()) {
      final String name = file.getName();
      if (name.startsWith(baseName) && FileUtilRt.extensionEquals(name, extension)) {
        result.add(file);
      }
    }
  }

  public static @Nullable ClientBundleMethodJamElement getElement(PsiMethod method) {
    return ReadAction.computeBlocking(() -> JamService.getJamService(method.getProject()).getJamElement(META.getJamKey(), method));
  }
}
