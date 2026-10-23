package com.intellij.guice;

import com.intellij.guice.actions.NewGuiceProviderAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks what the New | Guice actions and templates produce.
 */
public class GuiceNewFileActionsTest extends GuiceTestBase {

  private static String template(String name) throws IOException {
    try (InputStream in = GuiceNewFileActionsTest.class.getResourceAsStream("/fileTemplates/j2ee/" + name)) {
      assertNotNull("Template " + name, in);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** M13: a binding annotation goes on fields, parameters and @Provides methods; a scope annotation on types and methods. */
  public void testAnnotationTemplatesTargetWhatGuiceAccepts() throws IOException {
    String binding = template("GuiceNewBindingAnnotation.java.ft");
    assertThat(binding).contains("ElementType.FIELD");
    assertThat(binding).contains("ElementType.PARAMETER");
    assertThat(binding).contains("ElementType.METHOD");
    assertThat(binding).contains("BindingAnnotation");

    String scope = template("GuiceNewScopeAnnotation.java.ft");
    assertThat(scope).contains("ElementType.TYPE");
    assertThat(scope).contains("ElementType.METHOD");
    assertThat(scope).doesNotContain("ElementType.PARAMETER");
    assertThat(scope).contains("ScopeAnnotation");
  }

  /** §6.4: the New | Guice group shows up for a source directory of a module with Guice, and nowhere else. */
  public void testActionGroupIsVisibleForSourceDirectoriesOfGuiceModulesOnly() throws IOException {
    PsiDirectory sourceDir = myFixture.getPsiManager().findDirectory(myFixture.getTempDirFixture().findOrCreateDir("pkg"));
    assertNotNull(sourceDir);
    var group = new com.intellij.guice.actions.GuiceActionGroup();

    var inSourceRoot = updateWith(group, sourceDir, myFixture.getModule());
    assertThat(inSourceRoot.isVisible()).isTrue();

    // No module in the context: the group hides itself (no Guice to check against).
    var withoutModule = updateWith(group, sourceDir, null);
    assertThat(withoutModule.isVisible()).isFalse();

    // No IDE view (the action is not invoked from a project-view directory): hidden.
    var event = com.intellij.testFramework.TestActionEvent.createTestEvent(group, com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
      .add(com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT, getProject())
      .add(com.intellij.openapi.actionSystem.PlatformCoreDataKeys.MODULE, myFixture.getModule())
      .build());
    group.update(event);
    assertThat(event.getPresentation().isVisible()).isFalse();
  }

  private com.intellij.openapi.actionSystem.Presentation updateWith(com.intellij.guice.actions.GuiceActionGroup group,
                                                                     PsiDirectory directory,
                                                                     com.intellij.openapi.module.Module module) {
    com.intellij.ide.IdeView view = new com.intellij.ide.IdeView() {
      @Override
      public PsiDirectory @org.jetbrains.annotations.NotNull [] getDirectories() {
        return new PsiDirectory[]{directory};
      }

      @Override
      public PsiDirectory getOrChooseDirectory() {
        return directory;
      }
    };
    var builder = com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
      .add(com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT, getProject())
      .add(com.intellij.openapi.actionSystem.LangDataKeys.IDE_VIEW, view);
    if (module != null) {
      builder.add(com.intellij.openapi.actionSystem.PlatformCoreDataKeys.MODULE, module);
    }
    var event = com.intellij.testFramework.TestActionEvent.createTestEvent(group, builder.build());
    group.update(event);
    return event.getPresentation();
  }

  /** M14: the provider action creates a class in the package of the directory, with an {@code @Override get()}. */
  public void testNewProviderIsCreatedInThePackageOfTheDirectory() {
    myFixture.addFileToProject("pkg/Svc.java", "package pkg;\npublic interface Svc {}\n");
    PsiDirectory directory = myFixture.getPsiManager().findDirectory(myFixture.findFileInTempDir("pkg"));
    assertNotNull(directory);

    PsiClass provider = WriteAction.compute(() -> NewGuiceProviderAction.createProviderClass(directory, "SvcProvider", "pkg.Svc"));

    assertThat(provider.getQualifiedName()).isEqualTo("pkg.SvcProvider");
    PsiJavaFile file = (PsiJavaFile)provider.getContainingFile();
    assertThat(file.getPackageName()).isEqualTo("pkg");
    String text = file.getText();
    assertThat(text).contains("package pkg;");
    assertThat(text).contains("implements Provider<Svc>");
    assertThat(text).contains("@Override");
    assertThat(text).contains("public Svc get()");
    assertThat(text).doesNotContain("com.google.inject.Provider<");
  }
}
