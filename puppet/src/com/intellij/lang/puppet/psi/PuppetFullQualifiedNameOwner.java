package com.intellij.lang.puppet.psi;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public interface PuppetFullQualifiedNameOwner {
  @Nullable
  @NlsSafe
  String getName();

  @Nullable
  @NlsSafe
  String getNamespaceName();

  /**
   * @return a string to separate namespace from name in FQN
   */
  default @NlsSafe String getNamespaceDelimiter() {
    return SEPARATOR;
  }

  default @Nullable @NlsSafe String getFullQualifiedName() {
    String name = getName();
    if (StringUtil.isEmpty(name)) {
      return null;
    }
    String namespaceName = getNamespaceName();
    return namespaceName == null ? name : namespaceName + getNamespaceDelimiter() + name;
  }
}
