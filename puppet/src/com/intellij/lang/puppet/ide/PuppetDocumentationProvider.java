package com.intellij.lang.puppet.ide;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.mixins.PuppetVariableMixin;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PuppetDocumentationProvider implements DocumentationProvider {

  @Override
  public @Nls @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    PsiElement comment = PsiTreeUtil.prevVisibleLeaf(element);
    if (!(comment instanceof PsiComment)) {
      return null;
    }

    if (element instanceof PuppetClassDefinition || element instanceof PuppetTypeDefinition || element instanceof PuppetVariableMixin) {
      return uncomment(((PsiComment)comment));
    }
    return null;
  }

  @Override
  public @Nullable PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    if (!(object instanceof LookupElement)) {
      return null;
    }
    Object candidate = ((LookupElement)object).getObject();
    if (!(candidate instanceof PsiElement)) {
      return null;
    }
    return ((PsiElement)candidate);
  }

  private static @NlsSafe @Nullable String uncomment(@Nullable PsiComment comment) {
    if (comment == null) {
      return null;
    }

    boolean isHtml = false;

    List<String> text = new ArrayList<>();
    while (true) {
      String elementText = comment.getText();
      if (elementText.contains("</p>")) {
        isHtml = true;
      }

      text.addAll(ContainerUtil.reverse(StringUtil.split(elementText, "\n")));

      PsiElement prev = PsiTreeUtil.prevVisibleLeaf(comment);
      if (!(prev instanceof PsiComment)) {
        break;
      }
      comment = ((PsiComment)prev);
    }

    StringBuilder result = new StringBuilder();
    if (!isHtml) {
      result.append("<pre><code>");
    }

    for (String s : ContainerUtil.reverse(text)) {
      int index = s.indexOf("#");
      result.append(s.substring(index + 1)).append("\n");
    }

    if (!isHtml) {
      result.append("</pre></code>");
    }

    return result.toString();
  }
}
