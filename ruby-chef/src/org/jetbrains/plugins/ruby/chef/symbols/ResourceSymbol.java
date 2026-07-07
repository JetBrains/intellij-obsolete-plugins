package org.jetbrains.plugins.ruby.chef.symbols;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.v2.ClassModuleSymbol;
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer;
import org.jetbrains.plugins.ruby.utils.NamingConventions;

public class ResourceSymbol extends ClassModuleSymbol {
  public ResourceSymbol(RContainer classContainer, @Nullable Symbol parent) {
    super(classContainer.getProject(), classContainer.getFQNWithNesting(), classContainer, parent);
  }

  @Override
  public @Nullable String getName() {
    return myName != null ? NamingConventions.toUnderscoreCase(myName) : null;
  }
}
