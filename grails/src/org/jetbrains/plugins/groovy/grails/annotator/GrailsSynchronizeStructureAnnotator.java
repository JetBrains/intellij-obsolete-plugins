// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.annotator;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsConstants;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.config.GrailsSettingSynchronizer;
import org.jetbrains.plugins.grails.config.GrailsSettingsService;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public final class GrailsSynchronizeStructureAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof GroovyFile)) return;

    final VirtualFile file = ((GroovyFile)element).getVirtualFile();
    if (file == null || !file.getName().equals(GrailsUtils.BUILD_CONFIG)) return;

    final VirtualFile confDir = file.getParent();
    if (confDir == null || !confDir.getName().equals(GrailsUtils.CONF_DIRECTORY)) return;

    final VirtualFile grailsApp = confDir.getParent();
    if (grailsApp == null || !grailsApp.getName().equals(GrailsConstants.APP_DIRECTORY)) return;

    final GrailsApplication application = GrailsApplicationManager.getInstance(element.getProject()).findApplication(grailsApp);
    if (!(application instanceof Grails2Application grails2Application)) return;

    if (GrailsSettingSynchronizer.isUpdateSettingRunning(grails2Application.getProject())) return;
    if (!GrailsSettingsService.getGrailsSettings(grails2Application.getModule()).isBuildConfigOutdated(element.getText())) return;

    holder.newAnnotation(HighlightSeverity.WARNING, GrailsBundle.message("synchronize.structure.annotator.error.text.outdated"))
    .fileLevel()
    .withFix(new IntentionAction() {
      @Override
      public @NotNull String getText() {
        return GrailsBundle.message("synchronize.structure.annotator.fix.text.apply.changes");
      }

      @Override
      public @NotNull String getFamilyName() {
        return GrailsBundle.message("synchronize.structure.annotator.fix.family.apply");
      }

      @Override
      public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        return true;
      }

      @Override
      public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        GrailsFramework.forceSynchronizationSetting(grails2Application.getModule());
      }

      @Override
      public boolean startInWriteAction() {
        return false;
      }
    })
    .withFix(new IntentionAction() {
      @Override
      public @NotNull String getText() {
        return GrailsBundle.message("synchronize.structure.annotator.ignore.intention");
      }

      @Override
      public @NotNull String getFamilyName() {
        return getText();
      }

      @Override
      public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        return true;
      }

      @Override
      public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        FileDocumentManager.getInstance().saveAllDocuments();
        GrailsSettingsService.getGrailsSettings(grails2Application.getModule()).updateBuildConfig(psiFile.getText());
        DaemonCodeAnalyzer.getInstance(project).restart(this);
      }

      @Override
      public @Nullable PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
        return null;
      }

      @Override
      public boolean startInWriteAction() {
        return true;
      }
    }).create();
  }
}
