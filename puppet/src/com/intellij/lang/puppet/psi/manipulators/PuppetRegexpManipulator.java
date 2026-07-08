package com.intellij.lang.puppet.psi.manipulators;

import com.intellij.lang.puppet.psi.mixins.PuppetRegexpElementMixin;
import com.intellij.lang.puppet.util.PuppetElementFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PuppetRegexpManipulator extends AbstractElementManipulator<PuppetRegexpElementMixin> {

  private static final String CREATING_PREFIX = "if $a =~ ";

  @Override
  public PuppetRegexpElementMixin handleContentChange(@NotNull PuppetRegexpElementMixin element,
                                                      @NotNull TextRange range,
                                                      String newContent) throws IncorrectOperationException {
    String oldText = element.getText();
    PsiFile file = element.getContainingFile();
    newContent = StringUtil.escapeSlashes(newContent);
    String newText = CREATING_PREFIX + oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());
    PsiElement fromText = PuppetElementFactory.createLeafElement(file.getProject(), newText, CREATING_PREFIX.length() + 1);
    if (fromText instanceof PuppetRegexpElementMixin) {
      return (PuppetRegexpElementMixin)element.replace(fromText);
    }
    return element;
  }
}
