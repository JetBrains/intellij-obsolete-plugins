package com.intellij.seam.model.jam;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SeamJamModel {
  private static final Key<CachedValue<MergedSeamComponent>> MERGED_SEAM_COMPONENT = Key.create("MERGED_SEAM_COMPONENT");

  private final Module myModule;

  public static SeamJamModel getModel(@NotNull Module module) {
    return new SeamJamModel(module);
  }

  private SeamJamModel(@NotNull final Module module) {
    myModule = module;
  }

  public List<SeamJamComponent> getAnnotatedSeamComponents(boolean showFromLibraries) {
    return getJamClassElements(SeamJamComponent.META, SeamAnnotationConstants.COMPONENT_ANNOTATION, showFromLibraries);
  }

  public <T extends JamElement> List<T> getJamClassElements(final JamClassMeta<T> clazz, final String anno, boolean showFromLibraries) {
    final JamService service = JamService.getJamService(myModule.getProject());

    final GlobalSearchScope scope = getScope(showFromLibraries);

    return service.getJamClassElements(clazz, anno, scope);
  }

  private GlobalSearchScope getScope(boolean showFromLibraries) {
    return showFromLibraries
           ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule)
           : GlobalSearchScope.moduleWithDependenciesScope(myModule);
  }

  @NotNull
  public Set<SeamJamComponent> getSeamComponents() {
    return getSeamComponents(true);
  }

  @NotNull
  public Set<SeamJamComponent> getSeamComponents(final boolean mergeDomComponents) {
    return getSeamComponents(mergeDomComponents, true);
  }

  @NotNull
  public Set<SeamJamComponent> getSeamComponents(final boolean mergeDomComponents, boolean fromLibs) {

    final List<SeamJamComponent> annotated = getAnnotatedSeamComponents(fromLibs);
    Set<SeamJamComponent> components = new HashSet<>(annotated);
    if (mergeDomComponents) {
      components.addAll(getMergedComponents(annotated));
    }

    return components;
  }

  public List<SeamJamComponent> getMergedComponents(boolean fromLibs) {
    return getMergedComponents(getAnnotatedSeamComponents(fromLibs));
  }

  public List<SeamJamComponent> getMergedComponents(List<SeamJamComponent> annotated) {
    List<SeamJamComponent> mergedComponents = new ArrayList<>();
    List<PsiType> psiTypes = ContainerUtil.mapNotNull(annotated, seamJamComponent -> seamJamComponent.getComponentType());

    for (SeamComponents model : SeamDomModelManager.getInstance(myModule.getProject()).getAllModels(myModule)) {
      for (final SeamDomComponent domComponent : DomUtil.getDefinedChildrenOfType(model, SeamDomComponent.class)) {
        if (!psiTypes.contains(domComponent.getComponentType())) {
          final PsiType type = domComponent.getComponentType();
          if (type instanceof PsiClassType) {
            final PsiClass psiClass = ((PsiClassType)type).resolve();
            if (psiClass != null) {
              mergedComponents.add(getOrCreateMergedComponent(psiClass, domComponent));
            }
          }
        }
      }
    }
    return mergedComponents;
  }

  @NotNull
  private static MergedSeamComponent getOrCreateMergedComponent(PsiClass psiClass, SeamDomComponent domComponent) {
    CachedValue<MergedSeamComponent> cachedValue = psiClass.getUserData(MERGED_SEAM_COMPONENT);

    if (cachedValue == null) {
      cachedValue = CachedValuesManager.getManager(psiClass.getProject()).createCachedValue(() -> {
        MergedSeamComponent mergedSeamComponent = new MergedSeamComponent(psiClass, domComponent);

        return new CachedValueProvider.Result<>(mergedSeamComponent, PsiModificationTracker.MODIFICATION_COUNT);
      }, false);

      psiClass.putUserData(MERGED_SEAM_COMPONENT, cachedValue);
    }
    return cachedValue.getValue();
  }
}