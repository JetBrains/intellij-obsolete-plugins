package com.intellij.lang.puppet.ide.navigation;

import com.intellij.ide.actions.DefaultSearchEverywhereClassifier;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import org.jetbrains.annotations.Nullable;

public class PuppetSearchEverywhereClassifier extends DefaultSearchEverywhereClassifier {
  @Override
  public boolean isClass(@Nullable Object o) {
    return o instanceof PuppetNavigationItem && ((PuppetNavigationItem)o).getDelegate() instanceof PuppetClassDefinition;
  }

  @Override
  public boolean isSymbol(@Nullable Object o) {
    if (!(o instanceof PuppetNavigationItem)) {
      return false;
    }

    return !(((PuppetNavigationItem)o).getDelegate() instanceof PuppetClassDefinition);
  }
}
