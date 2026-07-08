package com.intellij.lang.puppet.ide.navigation;

import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetFunctionsStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetNodeStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetResourceInstanceStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetVariableStubsIndex;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.psi.PuppetDefaultWrapper.DEFAULT_NAME;
import static com.intellij.lang.puppet.psi.PuppetDefaultWrapper.DEFAULT_PRESENTABLE_NAME;

public class PuppetGotoSymbolContributor implements ChooseByNameContributor, ChooseByNameContributorEx {
  @Override
  public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
    StubIndex index = StubIndex.getInstance();
    if (!index.processAllKeys(PuppetNodeStubsIndex.KEY, processor, scope, filter)) return;
    if (!index.processAllKeys(PuppetResourceInstanceStubsIndex.KEY, processor, scope, filter)) return;
    if (!index.processAllKeys(PuppetClassStubsIndex.KEY, processor, scope, filter)) return;
    if (!index.processAllKeys(PuppetTypeStubIndex.KEY, processor, scope, filter)) return;
    if (!index.processAllKeys(PuppetFunctionsStubsIndex.KEY, processor, scope, filter)) return;
    if (!index.processAllKeys(PuppetVariableStubsIndex.KEY, processor, scope, filter)) return;
  }

  @Override
  public void processElementsWithName(@NotNull String name,
                                      @NotNull Processor<? super NavigationItem> processor,
                                      @NotNull FindSymbolParameters parameters) {
    Project project = parameters.getProject();
    GlobalSearchScope scope = parameters.getSearchScope();
    String adjustedName = StringUtil.equals(DEFAULT_PRESENTABLE_NAME, name) ? DEFAULT_NAME : name;
    Processor<NavigatablePsiElement> p = Processors.map(
      processor, o -> new PuppetNavigationItem(adjustedName, o));
    if (!ContainerUtil.process(PuppetNodeStubsIndex.getInstance().find(name, project, scope), p)) return;
    if (!ContainerUtil.process(PuppetResourceInstanceStubsIndex.getInstance().find(name, project, scope), p)) return;
    if (!ContainerUtil.process(PuppetClassStubsIndex.getInstance().find(name, project, scope), p)) return;
    if (!ContainerUtil.process(PuppetTypeStubIndex.getInstance().find(name, project, scope), p)) return;
    if (!ContainerUtil.process(PuppetFunctionsStubsIndex.getInstance().find(name, project, scope), p)) return;
    if (!ContainerUtil.process(PuppetVariableStubsIndex.getInstance().find(name, project, scope), p)) return;
  }
}

