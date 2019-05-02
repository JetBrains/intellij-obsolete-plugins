
package com.intellij.struts.dom.converters;

import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class FormBeanScopeProvider extends ScopeProvider {

  @Override
  @Nullable
  public DomElement getScope(@NotNull final DomElement element) {
    final DomFileElement<StrutsConfig> root = DomUtil.getFileElement(element);
    final MergingFileDescription<StrutsConfig> description = (MergingFileDescription)root.getFileDescription();
    final StrutsConfig config = description.getMergedRoot(root);
    return config.getFormBeans();
  }
}
