package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.tree.IElementType;

/**
 * @author Anna Bulenkova
 */
public class PuppetElementType extends IElementType {
  public PuppetElementType(final String debug_description) {
    super(debug_description, PuppetLanguage.INSTANCE);
  }
}
