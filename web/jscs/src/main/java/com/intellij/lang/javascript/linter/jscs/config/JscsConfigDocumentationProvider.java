package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.HintHint;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author Irina.Chernushina on 10/14/2014.
 */
public class JscsConfigDocumentationProvider implements DocumentationProvider {

  @Nullable
  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    return null;//todo?
  }

  @Nullable
  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    final PsiFile file = element.getContainingFile();
    if (file == null) return null;
    final VirtualFile virtualFile = file.getViewProvider().getVirtualFile();
    if (FileTypeRegistry.getInstance().isFileOfType(virtualFile, JscsConfigFileType.INSTANCE)) {
      final JsonProperty property = JSLinterConfigFileUtil.getProperty(element);
      if (property != null) {
        final PsiElement keyElement = JSLinterConfigFileUtil.getFirstChildAsStringLiteral(property);
        if (keyElement != null && (keyElement == element || property == element)) {
          final String name = StringUtil.unquoteString(keyElement.getText());
          final String description;
          final JscsOption option = JscsOption.safeValueOf(name);
          JscsDocumentationReader reader = JscsDocumentationReader.getInstance();
          if (option != null) {
            description = reader.getDescription(option);
          }
          else {
            description = reader.getInnerDescription(name);
          }
          if (description != null) {
            Component comp = null;
            final HintHint hintHint = new HintHint(comp, new Point(0, 0));
            return HintUtil.prepareHintText(description, hintHint);
          }
        }
      }
    }
    return null;
  }
}
