package com.intellij.plugins.jboss.arquillian.junit.quickFix;

import com.intellij.jarRepository.RepositoryUnresolvedReferenceQuickFixProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;

public final class ArquillianJUnitUnresolvedReference extends RepositoryUnresolvedReferenceQuickFixProvider {
  @Override
  protected boolean isSuspectedName(@NotNull String fqTypeName) {
    return fqTypeName.startsWith("org.jboss.arquillian.") && !fqTypeName.startsWith("org.jboss.arquillian.testng.");
  }

  @NotNull
  @Override
  protected RepositoryLibraryDescription getLibraryDescription() {
    return RepositoryLibraryDescription.findDescription(
      "org.jboss.arquillian.junit",
      "arquillian-junit-container");
  }
}
