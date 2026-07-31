package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlUndefinedElementFixProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GwtUiUndefinedElementFixProvider extends XmlUndefinedElementFixProvider {
  @Override
  public IntentionAction[] createFixes(@NotNull XmlAttribute attribute) {
    final XmlTag tag = attribute.getParent();
    if (tag != null) {
      final XmlElementDescriptor descriptor = tag.getDescriptor();
      if (descriptor instanceof GwtUiComponentDescriptor) {
        List<IntentionAction> fixes = ((GwtUiComponentDescriptor)descriptor).getCreateSetterQuickFixes(attribute);
        return fixes.toArray(IntentionAction.EMPTY_ARRAY);
      }
    }
    return null;
  }
}
