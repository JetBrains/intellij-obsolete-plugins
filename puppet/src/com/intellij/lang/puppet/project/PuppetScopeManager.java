package com.intellij.lang.puppet.project;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ConcurrentFactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.intellij.lang.puppet.project.PuppetProjectManager.PUPPET_PROJECT_TOPIC;

public class PuppetScopeManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(PuppetScopeManager.class);
  private final Map<PuppetEntity, GlobalSearchScope> myCache = ConcurrentFactoryMap.createMap(PuppetEntity::calcResolveScope);

  public PuppetScopeManager(@NotNull Project project) {
    project.getMessageBus().connect(this)
      .subscribe(PUPPET_PROJECT_TOPIC, new PuppetProjectListener() {
        @Override
        public void projectUpdated() {
          clearCache();
        }
      });
  }

  @Override
  public void dispose() {
  }

  public @Nullable GlobalSearchScope getResolveScope(@NotNull PuppetEntity<?> entity) {
    return myCache.get(entity);
  }

  public void clearCache() {
    LOG.debug("Dropping scopes caches");
    myCache.clear();
  }

  public static PuppetScopeManager getInstance(@NotNull Project project) {
    return project.getService(PuppetScopeManager.class);
  }
}
