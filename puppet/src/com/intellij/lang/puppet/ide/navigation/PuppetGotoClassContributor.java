package com.intellij.lang.puppet.ide.navigation;

import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetGotoClassContributor implements ChooseByNameContributor, ChooseByNameContributorEx {
  @Override
  public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
    StubIndex.getInstance().processAllKeys(PuppetClassStubsIndex.KEY, processor, scope, filter);
  }

  @Override
  public void processElementsWithName(@NotNull String name,
                                      @NotNull Processor<? super NavigationItem> processor,
                                      @NotNull FindSymbolParameters parameters) {
    Project project = parameters.getProject();
    GlobalSearchScope scope = parameters.getSearchScope();
    ContainerUtil.process(PuppetClassStubsIndex.getInstance().find(name, project, scope),
                          Processors.map(processor, o -> new PuppetNavigationItem(name, o)));
  }
}
