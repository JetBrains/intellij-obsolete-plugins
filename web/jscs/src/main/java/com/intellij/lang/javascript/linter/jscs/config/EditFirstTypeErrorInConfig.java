package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.javascript.linter.JSLinterEditConfigFileAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
* @author Irina.Chernushina on 10/21/2014.
*/
public class EditFirstTypeErrorInConfig extends JSLinterEditConfigFileAction {
  public EditFirstTypeErrorInConfig(@NotNull VirtualFile virtualFile) {
    super(virtualFile);
  }

  @Override
  public void invokeReally(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    preparation(project);
    super.invokeReally(project, editor, file);
  }

  private void preparation(Project project) {
    final Document document = FileDocumentManager.getInstance().getDocument(myVirtualFile);
    if (document != null) {
      final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
      if (psiFile instanceof JsonFile) {
        final List<Integer> offsets = new ArrayList<>();
        JscsConfigFileAnnotator.annotate((JsonFile)psiFile, (elt, message) -> {
          if (message.contains("Expected") && containsTypeName(message)) {
            offsets.add(elt.getTextRange().getStartOffset());
          }
        });

        for (Integer offset : offsets) {
          final JsonProperty property = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, JsonProperty.class, false);
          if (property != null) {
            setProperty(property.getName());
            return;
          }
        }
      }
    }
  }

  private static boolean containsTypeName(@NotNull final String s) {
    for (ValueType type : ValueType.values()) {
      if (s.contains(type.getNameOrFixedValue())) return true;
    }
    return false;
  }
}
