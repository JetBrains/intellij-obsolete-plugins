package com.intellij.gwt.uiBinder.declarations;

import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.openapi.util.Key;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class UiXmlDeclarationsManager {
  private static final Key<ParameterizedCachedValue<List<UiXmlVariableDeclaration>, XmlFile>> UI_DECLARATIONS_KEY = Key.create("GWT_UI_DECLARATIONS");
  private static final UiStyleElementsCachedValueProvider PROVIDER = new UiStyleElementsCachedValueProvider();

  private UiXmlDeclarationsManager() {
  }

  public static List<UiXmlVariableDeclaration> getDeclarations(@NotNull XmlFile file) {
    if (!UiBinderUtil.isUiXmlFile(file)) return Collections.emptyList();

    return CachedValuesManager.getManager(file.getProject()).getParameterizedCachedValue(file, UI_DECLARATIONS_KEY, PROVIDER, false, file);
  }

  public static @Nullable UiXmlVariableDeclaration findDeclaration(@NotNull XmlFile file, @NotNull String fieldName) {
    for (UiXmlVariableDeclaration declaration : getDeclarations(file)) {
      if (fieldName.equals(declaration.getVariableName())) {
        return declaration;
      }
    }
    return null;
  }

  private static class UiStyleElementsCachedValueProvider implements ParameterizedCachedValueProvider<List<UiXmlVariableDeclaration>, XmlFile> {
    @Override
    public CachedValueProvider.Result<List<UiXmlVariableDeclaration>> compute(XmlFile param) {
      final List<UiXmlVariableDeclaration> result = new SmartList<>();
      param.accept(new XmlRecursiveElementVisitor() {
        @Override
        public void visitXmlTag(@NotNull XmlTag tag) {
          final String localName = tag.getLocalName();
          if (tag.getNamespace().equals(UiBinderUtil.UI_BINDER_NAMESPACE)) {
            switch (localName) {
              case UiBinderUtil.UI_STYLE_TAG -> result.add(new UiStyleElement(tag));
              case UiBinderUtil.UI_WITH_TAG -> result.add(new UiXmlVariableDeclarationImpl(tag, null));
              case "import" -> result.addAll(UiImportVariable.createElements(tag));
            }
          }
          else {
            String fieldName = tag.getAttributeValue(UiBinderUtil.UI_FIELD_ATTRIBUTE, UiBinderUtil.UI_BINDER_NAMESPACE);
            if (fieldName != null) {
              String type = UiBinderUtil.getComponentClassName(tag);
              result.add(new UiXmlVariableDeclarationImpl(tag, fieldName, type));
            }
          }
          super.visitXmlTag(tag);
        }
      });
      return CachedValueProvider.Result.create(result, param);
    }
  }
}
