package com.intellij.lang.puppet.ide.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetIcons;
import com.intellij.lang.puppet.project.PuppetModule;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetParametrizedDeclaration;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.PuppetVariableLightElement;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.intellij.icons.AllIcons.Nodes.Class;
import static com.intellij.icons.AllIcons.Nodes.Field;
import static com.intellij.icons.AllIcons.Nodes.Folder;
import static com.intellij.icons.AllIcons.Nodes.Lambda;
import static com.intellij.icons.AllIcons.Nodes.Parameter;
import static com.intellij.icons.AllIcons.Nodes.Variable;

public final class PuppetLookupElements {
  private static final Icon VARIABLE_ICON = Variable;
  private static final Icon FACT_ICON = Field;
  private static final Icon PARAMETER_ICON = Parameter;
  private static final Icon FUNCTION_ICON = Lambda;
  private static final Icon MODULE_ICON = Folder;
  private static final Icon CLASS_ICON = Class;

  private static final PuppetLookupCachingRenderer VARIABLE_RENDERER = new PuppetLookupCachingRenderer() {
    @Override
    public void calcPresentation(LookupElement lookupElement, LookupElementPresentation presentation) {

      PsiElement psiElement = lookupElement.getPsiElement();
      assert psiElement instanceof PuppetVariable : "Got " + psiElement + " instead of PuppetVariable";

      PuppetVariable element = (PuppetVariable)psiElement;
      String typeName;

      boolean isBold = false;
      Icon elementIcon = VARIABLE_ICON;
      if (element.isMetaparameter()) {
        typeName = PuppetBundle.message("puppet.lookup.type.metaparameter");
        elementIcon = PARAMETER_ICON;
        isBold = true;
      }
      else if (element.isCoreFact()) {
        typeName = PuppetBundle.message("puppet.lookup.type.core.fact");
        elementIcon = FACT_ICON;
        isBold = true;
      }
      else if (element.isBuiltIn()) {
        typeName = PuppetBundle.message("puppet.lookup.type.builtin.variable");
        isBold = true;
      }
      else if (element.isParameter()) {
        PsiNamedElement anchor = PsiTreeUtil.getStubOrPsiParentOfType(element, PuppetParametrizedDeclaration.class);
        if (anchor == null) {
          anchor = element.getContainingFile();
        }
        typeName = anchor.getName();
        elementIcon = PARAMETER_ICON;
      }
      else if (element instanceof PuppetVariableLightElement) {
        typeName = ((PuppetVariableLightElement)element).getLookupTypeText();
        isBold = ((PuppetVariableLightElement)element).getLookupBoldness();
      }
      else {
        PsiFile containingFile = element.getContainingFile();
        typeName = containingFile == null ? "" : containingFile.getName();
      }

      presentation.setIcon(elementIcon);
      presentation.setItemText("$" + lookupElement.getLookupString());
      presentation.setItemTextBold(isBold);
      presentation.setTypeGrayed(true);
      presentation.setTypeText(typeName);
    }
  };

  private static final PuppetLookupCachingRenderer EXTERNAL_FACT_RENDERER = new PuppetLookupCachingRenderer() {
    @Override
    protected void calcPresentation(LookupElement lookupElement, LookupElementPresentation presentation) {
      presentation.setIcon(FACT_ICON);
      presentation.setItemText("$" + lookupElement.getLookupString());
      PsiElement element = lookupElement.getPsiElement();
      if (element != null) {
        PsiFile file = element.getContainingFile();
        if (file != null) {
          presentation.setTypeGrayed(true);
          presentation.setTypeText(file.getName());
        }
      }
    }
  };

  public static LookupElement forModule(@NotNull PuppetModule puppetModule) {
    return LookupElementBuilder.create(puppetModule.getShortName())
      .withIcon(PuppetIcons.PuppetLogo)
      .withTypeText(puppetModule.getName());
  }

  public static @NotNull LookupElement forExternalFact(@NotNull String name, @Nullable PsiElement reference) {


    LookupElementBuilder builder = reference == null ? LookupElementBuilder.create(name)
                                                     : LookupElementBuilder.create(reference, name);
    return builder.withRenderer(EXTERNAL_FACT_RENDERER);
  }

  public static @NotNull LookupElementBuilder forExternalParameter(@NotNull String name,
                                                                   @NotNull String typeName,
                                                                   @Nullable PsiElement reference) {
    // fixme add => ... , insert handler
    // fixme add decoration for metaparameter
    return (reference == null ? LookupElementBuilder.create(name)
                              : LookupElementBuilder.create(reference, name))
      .withIcon(PARAMETER_ICON)
      .withPresentableText("$" + name)
      .withTypeText(typeName, true);
  }

  public static @NotNull LookupElement forExternalFunction(@NotNull String name, @Nullable PsiElement reference) {
    // fixme add parens
    return (reference == null ? LookupElementBuilder.create(name)
                              : LookupElementBuilder.create(reference, name))
      .withIcon(FUNCTION_ICON)
      .withTypeText(PuppetBundle.message("puppet.lookup.type.function"), true);
  }

