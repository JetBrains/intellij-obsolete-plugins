// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.pluginSupport.buildTestData.GrailsBuildTestDataMemberProvider;
import org.jetbrains.plugins.grails.pluginSupport.seachable.GrailsSearchableMemberProvider;
import org.jetbrains.plugins.grails.references.bootstrap.GrailsBootStrapMemberProvider;
import org.jetbrains.plugins.grails.references.controller.ControllerMembersProvider;
import org.jetbrains.plugins.grails.references.domain.DomainMembersProvider;
import org.jetbrains.plugins.grails.references.filter.FilterMemberProvider;
import org.jetbrains.plugins.grails.references.jobs.JobsMemberProvider;
import org.jetbrains.plugins.grails.references.taglib.TaglibMembersProvider;
import org.jetbrains.plugins.grails.references.urlMappings.UrlMappingMemberProvider;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.EnumMap;
import java.util.Map;

public final class GrailsArtifactNonCodeMemberProcessor extends NonCodeMembersContributor {

  private static volatile Map<GrailsArtifact, MemberProvider[]> MAP;

  public static Map<GrailsArtifact, MemberProvider[]> getMemberProviderMap() {
    Map<GrailsArtifact, MemberProvider[]> res = MAP;
    if (res == null) {
      res = new EnumMap<>(GrailsArtifact.class);

      res.put(GrailsArtifact.CONTROLLER, new MemberProvider[]{new ControllerMembersProvider()});
      res.put(GrailsArtifact.DOMAIN, new MemberProvider[]{new DomainMembersProvider(), new GrailsSearchableMemberProvider(), new GrailsBuildTestDataMemberProvider()});
      res.put(GrailsArtifact.TAGLIB, new MemberProvider[]{new TaglibMembersProvider()});
      res.put(GrailsArtifact.JOB, new MemberProvider[]{new JobsMemberProvider()});
      res.put(GrailsArtifact.FILTER, new MemberProvider[]{new FilterMemberProvider()});
      res.put(GrailsArtifact.URLMAPPINGS, new MemberProvider[]{new UrlMappingMemberProvider()});
      res.put(GrailsArtifact.BOOTSTRAP, new MemberProvider[]{new GrailsBootStrapMemberProvider()});

      MAP = res;
    }

    return res;
  }

  private static boolean isTagLibByPackage(@NotNull PsiClass aClass) {
    String qualifiedName = aClass.getQualifiedName();

    return qualifiedName != null && qualifiedName.endsWith("TagLib") && qualifiedName.startsWith("org.codehaus.groovy.grails.plugins.web.taglib.");
  }
  
  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass psiClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (psiClass == null) return;

    GrailsArtifact artifact = CachedValuesManager.getCachedValue(psiClass, () ->
      CachedValueProvider.Result.create(getArtifact(psiClass), PsiModificationTracker.MODIFICATION_COUNT));
    if (artifact == null) return;

    // See org.codehaus.groovy.grails.compiler.logging.LoggingTransformer
    if (!GrailsPsiUtil.processLogVariable(processor, psiClass, ResolveUtil.getNameHint(processor))) return;

    MemberProvider[] providers = getMemberProviderMap().get(artifact);

    if (providers != null) {
      for (MemberProvider provider : providers) {
        provider.processMembers(processor, psiClass, place);
      }
    }
  }

  private static @Nullable GrailsArtifact getArtifact(@NotNull PsiClass psiClass) {
    GrailsArtifact artifact = GrailsUtils.calculateArtifactType(psiClass);
    return artifact != null ? artifact : isTagLibByPackage(psiClass) ? GrailsArtifact.TAGLIB : null;
  }
}
