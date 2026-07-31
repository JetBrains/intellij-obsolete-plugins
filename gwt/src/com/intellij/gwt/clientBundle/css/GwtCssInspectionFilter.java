package com.intellij.gwt.clientBundle.css;

import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssDeclarationElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.CssTerm;
import com.intellij.psi.css.CssTermList;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.css.impl.CssTermTypes;
import com.intellij.psi.css.inspections.CssApiBaseInspection;
import com.intellij.psi.css.inspections.CssInspectionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager.findDeclarations;

public final class GwtCssInspectionFilter extends CssInspectionFilter {
  @Override
  public boolean isSupported(@NotNull Class<? extends CssApiBaseInspection> clazz, @NotNull PsiElement context) {
    return true;
  }

  @Override
  public boolean isValueShouldBeValidatedWithCssScheme(@Nullable CssTermList value) {
    if (value != null) {
      final PsiFile containingFile = value.getContainingFile();
      if (containingFile instanceof StylesheetFile) {
        for (CssTerm term : value.getTerms()) {
          if (term.getTermType() == CssTermTypes.IDENT) {
            final String variableName = term.getText();
            Collection<GwtCssDeclarationElementBase> gwtSpecificDeclarations = 
              findDeclarations((StylesheetFile)containingFile, variableName, GwtCssDeclarationElementBase.class);
            if (!gwtSpecificDeclarations.isEmpty()) {
              return false;
            }
          }
        }
      }
    }
    return super.isValueShouldBeValidatedWithCssScheme(value);
  }
}