  public static @NotNull LookupElement forModuleDir(@NotNull PsiDirectory dir) {
    LookupElementBuilder lookup = LookupElementBuilder.create(dir, dir.getName())
      .withIcon(MODULE_ICON)
      .withTypeText(PuppetBundle.message("puppet.lookup.type.module"), true);
    return addStringInsertHandler(lookup, "::", false);
  }

  public static @Nullable LookupElementBuilder forVariable(@NotNull PuppetVariable element, boolean forceFqn) {
    final String name = forceFqn ? element.getFullQualifiedName() : element.getName();
    return name == null ? null : LookupElementBuilder.create(element, name).withRenderer(VARIABLE_RENDERER);
  }

  public static @Nullable LookupElement forClass(@NotNull PuppetClassDefinition element, boolean capitalize) {
    String name = element.getFullQualifiedName();
    if (name == null) {
      return null;
    }

    if (capitalize) {
      name = PuppetQualifiedNamesUtil.capitalizePuppetName(name);
    }

    return forClass(element, name);
  }

  public static @NotNull LookupElement forClass(@NotNull PsiElement element, @NotNull String name) {
    return LookupElementBuilder.create(element, name)
      .withIcon(CLASS_ICON)
      .withTypeText(PuppetBundle.message("puppet.lookup.type.class"), true);
  }

  public static @Nullable LookupElement forTypeDefinition(@NotNull PuppetTypeDefinition element, boolean capitalize) {
    String name = element.getFullQualifiedName();
    if (name == null) {
      return null;
    }

    return forTypeDefinition(element, name, capitalize);
  }

  public static @Nullable LookupElement forResourceInstance(@NotNull PuppetDelegatingLightNamedElement resourceInstanceDelegate,
                                                            @NotNull String type) {
    String name = resourceInstanceDelegate.getName();
    return StringUtil.isEmpty(name) ? null : LookupElementBuilder
      .create(name)
      .withIcon(PuppetIcons.ResourceInstance)
      .withTypeText(type);
  }

  public static @NotNull LookupElement forTypeDefinition(@NotNull PsiElement element, @NotNull String name, boolean capitalize) {
    return LookupElementBuilder.create(element, capitalize ? PuppetQualifiedNamesUtil.capitalizePuppetName(name) : name)
      .withIcon(PuppetIcons.ResourceType)
      .withTypeText(PuppetBundle.message("puppet.lookup.type.resource.type"), true);
  }

  public static @NotNull LookupElement forDataType(@NotNull String name) {
    return LookupElementBuilder.create(name)
      .bold()
      .withIcon(PuppetIcons.DataType)
      .withTypeText(PuppetBundle.message("puppet.lookup.type.data.type"), true);
  }


  @Contract("null, _, _ -> null; !null, _, _ -> !null")
  public static LookupElementBuilder addStringInsertHandler(@Nullable LookupElementBuilder lookupElement,
                                                            final @NotNull String s, final boolean insertSpace) {
    if (lookupElement == null) {
      return null;
    }

    return lookupElement.withInsertHandler(new InsertHandler<>() {
      @Override
      public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        Document document = context.getDocument();
        int textLength = document.getTextLength();
        int tailOffset = context.getTailOffset();
        CharSequence text = document.getCharsSequence();

        int offsetToReplace = findNonSpaceChar(text, tailOffset);
        offsetToReplace = normalizeSpaceBefore(document, tailOffset, offsetToReplace, insertSpace);

        if (offsetToReplace + s.length() > textLength ||
            !text.subSequence(offsetToReplace, offsetToReplace + s.length()).toString().equals(s)) {
          document.insertString(offsetToReplace, s);
        }

        int endOffset = normalizeSpaceAfter(document, offsetToReplace + s.length(), text, insertSpace);

        context.getEditor().getCaretModel().moveToOffset(endOffset);
      }

      private int findNonSpaceChar(CharSequence text, int offset) {
        while (offset < text.length() && text.charAt(offset) == ' ') {
          offset++;
        }
        return offset;
      }
    });
  }

  private static int normalizeSpaceAfter(Document document, int offsetAfterInsert, CharSequence text, boolean needSpace) {
    if (needSpace) {
      if (text.length() < offsetAfterInsert + 1 || text.charAt(offsetAfterInsert) != ' ') {
        document.insertString(offsetAfterInsert, " ");
      }
      return offsetAfterInsert + 1;
    }
    return offsetAfterInsert;
  }

  private static int normalizeSpaceBefore(Document document, int tailOffset, int offsetToReplace, boolean needSpace) {
    if (needSpace) {
      if (tailOffset <= offsetToReplace - 1) {
        document.deleteString(tailOffset, offsetToReplace - 1);
      }
      else {
        document.insertString(tailOffset, " ");
      }
      return tailOffset + 1;
    }
    else {
      document.deleteString(tailOffset, offsetToReplace);
      return tailOffset;
    }
  }
}
