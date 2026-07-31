package com.intellij.gwt.clientBundle.css;

import com.intellij.psi.PsiElement;
import com.intellij.psi.css.CssPropertyDescriptor;
import com.intellij.psi.css.CssPropertyValue;
import com.intellij.psi.css.descriptor.value.CssValueDescriptor;
import com.intellij.psi.css.impl.util.scheme.CssElementDescriptorFactory2;
import com.intellij.psi.css.impl.util.table.AbstractCssPropertyDescriptor;
import com.intellij.psi.css.impl.util.table.CssPropertyValueImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class GwtPropertyDescriptor extends AbstractCssPropertyDescriptor implements CssPropertyDescriptor {
  private final @NotNull String myPropertyName;
  private final @NotNull CssPropertyValueImpl myValue;
  private static final @NotNull CssValueDescriptor OUR_VALUE_DESCRIPTOR = CssElementDescriptorFactory2.getInstance().createAnyValueDescriptor(1, 1, null);

  public GwtPropertyDescriptor(@NotNull String propertyName) {
    myPropertyName = propertyName;
    myValue = new GwtCssPropertyValue();
  }

  @Override
  public @NotNull CssPropertyValue getValue() {
    return myValue;
  }

  @Override
  public boolean isShorthandValue() {
    return false;
  }

  @Override
  public @NotNull String getPropertyName() {
    return myPropertyName;
  }

  @Override
  public boolean getInherited() {
    return false;
  }

  @Override
  public @NotNull CssValueDescriptor getValueDescriptor() {
    return OUR_VALUE_DESCRIPTOR;
  }

  private static class GwtCssPropertyValue extends CssPropertyValueImpl {
    GwtCssPropertyValue() {
      super(Type.OR);
    }

    @Override
    public boolean isValueBelongs(@Nullable PsiElement element) {
      return true;
    }
  }

  @Override
  public @Nullable Icon getIcon() {
    return super.getIcon();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GwtPropertyDescriptor that)) return false;

    if (!myPropertyName.equals(that.myPropertyName)) return false;
    if (!myValue.equals(that.myValue)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myPropertyName.hashCode();
    result = 31 * result + (myValue.hashCode());
    return result;
  }
}
