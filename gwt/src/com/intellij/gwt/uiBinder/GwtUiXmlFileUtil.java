package com.intellij.gwt.uiBinder;

import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtUiXmlFileUtil {
  private static final Key<CachedValue<MultiValuesMap<String, XmlAttributeValue>>> GWT_UI_FIELDS_ATTRIBUTES_MAP = Key.create("GWT_UI_FIELDS_ATTRIBUTES_MAP");

  private GwtUiXmlFileUtil() {
  }

  public static @Nullable XmlTag findTagForField(final @NotNull XmlFile file, @NotNull String fieldName) {
    final MultiValuesMap<String, XmlAttributeValue> map = getFieldNameToAttributeMap(file);
    if (map == null) return null;

    final XmlAttributeValue value = map.getFirst(fieldName);
    if (value != null) {
      return PsiTreeUtil.getParentOfType(value, XmlTag.class);
    }
    return null;
  }

  public static @Nullable MultiValuesMap<String, XmlAttributeValue> getFieldNameToAttributeMap(XmlFile file) {
    if (!UiBinderUtil.isUiXmlFile(file)) return null;

    return CachedValuesManager.getManager(file.getProject()).getCachedValue(file, GWT_UI_FIELDS_ATTRIBUTES_MAP, new UiFieldsMapCachedValueProvider(file), false);
  }

  private static class UiFieldsMapCachedValueProvider implements CachedValueProvider<MultiValuesMap<String, XmlAttributeValue>> {
    private final XmlFile myFile;

    UiFieldsMapCachedValueProvider(XmlFile file) {
      myFile = file;
    }

    @Override
    public Result<MultiValuesMap<String, XmlAttributeValue>> compute() {
      final MultiValuesMap<String, XmlAttributeValue> result = new MultiValuesMap<>();
      myFile.accept(new XmlRecursiveElementVisitor() {
        @Override
        public void visitXmlAttributeValue(@NotNull XmlAttributeValue value) {
          final XmlAttribute attribute = PsiTreeUtil.getParentOfType(value, XmlAttribute.class);
          if (attribute != null) {
            final String name = attribute.getLocalName();
            if (name.equals(UiBinderUtil.UI_FIELD_ATTRIBUTE) && UiBinderUtil.hasUiBinderNamespace(attribute)) {
              result.put(value.getValue(), value);
            }
            else if (name.equals(UiBinderUtil.UI_TYPE_ATTRIBUTE)) {
              final XmlTag tag = attribute.getParent();
              if (UiBinderUtil.hasUiBinderNamespace(attribute) && tag != null && tag.getLocalName().equals(UiBinderUtil.UI_STYLE_TAG)
                  && tag.getAttribute(UiBinderUtil.UI_FIELD_ATTRIBUTE) == null) {
                result.put(UiStyleElement.DEFAULT_FIELD_NAME, value);
              }
            }
          }
        }
      });
      return Result.create(result, myFile);
    }
  }
}
