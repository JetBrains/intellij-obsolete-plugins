/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.play.PlayIcons;
import com.intellij.play.completion.beans.NameValueDescriptor;
import com.intellij.play.completion.beans.PlayTagDescriptor;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static com.intellij.play.completion.beans.NameValueDescriptor.create;
import static com.intellij.play.completion.beans.NameValueDescriptor.createAction;
import static com.intellij.play.completion.beans.NameValueDescriptor.createExpression;
import static com.intellij.play.completion.beans.NameValueDescriptor.createStringExpression;
import static com.intellij.play.completion.beans.PlayTagDescriptor.create;

public final class PlayCompletionUtils {

  private static final Set<PlayTagDescriptor> plays = new HashSet<>();

  static {
    plays.add(create("a", createAction()));
    plays.add(create("authenticityToken", true));
    plays.add(create("cache", createStringExpression()));
    plays.add(create("doBody", true));
    plays.add(create("doLayout", true));
    plays.add(create("if", createExpression()));
    plays.add(create("ifError", createStringExpression()));
    plays.add(PlayTagDescriptor.create("ifErrors"));
    plays.add(create("else", createExpression()));
    plays.add(create("elseif", createExpression()));
    plays.add(create("error", true,
                     createStringExpression(),
                     createStringExpression("field")));
    plays.add(create("errorClass", true, createStringExpression()));
    plays.add(PlayTagDescriptor.create("errors"));
    plays.add(create("extends", true, createStringExpression()));
    plays.add(create("field", createStringExpression()));
    plays.add(create("form", createAction(),
                     createStringExpression("method").setRequired(false),
                     createStringExpression("enctype ").setRequired(false),
                     createStringExpression("id").setRequired(false)));
    plays.add(create("get", true, createStringExpression()));
    plays.add(create("i18n", true));
    plays.add(create("ifnot", createExpression()));
    plays.add(create("include", true, createStringExpression()));
    plays.add(create("jsAction", true, createAction()));
    plays.add(create("list",
                     createExpression(), //  #{list users, as:'user'}
                     create("items").setRequired(false), // #{list items:users, as:'user'}
                     createStringExpression("as").setRequired(false)
    ));
    plays.add(create("option", createExpression()));
    plays.add(create("script", true,
                     createStringExpression(),   //#{script 'jquery-1.4.2.min.js' /}
                     createStringExpression("src"),
                     createStringExpression("id").setRequired(false),
                     createStringExpression("charset").setRequired(false)
    ));
    plays.add(create("render", true, createStringExpression()));
    plays.add(create("select",
                     createStringExpression(),
                     createStringExpression("name").setRequired(false),
                     create("items ").setRequired(false),
                     create("value").setRequired(false),
                     create("labelProperty").setRequired(false),
                     create("valueProperty").setRequired(false)

    ));
    plays.add(create("set", true));
    plays.add(create("stylesheet", true,
                     createStringExpression(),
                     createStringExpression("src").setRequired(false),
                     createStringExpression("id").setRequired(false),
                     createStringExpression("media").setRequired(false),
                     createStringExpression("title").setRequired(false)

    ));
    plays.add(PlayTagDescriptor.create("verbatim"));
  }

  public static Set<PlayTagDescriptor> getPredefinedTagDescriptors() {
    return plays;
  }

  public static Set<PlayTagDescriptor> getTagDescriptors(@Nullable Module module) {
    Set<PlayTagDescriptor> descriptors = new HashSet<>();
    descriptors.addAll(getPredefinedTagDescriptors());
    if (module != null) {
      descriptors.addAll(getCustomTagDescriptors(module));
      descriptors.addAll(getFastTagDescriptors(module));
    }
    return descriptors;
  }

  public static Set<PlayTagDescriptor> getCustomTagDescriptors(@NotNull Module module) {
    return ContainerUtil.map2Set(PlayPathUtils.getCustomTags(module).keySet(), fqn -> create(fqn, true));
  }

  public static Set<PlayTagDescriptor> getFastTagDescriptors(@NotNull Module module) {
    return ContainerUtil.map2Set(PlayPathUtils.getFastTags(module), fastTagDescriptor -> create(fastTagDescriptor.getFqn(), true));
  }

  @Nullable
  public static PlayTagDescriptor findTagDescriptor(@Nullable PlayTag tag) {
    if (tag == null) return null;
    for (final PlayTagDescriptor tagDescriptor : PlayCompletionUtils.getTagDescriptors(ModuleUtilCore.findModuleForPsiElement(tag))) {
      if (tagDescriptor.getTagName().equals(tag.getName())) {
        return tagDescriptor;
      }
    }
    return null;
  }

  public static LookupElementBuilder createLookupElement(@NotNull final PlayTagDescriptor descriptor) {
    return createLookupElement(descriptor, "");
  }

  public static LookupElementBuilder createTagNameLookupElement(@NotNull final PlayTagDescriptor descriptor) {
    return LookupElementBuilder.create(descriptor.getTagName()).
      withPresentableText(descriptor.getTagName()).bold().
      withIcon(PlayIcons.Play).
      withTypeText(descriptor.getPresentableText(), true);
  }

  public static LookupElementBuilder createLookupElement(@NotNull final PlayTagDescriptor descriptor, @NotNull String prefix) {

    return LookupElementBuilder.create(prefix + descriptor.getTagName() + descriptor.getTailText()).
      withLookupString(descriptor.getTagName()).
      withPresentableText(descriptor.getTagName()).bold().
      withIcon(PlayIcons.Play).
      withTypeText(descriptor.getPresentableText(), true).
      withInsertHandler(new InsertHandler<>() {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
          PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getEditor().getDocument());

          PsiElement current = context.getFile().findElementAt(context.getStartOffset());
          final PlayTag tag = PsiTreeUtil.getContextOfType(current, PlayTag.class, true);

          if (tag == null) return;

          final PlayTagDescriptor tagDescriptor = PlayCompletionUtils.findTagDescriptor(tag);
          if (tagDescriptor != null && tagDescriptor.getDescriptors().length > 0) {

            final PsiElement nameElement = tag.getNameElement();
            if (nameElement != null) {
              final CaretModel caretModel = context.getEditor().getCaretModel();
              caretModel.moveToOffset(nameElement.getTextOffset() + nameElement.getTextLength() + 1);
              final NameValueDescriptor[] nameValueDescriptors = tagDescriptor.getDescriptors();
              if (nameValueDescriptors.length == 1) {
                final NameValueDescriptor valueDescriptor = nameValueDescriptors[0];
                if (valueDescriptor.getName() == null) {
                  if (valueDescriptor.isStringExpression()) {
                    context.getDocument().insertString(caretModel.getOffset(), "''");
                  }
                  else if (valueDescriptor.isActionPreferred()) {
                    context.getDocument().insertString(caretModel.getOffset(), "@");
                  }
                  else {
                    return;
                  }

                  PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getEditor().getDocument());
                  caretModel.moveToOffset(caretModel.getOffset() + 1);
                }
              }
              AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
            }
          }
        }
      });
  }

  @Nullable
  private static Template getTemplate(@NotNull PlayTagDescriptor descriptor) {
    final NameValueDescriptor[] descriptors = descriptor.getDescriptors();

    return null;
  }
}
