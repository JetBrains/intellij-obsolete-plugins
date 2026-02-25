// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.spring.SpringModificationTrackersManager;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.custom.CustomModuleComponentsDiscoverer;
import com.intellij.spring.model.jam.stereotype.CustomSpringComponent;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.plugins.ImplKt;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class GrailsSpringBeanDiscoverer extends CustomModuleComponentsDiscoverer {
  private static final MultiMap<String, Couple<String>> PLUGINS_BEAN_MAP = new MultiMap<>();

  private static final Couple<String>[] COMMON_BEANS = new Couple[]{
    Couple.of("messageSource", "org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource"),
    Couple.of("grailsUrlMappingsHolder", "org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean")
  };

  private static final Trinity<String, String, String>[] BEANS_BY_GRAILS_VERSION = new Trinity[]{
    Trinity.create("2.0.0", "grailsLinkGenerator", "org.codehaus.groovy.grails.web.mapping.LinkGenerator"),
    Trinity.create("2.0.0", "groovyPageRenderer", "grails.gsp.PageRenderer"),
  };

  static {
    PLUGINS_BEAN_MAP.put("hibernate", Arrays.asList(
      Couple.of("hibernateProperties", "org.springframework.beans.factory.config.PropertiesFactoryBean"),
      //Pair.create("nativeJdbcExtractor", "org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor"),
      //Pair.create("lobHandlerDetector", "org.codehaus.groovy.grails.orm.hibernate.support.SpringLobHandlerDetectorFactoryBean"),
      //Pair.create("proxyHandler", "org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler"),
      //Pair.create("eventTriggeringInterceptor", "org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor"),
      //Pair.create("hibernateEventListeners", "org.codehaus.groovy.grails.orm.hibernate.HibernateEventListeners"),
      //Pair.create("entityInterceptor", "org.hibernate.EmptyInterceptor"),
      Couple.of("sessionFactory", "org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean"),
      Couple.of("sessionFactory", "org.grails.orm.hibernate.HibernateMappingContextSessionFactoryBean"), // GORM 5
      Couple.of("transactionManager", "org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTransactionManager")
      //Pair.create("persistenceInterceptor", "org.codehaus.groovy.grails.orm.hibernate.support.HibernatePersistenceContextInterceptor"),
      //Pair.create("flushingRedirectEventListener", "org.codehaus.groovy.grails.orm.hibernate.support.FlushOnRedirectEventListener"),
      //Pair.create("openSessionInViewInterceptor", "org.codehaus.groovy.grails.orm.hibernate.support.GrailsOpenSessionInViewInterceptor")
    ));

    List<Couple<String>> shiroBeans = Collections.singletonList(
      Couple.of("jsecSecurityManager", "org.jsecurity.web.DefaultWebSecurityManager")
    );

    PLUGINS_BEAN_MAP.put("shiro", shiroBeans);
    PLUGINS_BEAN_MAP.put("jsecurity", shiroBeans);
  }

  @Override
  public @NotNull Collection<CommonSpringBean> getCustomComponents(final @Nullable Module module) {
    if (module == null) return Collections.emptyList();
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(
      module,
      () -> new CachedValueProvider.Result<>(evaluateCustomComponents(module), getDependencies(module))
    );
  }

  public static @NotNull Collection<CommonSpringBean> evaluateCustomComponents(Module module) {
    if (GrailsFramework.isCommonPluginsModule(module)) return Collections.emptyList();

    List<CommonSpringBean> result = new ArrayList<>();

    final GrailsStructure structure = GrailsStructure.getInstance(module);
    if (structure == null) return Collections.emptyList();

    // Add services
    for (GrClassDefinition classDefinition : GrailsArtifact.SERVICE.getInstances(module).values()) {
      result.add(new CustomSpringComponent(classDefinition));
    }

    Set<String> existBeans = new HashSet<>();

    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
    JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());

    for (Couple<String> pair : COMMON_BEANS) {
      addBeanIfClassExists(result, existBeans, facade, scope, pair.first, pair.second);
    }

    // Add beans from plugins.
    for (String pluginName : ContainerUtil.concat(
      structure.getInstalledCommonPlugins().keySet(),
      getPluginsNamesFromDescriptors(module))
    ) {
      for (Couple<String> pair : PLUGINS_BEAN_MAP.get(pluginName)) {
        addBeanIfClassExists(result, existBeans, facade, scope, pair.first, pair.second);
      }
    }

    for (Trinity<String, String, String> trinity : BEANS_BY_GRAILS_VERSION) {
      if (structure.isAtLeastGrails(trinity.first)) {
        addBeanIfClassExists(result, existBeans, facade, scope, trinity.second, trinity.third);
      }
    }

    // Add beans defined in grails-app/conf/spring/resources.groovy
    PsiManager psiManager = PsiManager.getInstance(module.getProject());

    addBeanFromResourceGroovy(result, existBeans, psiManager, structure.getAppRoot());

    Module commonPluginsModule = GrailsFramework.findCommonPluginsModule(module);
    if (commonPluginsModule != null) {
      for (VirtualFile root : ModuleRootManager.getInstance(commonPluginsModule).getContentRoots()) {
        addBeanFromResourceGroovy(result, existBeans, psiManager, root);

        for (VirtualFile file : root.getChildren()) {
          if (file.getName().endsWith("GrailsPlugin.groovy")) {
            GrTypeDefinition pluginClass = GroovyUtils.getPublicClass(file, psiManager);
            if (pluginClass != null) {
              List<GrailsResourceBeanExtractor.BeanDescriptor> descriptors = GrailsResourceBeanExtractor.getBeanDescriptorsFromPluginClass(pluginClass);
              convertBeans(result, existBeans, descriptors);
              break;
            }
          }
        }
      }
    } else {
      AllClassesSearch.search(scope, module.getProject(), name -> name.endsWith("GrailsPlugin")).asIterable().forEach((PsiElement clazz) -> {
        if (!(clazz instanceof GrTypeDefinition)) clazz = clazz.getNavigationElement();
        if (!(clazz instanceof GrTypeDefinition)) return;
        List<GrailsResourceBeanExtractor.BeanDescriptor> descriptors = GrailsResourceBeanExtractor.getBeanDescriptorsFromPluginClass(
          (GrTypeDefinition)clazz
        );
        convertBeans(result, existBeans, descriptors);
      });
    }

    return result;
  }

  private static @NotNull List<String> getPluginsNamesFromDescriptors(Module module) {
    final GrailsApplication application = GrailsApplicationManager.getInstance(module.getProject()).findApplication(module.getModuleFile());
    if (application == null) return Collections.emptyList();
    return ContainerUtil.map(ImplKt.computePlugins(application), p -> p.getPluginName());
  }

  private static void addBeanIfClassExists(List<CommonSpringBean> result,
                                           Set<String> existBeans,
                                           JavaPsiFacade facade,
                                           GlobalSearchScope scope,
                                           String beanName,
                                           String className) {
    if (existBeans.contains(beanName)) return;

    PsiClass beanClass = facade.findClass(className, scope);
    if (beanClass != null) {
      result.add(new GrailsCustomSpringComponent(beanClass, beanName));
      existBeans.add(beanName);
    }
  }

  private static void convertBeans(List<CommonSpringBean> result, Set<String> existBeans, Collection<GrailsResourceBeanExtractor.BeanDescriptor> descriptors) {
    for (final GrailsResourceBeanExtractor.BeanDescriptor bean : descriptors) {
      if (existBeans.contains(bean.getName())) continue;

      PsiClass resolveResult = RecursionManager.doPreventingRecursion(bean, true, () -> {
        PsiType type = bean.getType();
        if (type instanceof PsiClassType) {
          return ((PsiClassType)type).resolve();
        }

        return null;
      });

      if (resolveResult != null) {
        result.add(new GrailsCustomSpringComponent(resolveResult, bean.getName()));
        existBeans.add(bean.getName());
      }
    }
  }

  private static void addBeanFromResourceGroovy(List<CommonSpringBean> result, Set<String> existBeans, PsiManager manager, VirtualFile rootDirectory) {
    VirtualFile resourcesFile = VfsUtil.findRelativeFile(rootDirectory, "grails-app", "conf", "spring", "resources.groovy");
    if (resourcesFile == null) return;

    PsiFile resourcePsiFile = manager.findFile(resourcesFile);
    if (!(resourcePsiFile instanceof GroovyFile)) return;

    List<GrailsResourceBeanExtractor.BeanDescriptor> descriptors =
      GrailsResourceBeanExtractor.getBeanDescriptorsFromResourcesGroovy((GroovyFile)resourcePsiFile);

    convertBeans(result, existBeans, descriptors);
  }

  @Override
  public Object[] getDependencies(@NotNull Module module) {
    return ArrayUtil.append(SpringModificationTrackersManager.getInstance(module.getProject()).getOuterModelsDependencies(),
                           PsiModificationTracker.MODIFICATION_COUNT);
  }

  @Override
  public @NotNull String getProviderName() {
    return "Grails Spring Beans";
  }
}
