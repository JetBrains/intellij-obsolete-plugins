package com.intellij.dmserver.facet;

import com.intellij.ProjectTopics;
import com.intellij.facet.Facet;
import com.intellij.facet.ProjectWideFacetAdapter;
import com.intellij.facet.ProjectWideFacetListenersRegistry;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author michael.golubev
 */
public abstract class DMNestedBundlesUpdater {

  public DMNestedBundlesUpdater(Project project, Disposable parentDisposable) {
    project.getMessageBus().connect(parentDisposable).subscribe(ProjectTopics.MODULES, new ModuleListener() {

      @Override
      public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
        Collection<NestedUnitIdentity> nestedBundles = getNestedBundles();
        boolean dirty = false;
        for (Iterator<NestedUnitIdentity> itModule = nestedBundles.iterator(); itModule.hasNext(); ) {
          Module nextModule = itModule.next().getModule();
          if (nextModule == null) {
            itModule.remove();
            dirty = true;
          }
        }
        if (dirty) {
          setNestedBundles(nestedBundles);
        }
      }
    });
    ProjectWideFacetListenersRegistry.getInstance(project).registerListener(new ProjectWideFacetAdapter<>() {

      @Override
      public void facetRemoved(@NotNull Facet facet) {
        facedAddedOrRemoved(facet);
      }

      @Override
      public void facetAdded(@NotNull Facet facet) {
        facedAddedOrRemoved(facet);
      }

      private void facedAddedOrRemoved(Facet facet) {
        if (facet instanceof DMFacetBase) {
          dmFacetAddedOrRemoved((DMFacetBase)facet);
        }
      }
    }, parentDisposable);
  }

  protected abstract Collection<NestedUnitIdentity> getNestedBundles();

  protected abstract void setNestedBundles(Collection<NestedUnitIdentity> nestedBundles);

  protected abstract void dmFacetAddedOrRemoved(DMFacetBase facet);
}
