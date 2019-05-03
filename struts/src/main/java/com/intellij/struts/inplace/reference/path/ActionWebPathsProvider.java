/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.path;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.javaee.web.CustomServletReferenceAdapter;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.javaee.web.ServletMappingType;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.ActionMappings;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ConstantFunction;
import com.intellij.util.xml.ElementPresentationManager;
import com.intellij.util.xml.highlighting.ResolvingElementQuickFix;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ActionWebPathsProvider extends CustomServletReferenceAdapter {

  private final boolean myPrefixAllowed;

  public ActionWebPathsProvider() {
    myPrefixAllowed = true;
  }

  public ActionWebPathsProvider(final boolean prefixAllowed) {
    myPrefixAllowed = prefixAllowed;
  }

  @Override
  @Nullable
  public PathReference createWebPath(final String path, final @NotNull PsiElement element, final ServletMappingInfo info) {
    StrutsModel model = StrutsManager.getInstance().getStrutsModel(element);
    if (model != null) {
      final Action action = model.resolveActionURL(path);
      if (action != null) {
        return new PathReference(path, new ConstantFunction<>(StrutsApiIcons.ActionMapping)) {
          @Override
          public PsiElement resolve() {
            return action.getXmlTag();
          }
        };
      }
    }
    return null;
  }

  @Override
  protected PsiReference[] createReferences(final @NotNull PsiElement element,
                                            final int offset,
                                            final String text,
                                            final @Nullable ServletMappingInfo info,
                                            final boolean soft) {
    final StrutsModel model = StrutsManager.getInstance().getStrutsModel(element);
    if (model != null) {
      return new PsiReference[]{new ActionReference(element, offset, text, info == null || info.equals(model.getServletMappingInfo()) ? info : null, soft)};
    }
    return PsiReference.EMPTY_ARRAY;
  }

  private class ActionReference extends PsiReferenceBase<PsiElement>
    implements EmptyResolveMessageProvider, LocalQuickFixProvider {


    ActionReference(@NotNull final PsiElement element, int offset, String text, ServletMappingInfo info, final boolean soft) {

      super(element, new TextRange(offset, offset + text.length()), soft);
      if (info != null && (info.getType() != ServletMappingType.PATH || myPrefixAllowed)) {
        final TextRange range = info.getNameRange(text);
        if (range != null) {
          setRangeInElement(range.shiftRight(offset));
        }
      }
    }

    @Override
    @Nullable
    public PsiElement resolve() {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myElement);
      if (model == null) {
        return null;
      }
      String url = getValue();
      if (!url.startsWith("/")) url = "/" + url;
      Action action = model.findAction(url);
      if (action != null) {
        return action.getXmlTag();
      }
      else {
        return null;
      }
    }

    @Override
    @NotNull
    public Object[] getVariants() {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myElement);
      if (model == null) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
      }
      final ServletMappingInfo info = model.getServletMappingInfo();
      List<Action> actions = model.getActions();
      return ElementPresentationManager.getInstance().createVariants(actions, action -> {
        final String actionPath = action.getPath().getValue();
        if (actionPath == null) {
          return null;
        }
        else {
          return myPrefixAllowed ? info.addMapping(actionPath) : actionPath;
        }
      }, Iconable.ICON_FLAG_VISIBILITY);
    }

    @Override
    @NotNull
    public String getUnresolvedMessagePattern() {
      return StrutsBundle.message("cannot.resolve.action", getValue());
    }

    @Nullable
    private ResolvingElementQuickFix createFix() {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myElement);
      if (model == null) {
        return null;
      }
      final String text = getValue();
      final ActionMappings scope = model.getMergedModel().getActionMappings();
      return ResolvingElementQuickFix.createFix(text, Action.class, scope);
    }

    @Override
    public LocalQuickFix[] getQuickFixes() {
      final ResolvingElementQuickFix quickFix = createFix();
      return quickFix == null ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[] {quickFix};
    }
  }
}
