package com.intellij.lang.puppet.util;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.lang.puppet.psi.mixins.PuppetVariableMixin;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.lang.puppet.PuppetTokenTypes.CLASS_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.FUNCTION_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.NAMESPACE_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.RESOURCE_INSTANCE_DECLARATION;
import static com.intellij.lang.puppet.PuppetTokenTypes.TYPE_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.VAR_WRAPPER;


public final class PuppetTypesInfoUtil {
  private static final Map<IElementType, @Nls String> TYPE_TO_NAME_MAP = getTypeToNameMap();

  public static @Nullable @Nls String getTypeName(PsiElement element) {

    IElementType type = PsiUtilCore.getElementType(element);

    if (TYPE_TO_NAME_MAP.containsKey(type)) {
      return TYPE_TO_NAME_MAP.get(type);
    }

    if (VAR_WRAPPER.equals(type)) {
      if (((PuppetVariableMixin)element).isParameter()) {
        return PuppetBundle.message("puppet.type.names.parameter");
      }
      else {
        return PuppetBundle.message("puppet.type.names.variable");
      }
    }

    if (element instanceof PuppetLazyProxyLightElement) {
      return ((PuppetLazyProxyLightElement)element).getTypeName();
    }

    return null;
  }

  private static Map<IElementType, String> getTypeToNameMap() {
    Map<IElementType, String> result = new HashMap<>();
    result.put(RESOURCE_INSTANCE_DECLARATION, PuppetBundle.message("puppet.type.names.resource_instance"));
    result.put(FUNCTION_DEFINITION, PuppetBundle.message("puppet.type.names.function_definition"));
    result.put(CLASS_DEFINITION, PuppetBundle.message("puppet.type.names.class_definition"));
    result.put(NAMESPACE_DEFINITION, PuppetBundle.message("puppet.type.names.namespace_definition"));
    result.put(TYPE_DEFINITION, PuppetBundle.message("puppet.type.names.resource_definition"));

    return result;
  }
}
