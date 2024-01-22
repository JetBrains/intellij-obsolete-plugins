package com.intellij.plugins.jboss.arquillian.testng.quickFix;

import com.intellij.jarRepository.RepositoryUnresolvedReferenceQuickFixProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;

public class ArquillianTestNGUnresolvedReference extends RepositoryUnresolvedReferenceQuickFixProvider {
  @Override
  protected boolean isSuspectedName(@NotNull String fqTypeName) {
    return fqTypeName.startsWith("org.jboss.arquillian.") && !fqTypeName.startsWith("org.jboss.arquillian.junit.");
  }

  @NotNull
  @Override
  protected RepositoryLibraryDescription getLibraryDescription() {
    return RepositoryLibraryDescription.findDescription(
      "org.jboss.arquillian.testng",
      "arquillian-testng-container");
  }
}
