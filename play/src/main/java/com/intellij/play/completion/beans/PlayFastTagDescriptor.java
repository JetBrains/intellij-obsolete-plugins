package com.intellij.play.completion.beans;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayFastTagDescriptor {
  @NotNull private final PsiMethod myMethod;
  @Nullable private final JamStringAttributeElement<String> myNamespace;

  public PlayFastTagDescriptor(@NotNull PsiMethod method, @Nullable JamStringAttributeElement<String> namespace) {
    myMethod = method;
    assert myMethod.getName().startsWith("_");

    myNamespace = namespace;
  }

  @NotNull
  public PsiMethod getMethod() {
    return myMethod;
  }

  @Nullable
  public JamStringAttributeElement<String> getNamespace() {
    return myNamespace;
  }

  @Nullable
  public String getNamespaceValue() {
    JamStringAttributeElement<String> namespace = getNamespace();

    return (namespace == null || namespace.getValue() == null ? "" : namespace.getValue());
  }

  public String getName() {
     return myMethod.getName().substring(1);
  }

  public String getFqn() {
    return getNamespaceValue()+ "." + getName();
  }
}
