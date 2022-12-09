package com.intellij.dmserver.intention;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.editor.UnitsCollector;
import com.intellij.dmserver.libraries.LibrariesDialogCreator;
import com.intellij.dmserver.manifest.HeaderValuePartProcessor;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UnitResolver implements HeaderValuePartProcessor<ProblemsHolder> {

  @NonNls
  private static final Pattern TRIM_PATTERN = Pattern.compile("(\\s*)(\\S*?)\\s*");

  @Override
  public void process(HeaderValuePart headerValue, ProblemsHolder problemsHolder) {
    Matcher trimMatcher = TRIM_PATTERN.matcher(headerValue.getText());
    if (!trimMatcher.matches()) {
      return;
    }
    int start = trimMatcher.group(1).length();
    String unitName = trimMatcher.group(2);
    int length = unitName.length();
    if (length == 0) {
      return;
    }

    if (getUnitsCollector(AvailableBundlesProvider.getInstance(headerValue.getProject())).isUnitAvailable(unitName)) {
      return;
    }

    registerProblem(problemsHolder, headerValue, new TextRange(start, start + length), unitName);
  }

  protected void registerProblem(ProblemsHolder problemsHolder, HeaderValuePart headerValue, TextRange textRange, String unitName) {
    LocalQuickFix[] quickFixes = LibrariesDialogCreator.isDialogAvailable(headerValue.getProject())
                                 ? new LocalQuickFix[]{new DownloadUnitQuickFix(unitName)}
                                 : LocalQuickFix.EMPTY_ARRAY;
    problemsHolder.registerProblem(headerValue, textRange, getProblemMessage(unitName), quickFixes);
  }

  @Nls
  protected abstract String getProblemMessage(String unitName);

  protected abstract UnitsCollector getUnitsCollector(AvailableBundlesProvider provider);

  private static final class DownloadUnitQuickFix implements LocalQuickFix {

    private final String myPackageName;

    private DownloadUnitQuickFix(String unitName) {
      myPackageName = unitName;
    }

    @Override
    @NotNull
    public String getName() {
      return DmServerBundle.message("UnknownImportedPackageInspection.DownloadQuickFix.name");
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return DmServerBundle.message("UnknownImportedPackageInspection.DownloadPackageQuickFix.family.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      LibrariesDialogCreator.showDialog(project, myPackageName);
    }
  }
}
