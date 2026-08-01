package com.intellij.gwt.uiBinder;

import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.openapi.util.Key;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtUiXmlFileUtil {
  private static final Key<CachedValue<MultiMap<String, XmlAttributeValue>>> GWT_UI_FIELDS_ATTRIBUTES_MAP = Key.create("GWT_UI_FIELDS_ATTRIBUTES_MAP");

  private GwtUiXmlFileUtil() {
  }

  public static @Nullable XmlTag findTagForField(final @NotNull XmlFile file, @NotNull String fieldName) {
    final MultiMap<String, XmlAttributeValue> map = getFieldNameToAttributeMap(file);
    if (map == null) return null;

    final XmlAttributeValue value = ContainerUtil.getFirstItem(map.get(fieldName));
    if (value != null) {
      return PsiTreeUtil.getParentOfType(value, XmlTag.class);
    }
    return null;
  }

  public static @Nullable MultiMap<String, XmlAttributeValue> getFieldNameToAttributeMap(XmlFile file) {
    if (!UiBinderUtil.isUiXmlFile(file)) return null;

    return CachedValuesManager.getManager(file.getProject()).getCachedValue(file, GWT_UI_FIELDS_ATTRIBUTES_MAP, new UiFieldsMapCachedValueProvider(file), false);
  }

  private static class UiFieldsMapCachedValueProvider implements CachedValueProvider<MultiMap<String, XmlAttributeValue>> {
    private final XmlFile myFile;

    UiFieldsMapCachedValueProvider(XmlFile file) {
      myFile = file;
    }

    @Override
    public Result<MultiMap<String, XmlAttributeValue>> compute() {
      final MultiMap<String, XmlAttributeValue> result = MultiMap.createSet();
      myFile.accept(new XmlRecursiveElementVisitor() {
        @Override
        public void visitXmlAttributeValue(@NotNull XmlAttributeValue value) {
          final XmlAttribute attribute = PsiTreeUtil.getParentOfType(value, XmlAttribute.class);
          if (attribute != null) {
            final String name = attribute.getLocalName();
            if (name.equals(UiBinderUtil.UI_FIELD_ATTRIBUTE) && UiBinderUtil.hasUiBinderNamespace(attribute)) {
              result.putValue(value.getValue(), value);
            }
            else if (name.equals(UiBinderUtil.UI_TYPE_ATTRIBUTE)) {
              final XmlTag tag = attribute.getParent();
              if (UiBinderUtil.hasUiBinderNamespace(attribute) && tag != null && tag.getLocalName().equals(UiBinderUtil.UI_STYLE_TAG)
                  && tag.getAttribute(UiBinderUtil.UI_FIELD_ATTRIBUTE) == null) {
                result.putValue(UiStyleElement.DEFAULT_FIELD_NAME, value);
              }
            }
          }
        }
      });
      return Result.create(result, myFile);
    }
  }
}
