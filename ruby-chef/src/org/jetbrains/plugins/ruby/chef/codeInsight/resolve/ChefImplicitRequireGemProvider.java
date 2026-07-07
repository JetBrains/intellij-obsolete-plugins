package org.jetbrains.plugins.ruby.chef.codeInsight.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.gem.GemDependency;
import org.jetbrains.plugins.ruby.gem.GemInfo;
import org.jetbrains.plugins.ruby.gem.detector.ImplicitRequireGemProvider;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RFile;

import java.util.HashMap;
import java.util.Map;

public final class ChefImplicitRequireGemProvider implements ImplicitRequireGemProvider {

  @Override
  public @NotNull Map<String, GemDependency> getImplicitDependency(final @NotNull RFile file) {
    final HashMap<String, GemDependency> result = new HashMap<>();
    final GemInfo chefGemInfo = ChefUtil.findChefGem(file);
    if (chefGemInfo == null) return result;

    result.put(ChefUtil.CHEF_GEM, chefGemInfo.asDependency());
    return result;
  }
}
