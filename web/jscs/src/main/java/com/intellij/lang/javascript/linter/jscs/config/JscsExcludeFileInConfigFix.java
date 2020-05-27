package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.json.JsonElementTypes;
import com.intellij.json.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Irina.Chernushina on 10/14/2014.
 */
public class JscsExcludeFileInConfigFix implements HighPriorityAction, IntentionAction {
  @NotNull
  private final VirtualFile myConfigFile;
  @NotNull
  private final String myFileName;
  @NotNull
  private final String myRelativePath;

  public JscsExcludeFileInConfigFix(@NotNull final VirtualFile configFile,
                                    @NotNull final String fileName,
                                    @NotNull final String relativePath) {
    myConfigFile = configFile;
    myFileName = fileName;
    myRelativePath = relativePath;
  }

  @NotNull
  @Override
  public String getText() {
    return "Exclude " + myFileName + " from JSCS analysis in " + myConfigFile.getName();
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return "Exclude file(s) from analysis in config file";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile jsPsiFile) throws IncorrectOperationException {
    final Document document = FileDocumentManager.getInstance().getDocument(myConfigFile);
    if (document != null) {
      final JsonElementGenerator generator = new JsonElementGenerator(project);

      final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
      if (psiFile != null) {
        final Collection<JsonProperty> properties = PsiTreeUtil.findChildrenOfType(psiFile, JsonProperty.class);
        for (JsonProperty property : properties) {
          if (JscsOption.excludeFiles.name().equals(property.getName())) {
            final JsonValue value = property.getValue();
            if (value != null) {
              updateExistingProperty(project, generator, value, psiFile, jsPsiFile);
            }
            return;
          }
        }
        createNewProperty(project, generator, psiFile, jsPsiFile);
      }
    }
  }

  private void updateExistingProperty(Project project, JsonElementGenerator generator, JsonValue value, PsiFile psiFile, PsiFile jsPsiFile) {
    final JsonArray array = ObjectUtils.tryCast(value, JsonArray.class);
    if (array == null) {
      OpenSourceUtil.navigate(true, PsiNavigationSupport.getInstance().createNavigatable(project, myConfigFile,
                                                                                         value.getTextOffset()));
      return;
    }
    final JsonStringLiteral literal = generator.createStringLiteral(myRelativePath);
    final List<JsonValue> list = array.getValueList();
    final PsiElement added;
    if (list.isEmpty()) {
      ASTNode rBracketNode = array.getNode().findChildByType(JsonElementTypes.L_BRACKET);
      if (rBracketNode == null) {
        return;
      }
      added = array.addAfter(literal, rBracketNode.getPsi());
    } else {
      final JsonValue anchor = list.get(list.size() - 1);
      final PsiElement comma = array.addAfter(generator.createComma(), anchor);
      added = array.addAfter(literal, comma);
    }
    reformatAndNavigate(project, added, psiFile, jsPsiFile);
  }

  private void createNewProperty(Project project, JsonElementGenerator generator, PsiFile psiFile, PsiFile jsPsiFile) {
    final PsiElement obj = psiFile.getFirstChild();
    final JsonObject jsonObject = ObjectUtils.tryCast(obj, JsonObject.class);
    if (jsonObject == null) return;

    final String propertyText = "[\"" + myRelativePath + "\"]";
    final JsonProperty createdProperty = generator.createProperty(JscsOption.excludeFiles.name(), propertyText);
    final List<JsonProperty> list = jsonObject.getPropertyList();
    PsiElement added = null;
    if (list.isEmpty()) {
      added = jsonObject.replace(generator.createObject("\"" + JscsOption.excludeFiles.name() + "\": " + propertyText));
      CodeStyleManager.getInstance(project).reformat(psiFile);
    } else {
      final JsonProperty anchor = list.get(list.size() - 1);
      final PsiElement comma = jsonObject.addAfter(generator.createComma(), anchor);
      added = jsonObject.addAfter(createdProperty, comma);
    }
    reformatAndNavigate(project, added, psiFile, jsPsiFile);
  }

  private void reformatAndNavigate(@NotNull final Project project,
                                   @Nullable final PsiElement created,
                                   @NotNull PsiFile config,
                                   @NotNull PsiFile jsPsiFile) {
    if (created == null) {
      OpenSourceUtil.navigate(true, new OpenFileDescriptor(project, myConfigFile));
      return;
    }

    final JsonProperty property = JSLinterConfigFileUtil.getProperty(created);
    int offset = created.getTextOffset();
    if (property != null) {
      offset = property.getTextOffset();
      final JsonValue value = property.getValue();
      if (value != null) {
        offset = value.getTextOffset();
      }
      // this line throws exception, property.getParent() is ok
      //CodeStyleManager.getInstance(project).reformat(property);
    } else {
      final Integer offsetObj = ObjectUtils.doIfCast(config.getFirstChild(), JsonObject.class, obj -> {
        final List<JsonProperty> list = obj.getPropertyList();
        for (JsonProperty jsonProperty : list) {
          if (JscsOption.excludeFiles.name().equals(StringUtil.stripQuotesAroundValue(jsonProperty.getName()))) {
            return jsonProperty.getValue() == null ? null : jsonProperty.getValue().getTextOffset();
          }
        }
        return null;
      });
      offset = offsetObj == null ? offset : offsetObj;
    }
    OpenSourceUtil.navigate(true, PsiNavigationSupport.getInstance().createNavigatable(project, myConfigFile, offset));
    DaemonCodeAnalyzer.getInstance(project).restart(jsPsiFile);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
