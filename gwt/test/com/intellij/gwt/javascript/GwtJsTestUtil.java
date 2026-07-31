package com.intellij.gwt.javascript;

import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.lang.Language;
import com.intellij.lang.javascript.JSLanguageDialect;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.intellij.psi.LanguageSubstitutors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtJsTestUtil {
  public static final String GWT_FILE_EXTENSION = "GwtJavaScript";

  private static final GwtTestLanguageSubstitutor SUBSTITUTOR = new GwtTestLanguageSubstitutor();
  private static class GwtTestLanguageSubstitutor extends LanguageSubstitutor {
    @Nullable
    @Override
    public Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
      final JSLanguageDialect dialect = GwtLanguageDialect.GWT_DIALECT;
      return GWT_FILE_EXTENSION.equals(file.getExtension()) ? dialect : null;
    }
  }

  public static void setUpGwtDialect() {
    if (LanguageSubstitutors.getInstance().allForLanguage(PlainTextLanguage.INSTANCE).contains(SUBSTITUTOR)) {
      return;
    }
    LanguageSubstitutors.getInstance().addExplicitExtension(PlainTextLanguage.INSTANCE, SUBSTITUTOR);
  }
}
