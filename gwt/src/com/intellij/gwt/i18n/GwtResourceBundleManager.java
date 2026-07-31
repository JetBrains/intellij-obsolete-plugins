/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInspection.i18n.JavaI18nUtil;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.lang.properties.PropertiesReferenceManager;
import com.intellij.lang.properties.psi.I18nizedTextGenerator;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.PropertyCreationHandler;
import com.intellij.lang.properties.psi.ResourceBundleManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UExpression;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class GwtResourceBundleManager extends ResourceBundleManager {
  private static final Logger LOG = Logger.getInstance(GwtResourceBundleManager.class);
  private final PsiManager myPsiManager;
  private final GwtI18nManager myI18nManager;

  public GwtResourceBundleManager(final Project project) {
    super(project);
    myPsiManager = PsiManager.getInstance(project);
    myI18nManager = GwtI18nManager.getInstance(project);
  }

  @Override
  public @Nullable PsiClass getResourceBundle() {
    return null;
  }

  @Override
  public @NonNls String getTemplateName() {
    return null;
  }

  @Override
  public @NonNls String getConcatenationTemplateName() {
    return null;
  }

  @Override
  public boolean isActive(@NotNull PsiFile context) {
    return GwtModulesManager.getInstance(context.getProject()).isUnderGwtModule(context.getVirtualFile());
  }

  @Override
  public boolean canShowJavaCodeInfo() {
    return false;
  }

  @Override
  public String suggestPropertyKey(final @NotNull String value) {
    return GwtI18nUtil.suggestPropertyKey(value, PsiNameHelper.getInstance(myProject), LanguageLevel.HIGHEST);
  }

  @Override
  public List<String> suggestPropertiesFiles(Set<Module> contextModules) {
    final List<String> paths = new ArrayList<>();

    PropertiesReferenceManager.getInstance(myProject).processAllPropertiesFiles((baseName, propertiesFile) -> {
      if (myI18nManager.getPropertiesInterface(propertiesFile) != null) {
        paths.add(FileUtil.toSystemDependentName(propertiesFile.getVirtualFile().getPath()));
      }
      return true;
    });
    return paths;
  }

  private void addMethod(final PsiClass anInterface, final String key, final PsiExpression[] parameters) throws IncorrectOperationException {
    if (parameters.length == 0 && containsMethod(anInterface, key)) {
      return;
    }

    PsiFile psiFile = anInterface.getContainingFile();
    FileModificationService.getInstance().prepareFileForWrite(psiFile);
    final VirtualFile virtualFile = psiFile.getVirtualFile();
    LOG.assertTrue(virtualFile != null);

    GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(myProject, psiFile.getVirtualFile());
    GwtVersion gwtVersion = GwtFacet.getGwtVersion(gwtFacet);

    PsiMethod method = GwtI18nUtil.addMethod(anInterface, key, gwtVersion);
    CodeStyleManager.getInstance(myProject).reformat(method);
    if (parameters.length > 0) {
      TemplateBuilderImpl builder = new TemplateBuilderImpl(method);
      CreateFromUsageUtils.setupMethodParameters(method, builder, parameters[0], PsiSubstitutor.EMPTY, parameters);
      method = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(method);

      final OpenFileDescriptor descriptor = new OpenFileDescriptor(myProject, virtualFile, method.getTextRange().getStartOffset());

      Document document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
      LOG.assertTrue(document != null);
      RangeMarker methodRange = document.createRangeMarker(method.getTextRange());
      final Editor editor = FileEditorManager.getInstance(myProject).openTextEditor(descriptor, true);
      final Template template = builder.buildTemplate();

      editor.getCaretModel().moveToOffset(methodRange.getStartOffset());
      editor.getDocument().deleteString(methodRange.getStartOffset(), methodRange.getEndOffset());

      ApplicationManager.getApplication().invokeLater(() -> TemplateManager.getInstance(myProject).startTemplate(editor, template));
    }
  }

  private boolean containsMethod(PsiClass anInterface, String key) {
    final PsiNameHelper nameHelper = PsiNameHelper.getInstance(myProject);
    final String methodName = GwtI18nUtil.convertPropertyName2MethodName(key, nameHelper, PsiUtil.getLanguageLevel(anInterface));
    final PsiMethod[] methods = anInterface.findMethodsByName(methodName, true);
    for (PsiMethod method : methods) {
      if (method.getParameterList().getParametersCount() == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable PropertyCreationHandler getPropertyCreationHandler() {
    return new GwtPropertyCreationHandler();
  }

  @Override
  public @Nullable I18nizedTextGenerator getI18nizedTextGenerator() {
    return new GwtI18nizedTextGenerator();
  }

  private class GwtI18nizedTextGenerator extends I18nizedTextGenerator {
    private static final @NonNls String GET_LOCALIZABLE_INSTANCE_TEMPLATE = "(({0}) " + GwtSdkUtil.GWT_CLASS_NAME + ".create({0}.class))";

    @Override
    public @NotNull String getI18nizedText(final @NotNull String propertyKey,
                                           final @Nullable PropertiesFile propertiesFile,
                                           final PsiElement context) {
      return getI18nizedConcatenationText(propertyKey, "", propertiesFile, context);
    }

    private String getI18nizedText(@Nullable @NonNls String qualifier, final String propertyKey, final PsiElement context, String parameters) {
      if (qualifier == null) {
        qualifier = "constants";
      }
      String methodName = GwtI18nUtil.convertPropertyName2MethodName(propertyKey,
                                                                     PsiNameHelper.getInstance(myPsiManager.getProject()),
                                                                     PsiUtil.getLanguageLevel(context));
      return qualifier + "." + methodName + "(" + parameters + ")";
    }

    private String getLocalizableInstance(final @NotNull PsiClass anInterface, final @NotNull PsiElement context) {
      PsiClassType type = JavaPsiFacade.getInstance(context.getProject()).getElementFactory().createType(anInterface);
      Set<String> expressions = JavaI18nUtil.suggestExpressionOfType(type, context);
      Iterator<String> iterator = expressions.iterator();
      if (iterator.hasNext()) {
        return iterator.next();
      }
      return MessageFormat.format(GET_LOCALIZABLE_INSTANCE_TEMPLATE, anInterface.getQualifiedName());
    }

    @Override
    public @NotNull String getI18nizedConcatenationText(final @NotNull String propertyKey,
                                                        final @NotNull String parametersString,
                                                        final @Nullable PropertiesFile propertiesFile,
                                                        final PsiElement context) {
      String qualifier = null;
      if (propertiesFile != null) {
        PsiClass anInterface = myI18nManager.getPropertiesInterface(propertiesFile);
        if (anInterface != null) {
          qualifier = getLocalizableInstance(anInterface,context);
        }
      }

      return getI18nizedText(qualifier, propertyKey, context, parametersString);
    }
  }

  private class GwtPropertyCreationHandler implements PropertyCreationHandler {
    @Override
    public void createProperty(final @NotNull Project project,
                               final @NotNull Collection<PropertiesFile> propertiesFiles,
                               final @NotNull @NlsSafe String key,
                               final @NotNull String value,
                               final @NotNull UExpression @NotNull [] parameters) throws IncorrectOperationException {
      JavaI18nUtil.DEFAULT_PROPERTY_CREATION_HANDLER.createProperty(project, propertiesFiles, key, value, parameters);
      Iterator<PropertiesFile> iterator = propertiesFiles.iterator();
      if (iterator.hasNext()) {
        PropertiesFile propertiesFile = iterator.next();
        PsiClass anInterface = myI18nManager.getPropertiesInterface(propertiesFile);
        if (anInterface != null) {
          PsiExpression[] expressions =
            Arrays.stream(parameters)
              .map(expression -> expression.getSourcePsi())
              .filter(sourcePsi -> sourcePsi instanceof PsiExpression)
              .map(sourcePsi -> (PsiExpression)sourcePsi)
              .toArray(PsiExpression[]::new);
          addMethod(anInterface, key, expressions.length == parameters.length ? expressions : PsiExpression.EMPTY_ARRAY);
        }
      }
    }
  }
}
