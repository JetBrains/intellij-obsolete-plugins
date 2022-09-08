package com.intellij.seam.structure;

import com.intellij.javaee.module.view.nodes.JavaeeClassNodeDescriptor;
import com.intellij.javaee.module.view.nodes.JavaeeNodeDescriptor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamRole;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class SeamComponentNodeDescriptor extends JavaeeClassNodeDescriptor {
  private final SeamJamComponent mySeamComponent;

  public SeamComponentNodeDescriptor(SeamJamComponent seamComponent, JavaeeNodeDescriptor parentDescriptor, Object parameters) {
    super(seamComponent.getPsiElement(), parentDescriptor, parameters);

    mySeamComponent = seamComponent;
  }

  @Override
  public String getNewNodeText() {
     if (!mySeamComponent.isValid()) return "";

    return StringUtil.isEmptyOrSpaces(mySeamComponent.getComponentName()) ? super.getNewNodeText() : mySeamComponent.getComponentName();
  }
  @Override
  public boolean isValid() {
    return mySeamComponent != null && mySeamComponent.isValid();
  }

  @Override
  public Object @NotNull [] getEqualityObjects() {
    return new Object[] { mySeamComponent };
  }

  @Override
  public String getNewTooltip() {
    if (!mySeamComponent.isValid()) return "";

    StringBuilder tooltip = new StringBuilder();
    final String name = mySeamComponent.getComponentName();
    final PsiType psiType = mySeamComponent.getComponentType();
    final SeamComponentScope scope = mySeamComponent.getComponentScope();
    final Collection<SeamJamRole> roles = mySeamComponent.getRoles();

    if (name != null) {
      tooltip.append("<tr><td><strong>name:</strong></td><td>").append(name).append("</td></tr>");
    }
    if (psiType != null) {
      tooltip.append("<tr><td><strong>class:</strong></td><td>").append(psiType.getPresentableText()).append("</td></tr>");
    }

    if (scope != null) {
      tooltip.append("<tr><td><strong>scope:</strong></td><td>").append(scope.getValue()).append("</td></tr>");
    }

    if (!roles.isEmpty()) {
      tooltip.append("<tr><td valign=\"top\"><strong>roles:</strong></td><td><table>");
      for (SeamJamRole role : roles) {
        final String roleName = role.getName();
        final SeamComponentScope roleScope = role.getScope();

        tooltip.append("<tr><td>").append(roleName == null ? "unknown" : roleName).append("</td><td>").
          append(roleScope == null ? "" : roleScope.getValue()).append("</td></tr>");
      }
      tooltip.append("</table></td></tr>");
    }

    if (tooltip.length() != 0) {
      return "<html><table>" + tooltip + "</table></html>";
    }

    return super.getNewTooltip();
  }

  @Override
  public JavaeeNodeDescriptor @NotNull [] getChildren() {
    return JavaeeNodeDescriptor.EMPTY_ARRAY;
  }

  @Override
  protected void doUpdate() {
    super.doUpdate();
    final String textExt = getNewNodeTextExt();
    if (textExt != null) {
      addColoredFragment(" (" + getNewNodeTextExt() + ")", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
  }

  @Nullable
  protected String getNewNodeTextExt() {
    PsiClass psiClass = getJamElement();
    if (psiClass == null || !psiClass.isValid()) return null;

    return psiClass.getQualifiedName();
  }
}
