package com.intellij.seam.model.converters;

import com.intellij.psi.PsiType;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamProperty;
import com.intellij.seam.model.xml.components.SeamValue;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.WrappingConverter;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SeamPropertyValueConverter extends WrappingConverter {

  @Override
  @Nullable
  public Converter getConverter(@NotNull final GenericDomValue element) {
    final String stringValue = element.getStringValue();

    if (stringValue!= null && SeamCommonUtils.isElText(stringValue)) return null;

    final PsiType type = getValueType(element);

    if (type == null) return null;

    final GenericDomValueConvertersRegistry registry = SeamDomModelManager.getInstance(element.getManager().getProject()).getValueConvertersRegistry();

    return registry.getConverter(element, type);
  }

  @Nullable
  public static PsiType getValueType(final GenericDomValue element) {
    if (element instanceof SeamValue) {
      final SeamProperty seamProperty = element.getParentOfType(SeamProperty.class, false);

      if (seamProperty != null) {
        return seamProperty.getPropertyType();
      }
    }
    return null;
  }
}

