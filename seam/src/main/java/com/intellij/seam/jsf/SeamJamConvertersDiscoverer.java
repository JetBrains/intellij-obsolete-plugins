package com.intellij.seam.jsf;

import com.intellij.jam.JamService;
import com.intellij.jsf.model.common.JsfCommonConverter;
import com.intellij.jsf.model.common.JsfConvertersDiscoverer;
import com.intellij.openapi.module.Module;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.jam.jsf.SeamJamConverter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SeamJamConvertersDiscoverer implements JsfConvertersDiscoverer {

  @Override
  public void addJsfConverters(@NotNull Module module, @NotNull Set<JsfCommonConverter> converters) {
    final List<SeamJamConverter> jamConverters = JamService.getJamService(module.getProject())
      .getJamClassElements(SeamJamConverter.META, SeamAnnotationConstants.JSF_CONVERTER_ANNOTATION,
                           GlobalSearchScope.moduleWithLibrariesScope(module));

    converters.addAll(jamConverters);
  }
}
