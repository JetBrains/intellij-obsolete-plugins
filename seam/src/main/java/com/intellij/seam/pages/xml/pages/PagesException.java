package com.intellij.seam.pages.xml.pages;


import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

@Presentation(icon = "AllIcons.Nodes.ExceptionClass")
public interface PagesException extends RedirectOwner, SeamPagesDomElement {
  @NotNull
  @Attribute("class")
  @ExtendClass(value = "java.lang.Exception", instantiatable = false)
  GenericAttributeValue<PsiClass> getClazz();

  @NotNull
  EndConversation getEndConversation();

  @NotNull
  HttpError getHttpError();
}
