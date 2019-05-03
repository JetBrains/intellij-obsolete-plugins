/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.tiles.springMvc;

import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringModelSearchParameters;
import com.intellij.spring.model.jam.javaConfig.ContextJavaBean;
import com.intellij.spring.model.utils.SpringCommonUtils;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.xml.beans.*;
import com.intellij.spring.model.xml.mvc.TilesConfigurer;
import com.intellij.spring.web.SpringWebConstants;
import com.intellij.spring.web.mvc.model.SpringMVCModel;
import com.intellij.spring.web.mvc.model.SpringMVCModelService;
import com.intellij.struts.StrutsPluginDomFactory;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.TilesModel;
import com.intellij.struts.TilesModelProvider;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.struts.psi.TilesModelImpl;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class MVCTilesModelProvider implements TilesModelProvider {

  @Override
  @NotNull
  public Collection<TilesModel> computeModels(@NotNull final Module module) {
    SpringFacet springFacet = SpringFacet.getInstance(module);
    if (springFacet == null) {
      return Collections.emptyList();
    }

    Collection<WebFacet> webFacets = WebFacet.getInstances(module);
    if (webFacets.isEmpty()) {
      return Collections.emptyList();
    }

    final List<TilesModel> models = new SmartList<>();
    Processor<Set<XmlFile>> processor = definitions -> {
      final StrutsPluginDomFactory<TilesDefinitions, TilesModel> factory =
        StrutsProjectComponent.getInstance(module.getProject()).getTilesFactory();
      DomFileElement<TilesDefinitions> domFileElement = factory.createMergedModelRoot(definitions);
      if (domFileElement != null) {
        TilesModelImpl tilesModel = new TilesModelImpl(definitions, domFileElement, "mvcTilesModel");
        models.add(tilesModel);
      }
      return true;
    };

    for (WebFacet webFacet : webFacets) {
      SpringMVCModel springMVCModel = SpringMVCModelService.getInstance().getModel(webFacet, springFacet);
      if (springMVCModel != null) {
        Collection<SpringModel> servletModels = springMVCModel.getServletModels();
        for (SpringModel servletModel : servletModels) {
          processTiles(servletModel, processor);
        }
      }
    }
    return models;
  }

  private static void processTiles(SpringModel model, Processor<Set<XmlFile>> consumer) {
    Module module = model.getModule();
    assert module != null : model;

    for (String configurerClass : SpringWebConstants.TILES_CONFIGURER_CLASSES) {
      PsiClass tilesConfigurer = SpringCommonUtils.findLibraryClass(module, configurerClass);
      if (tilesConfigurer != null) {
        if (!processTilesConfigurer(model, consumer, tilesConfigurer)) {
          return; // stop after finding configurers with first (highest version) matched configurer class
        }
      }
    }
  }

  private static boolean processTilesConfigurer(SpringModel model,
                                                Processor<Set<XmlFile>> consumer,
                                                PsiClass configurerClass) {
    final SpringModelSearchParameters.BeanClass searchParameters =
      SpringModelSearchParameters.byClass(configurerClass).withInheritors().effectiveBeanTypes();
    List<SpringBeanPointer> pointers = SpringModelSearchers.findBeans(model, searchParameters);
    for (SpringBeanPointer pointer : pointers) {
      Set<VirtualFile> tilesXmlFiles = getTilesXmlFiles(pointer);

      final PsiManager psiManager = configurerClass.getManager();
      final StrutsPluginDomFactory<TilesDefinitions, TilesModel> tilesFactory =
        StrutsProjectComponent.getInstance(psiManager.getProject()).getTilesFactory();
      Set<XmlFile> foundFiles = new LinkedHashSet<>(tilesXmlFiles.size());
      for (VirtualFile file : tilesXmlFiles) {
        PsiFile psiFile = psiManager.findFile(file);
        if (psiFile instanceof XmlFile &&
            tilesFactory.getDomRoot((XmlFile)psiFile) != null) {
          foundFiles.add((XmlFile)psiFile);
        }
      }
      if (!consumer.process(foundFiles)) {
        return false;
      }
    }
    return pointers.isEmpty();
  }

  @NotNull
  private static Set<VirtualFile> getTilesXmlFiles(SpringBeanPointer pointer) {
    if (!pointer.isValid()) return Collections.emptySet();
    CommonSpringBean springBean = pointer.getSpringBean();

    List<PsiReference> referenceElements = new SmartList<>();
    if (springBean instanceof SpringBean) {
      SpringPropertyDefinition property =
        SpringPropertyUtils.findPropertyByName(springBean, TilesConfigurerDefinitionsConverter.DEFINITIONS_ATTRIBUTE);
      if (!(property instanceof SpringProperty)) {
        return Collections.emptySet();
      }

      SpringProperty springProperty = (SpringProperty)property;

      String valueAttribute = property.getValueAsString();
      if (valueAttribute != null) {
        addReferences(referenceElements, springProperty.getValueAttr().getXmlAttributeValue());
      }
      else {
        collectCollectionProperties(referenceElements, springProperty);
      }
    }
    else if (springBean instanceof TilesConfigurer) {
      TilesConfigurer tilesConfigurer = (TilesConfigurer)springBean;
      for (com.intellij.spring.model.xml.mvc.TilesDefinitions definitions : tilesConfigurer.getTilesDefinitions()) {
        if (definitions.getLocation().getStringValue() != null) {
          addReferences(referenceElements, definitions.getLocation().getXmlAttributeValue());
        }
      }
    }
    else if (springBean instanceof ContextJavaBean) {
      collectJavaBeanReferences(referenceElements, springBean);
    }

    Set<VirtualFile> files = ContainerUtil.newLinkedHashSet();
    for (PsiReference reference : referenceElements) {
      if (!(reference instanceof PsiPolyVariantReference)) {
        continue;
      }
      for (ResolveResult result : ((PsiPolyVariantReference)reference).multiResolve(false)) {
        ContainerUtil.addIfNotNull(files, PsiUtilBase.asVirtualFile(result.getElement()));
      }
    }

    return files;
  }

  private static void addReferences(List<PsiReference> references, @Nullable PsiElement psiElement) {
    if (psiElement == null) return;

    final PsiReference[] psiReferences = psiElement.getReferences();
    ContainerUtil.addAllNotNull(references, psiReferences);
  }

  private static void collectCollectionProperties(List<PsiReference> references, SpringProperty springProperty) {
    if (DomUtil.hasXml(springProperty.getList())) {
      collect(references, springProperty.getList());
    }
    if (DomUtil.hasXml(springProperty.getSet())) {
      collect(references, springProperty.getSet());
    }
    if (DomUtil.hasXml(springProperty.getArray())) {
      collect(references, springProperty.getArray());
    }
  }

  private static void collect(List<PsiReference> references, ListOrSet list) {
    for (SpringValue springValue : list.getValues()) {
      addReferences(references, springValue.getXmlElement());
    }
  }

  private static void collectJavaBeanReferences(List<PsiReference> references,
                                                CommonSpringBean springBean) {
    final PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass == null) return;

    PsiMethod setter =
      PropertyUtilBase.findPropertySetter(beanClass, TilesConfigurerDefinitionsConverter.DEFINITIONS_ATTRIBUTE, false, true);
    if (setter == null) {
      setter = PropertyUtilBase.findPropertySetter(PsiTypesUtil.getPsiClass(springBean.getBeanType(true)),
                                                   TilesConfigurerDefinitionsConverter.DEFINITIONS_ATTRIBUTE, false, true);
    }
    if (setter == null) return;

    final LocalSearchScope scope = new LocalSearchScope(((ContextJavaBean)springBean).getPsiElement());
    PsiCall setDefinitionsCall = ContainerUtil.getFirstItem(StringExpressionHelper.searchMethodCalls(setter, scope));
    if (setDefinitionsCall == null) return;

    PsiExpressionList argumentList = setDefinitionsCall.getArgumentList();
    if (argumentList != null) {
      final PsiExpression[] paramExpressions = argumentList.getExpressions();
      for (PsiExpression expression : paramExpressions) { // vararg

        // new String[] {..}
        if (expression instanceof PsiNewExpression) {
          final PsiArrayInitializerExpression arrayInitializer = ((PsiNewExpression)expression).getArrayInitializer();
          if (arrayInitializer == null) continue;
          for (PsiExpression initializer : arrayInitializer.getInitializers()) {
            addReferences(references, initializer);
          }
          continue;
        }

        // CONSTANT_REF, build references on its initializer(s)
        if (expression instanceof PsiReferenceExpression) {
          PsiElement resolve = ((PsiReferenceExpression)expression).resolve();
          if (resolve instanceof PsiVariable) {
            PsiExpression initializer = ((PsiVariable)resolve).getInitializer();

            if (initializer instanceof PsiLiteralExpression) {
              ContainerUtil.addAllNotNull(references, createResourceReferences(initializer));
              continue;
            }

            if (initializer instanceof PsiNewExpression) {
              initializer = ((PsiNewExpression)initializer).getArrayInitializer();
            }

            if (initializer instanceof PsiArrayInitializerExpression) {
              for (PsiExpression psiExpression : ((PsiArrayInitializerExpression)initializer).getInitializers()) {
                ContainerUtil.addAllNotNull(references, createResourceReferences(psiExpression));
              }
            }
          }
        }

        addReferences(references, expression);
      }
    }
  }

  @NotNull
  private static PsiReference[] createResourceReferences(PsiExpression initializer) {
    return TilesConfigurerReferenceContributor.PROVIDER.getReferencesByElement(initializer, new ProcessingContext());
  }
}
