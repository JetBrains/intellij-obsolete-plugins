package com.intellij.seam.pages.xml.pages;

import com.intellij.openapi.paths.PathReference;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

public interface PagesViewIdOwner extends SeamPagesDomElement {
    @NotNull
    @Required(nonEmpty = true, value = false)
    GenericAttributeValue<PathReference> getViewId();
}
