package com.intellij.gwt.clientBundle.css.language.psi;

import com.intellij.psi.css.CssElement;

import java.util.List;

public interface GwtCssExternal extends CssElement {
  List<String> getSelectorNames();
}
