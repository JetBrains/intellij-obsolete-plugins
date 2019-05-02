/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.struts;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.ElementPresentationUtil;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.ui.JBUI;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author peter
 */
public class StrutsIconProvider extends IconProvider {
  // IconProvider -------------------------------------------------------------
  // original code posted by Sascha Weinreuter

  @Override
  @Nullable
  public Icon getIcon(@NotNull final PsiElement element, final int flags) {
    if (!(element instanceof PsiClass)) {
      return null;
    }
    // IconProvider queries non-physical PSI as well (e.g. completion items)
    if (!element.isPhysical()) {
      return null;
    }

    Icon strutsIcon = null;

    // handle JAVA classes
    PsiClass psiClass = (PsiClass) element;

    if (InheritanceUtil.isInheritor(psiClass, "org.apache.struts.action.Action")) {
      strutsIcon = StrutsApiIcons.ActionMapping_small;
    } else if (InheritanceUtil.isInheritor(psiClass, "org.apache.struts.action.ActionForm")) {
      strutsIcon = StrutsApiIcons.FormBean_small;
    } else if (InheritanceUtil.isInheritor(psiClass, "org.apache.struts.tiles.Controller")) {
      strutsIcon = StrutsApiIcons.Tiles.Tile_small;
    }

    // match? build new layered icon
    if (strutsIcon != null) {
      LayeredIcon icon = new LayeredIcon(2);
      Icon original = PsiClassImplUtil.getClassIcon(flags, psiClass);
      icon.setIcon(original, 0);
      icon.setIcon(strutsIcon, 1, StrutsIconsOverlays.OVERLAY_ICON_OFFSET_X, StrutsIconsOverlays.OVERLAY_ICON_OFFSET_Y);
      RowIcon rowIcon = new RowIcon(2);
      rowIcon.setIcon(JBUI.scale(icon), 0);
      return ElementPresentationUtil.addVisibilityIcon((PsiClass) element, flags, rowIcon);
    }

    return null;
  }
}
