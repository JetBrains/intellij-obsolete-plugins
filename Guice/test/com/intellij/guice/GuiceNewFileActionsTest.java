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
