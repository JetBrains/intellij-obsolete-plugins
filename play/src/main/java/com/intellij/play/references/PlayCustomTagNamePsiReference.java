package com.intellij.play.references;

import com.intellij.codeInsight.daemon.quickFix.CreateFileFix;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.completion.beans.PlayFastTagDescriptor;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class PlayCustomTagNamePsiReference extends PsiReferenceBase<PsiElement> implements LocalQuickFixProvider {
  private final PlayTag myPlayTag;
  private final String myTagName;
  @NotNull private final String myFqn;
  private static final String CUSTOM_TAGS_DIR = "tags";

  public PlayCustomTagNamePsiReference(@NotNull PlayTag playTag, @NotNull String tagName, @NotNull String fqn) {
    super(playTag, TextRange.from(playTag.getText().indexOf(tagName), tagName.length()));
    myPlayTag = playTag;
    myTagName = tagName;
    myFqn = fqn;
  }


  @Override
  public PsiElement resolve() {
    final Module module = ModuleUtilCore.findModuleForPsiElement(myPlayTag);

    if (module != null) {
      final Map<String, PsiFile> tags = PlayPathUtils.getCustomTags(module);
      for (Map.Entry<String, PsiFile> tag : tags.entrySet()) {
        PsiFile tagFile = tag.getValue();
        if (tagFile instanceof PlayPsiFile && myFqn.equals(tag.getKey())) {
          return tagFile;
        }
      }
      for (PlayFastTagDescriptor descriptor : PlayPathUtils.getFastTags(module)) {
        if (myFqn.equals(descriptor.getFqn())) {
          return descriptor.getMethod();
        }
      }
    }
    return null;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    String tagName = getNewTagName(newElementName);
    String fqnName = myFqn.contains(".") ? StringUtil.getPackageName(myFqn) + "." + tagName : tagName;
    ((PlayTag)getElement()).setName(fqnName);
    return getElement();
  }

  private static String getNewTagName(String newElementName) {
    // here we rename method (_myMethodName) for fastTags or play file for standard tags
    if (newElementName.contains(".")) return FileUtilRt.getNameWithoutExtension(newElementName);
    if (newElementName.startsWith("_")) return newElementName.substring(1);

    return newElementName;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    final Module module = ModuleUtilCore.findModuleForPsiElement(myPlayTag);

    if (module != null) {
      if (element instanceof PlayPsiFile) {
        final Map<String, PsiFile> tags = PlayPathUtils.getCustomTags(module);
        for (Map.Entry<String, PsiFile> entry : tags.entrySet()) {
          if (element.equals(entry.getValue())) {
            ((PlayTag)getElement()).setName(entry.getKey());
            return element;
          }
        }
      }
      if (element instanceof PsiMethod) {
        final Set<PlayFastTagDescriptor> tags = PlayPathUtils.getFastTags(module);
        for (PlayFastTagDescriptor fastTag : tags) {
          if (element.equals(fastTag.getMethod())) {
            ((PlayTag)getElement()).setName(fastTag.getFqn());
            return element;
          }
        }
      }
    }
    return super.bindToElement(element);
  }

  @Override
  public @NotNull LocalQuickFix @Nullable [] getQuickFixes() {
    final LocalQuickFix fix = createNewCustomTagFix();
    return fix == null ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[]{fix};
  }

  @Nullable
  private LocalQuickFix createNewCustomTagFix() {
    Module module = ModuleUtilCore.findModuleForPsiElement(myPlayTag);
    if (module == null) return null;

    PsiDirectory viewsDirectory = PlayPathUtils.getViewsDirectory(module);
    if (viewsDirectory == null) return null;

    final String newFileName = CUSTOM_TAGS_DIR + "/" + myFqn.replace(".", "/") + ".html";

    return new CreateFileFix(newFileName, viewsDirectory, "");
  }
}
