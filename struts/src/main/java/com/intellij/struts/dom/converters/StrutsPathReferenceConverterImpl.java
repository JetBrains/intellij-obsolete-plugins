/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts.dom.converters;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.javaee.facet.JavaeeFacetUtil;
import com.intellij.javaee.web.ServletPathReferenceProvider;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.paths.PathReferenceManager;
import com.intellij.openapi.paths.PathReferenceProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.Controller;
import com.intellij.struts.dom.Forward;
import com.intellij.struts.dom.SetProperty;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.struts.inplace.reference.config.ForwardReferenceProvider;
import com.intellij.struts.inplace.reference.config.TilesReferenceProvider;
import com.intellij.util.ConstantFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.*;
import com.intellij.util.xml.highlighting.ResolvingElementQuickFix;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds tiles definitions as valid paths
 *
 * @author Dmitry Avdeev
 */
public class StrutsPathReferenceConverterImpl extends StrutsPathReferenceConverter {

  @NonNls private static final String INPUT_ATTRIBUTE = "input";
  @NonNls private static final String INPUT_FORWARD = "inputForward";

  private static final TilesReferenceProvider TILES_REFERENCE_PROVIDER = new TilesReferenceProvider(true);
  private static final ForwardReferenceProvider FORWARD_REFERENCE_PROVIDER = new ForwardReferenceProvider(true);

  private final PathReferenceProvider myTilesPathsProvider = new PathReferenceProvider() {

    @Override
    @Nullable
    public PathReference getPathReference(@NotNull String path, @NotNull final PsiElement element) {
      TilesModel model = StrutsManager.getInstance().getTiles(element);
      if (model != null) {
        final Definition definition = model.findDefinition(path);
        if (definition != null) {
          return new PathReference(path, new ConstantFunction<>(StrutsApiIcons.Tiles.Tile)) {

            @Override
            @Nullable
            public PsiElement resolve() {
              return definition.ensureTagExists();
            }
          };
        }
      }
      return null;
    }

    @Override
    public boolean createReferences(@NotNull PsiElement psiElement, @NotNull final List<PsiReference> references, final boolean soft) {
      final PsiReference[] refs = TILES_REFERENCE_PROVIDER.getReferencesByElement(psiElement);
      ContainerUtil.addAll(references, refs);
      return false;
    }
  };

  private final PathReferenceProvider myForwardsPathsProvider = new PathReferenceProvider() {

    @Override
    public boolean createReferences(@NotNull final PsiElement psiElement, final @NotNull List<PsiReference> references, final boolean soft) {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(psiElement);
      if (model != null) {
        PsiReference reference = new XmlValueReference((XmlAttributeValue)psiElement, FORWARD_REFERENCE_PROVIDER) {

          @Override
          @Nullable
          protected PsiElement doResolve() {
            String url = getValue();
            url = WebUtil.trimURL(url);
            Forward forward = null;
            final DomElement element = DomManager.getDomManager(psiElement.getProject()).getDomElement(((XmlAttribute)getElement().getParent()).getParent());
            if (element instanceof Action) {
              forward = DomUtil.findByName(((Action)element).getForwards(), url);
            }
            if (forward == null) {
              forward = model.findForward(url);
            }
            return forward == null ? null : forward.getXmlTag();
          }

          @Override
          @Nullable
          protected Object[] doGetVariants() {
            final List<Forward> forwards = new ArrayList<>(model.getGlobalForwards());
            final DomElement element = DomManager.getDomManager(psiElement.getProject()).getDomElement(((XmlAttribute)getElement().getParent()).getParent());
            assert element instanceof Action;
            forwards.addAll(((Action)element).getForwards());
            return ElementPresentationManager.getInstance().createVariants(forwards);
          }

          @Override
          @Nullable
          protected DomElement getScope() {
            return model.getMergedModel().getGlobalForwards();
          }

          @Override
          public LocalQuickFix[] getQuickFixes() {
            final ResolvingElementQuickFix quickFix = createResolvingFix(getScope());
            if (quickFix != null) {
              final DomElement element = DomManager.getDomManager(psiElement.getProject()).getDomElement(((XmlAttribute)getElement().getParent()).getParent());
              assert element instanceof Action;
              final ResolvingElementQuickFix local = createResolvingFix(element);
              assert local != null;
              local.setTypeName(StrutsBundle.message("local.forward"));
              return new LocalQuickFix[] {quickFix, local};
            }
            return LocalQuickFix.EMPTY_ARRAY;
          }
        };
        references.add(reference);
      }
      return false;
    }

    @Override
    @Nullable
    public PathReference getPathReference(@NotNull String path, @NotNull final PsiElement element) {
      return new PathReference(path, new ConstantFunction<>(StrutsApiIcons.Forward));
    }
  };

  private final PathReferenceProvider myStrutsPagesPathsProvider = new StrutsPagesReferenceProvider();

  private final PathReferenceProvider[] myProviders = new PathReferenceProvider[] {
    PathReferenceManager.getInstance().getGlobalWebPathReferenceProvider(),
    myStrutsPagesPathsProvider,
    myTilesPathsProvider,
    new ServletPathReferenceProvider()
  };

  @Override
  public PathReference fromString(@Nullable final String s, final ConvertContext context) {
    final Module module = context.getModule();
    if (module == null || s == null) return null;

    if (!context.getInvocationElement().isValid()) return null;

    final XmlElement xmlElement = context.getReferenceXmlElement();
    if (xmlElement == null) return null;

    if (isInputForward(xmlElement)) {
      final WebFacet webFacet = JavaeeFacetUtil.getInstance().getJavaeeFacet(context, WebFacet.ID);
      if (webFacet != null) {
        return myForwardsPathsProvider.getPathReference(s, xmlElement);
      }
    }
    return PathReferenceManager.getInstance().getCustomPathReference(s, module, xmlElement, myProviders);
  }

  @Override
  @NotNull
  public PsiReference[] createReferences(@NotNull final PsiElement psiElement, final boolean soft) {
    final PsiElement parent = psiElement.getParent();
    if (isInputForward(parent)) {
      return PathReferenceManager.getReferencesFromProvider(myForwardsPathsProvider, psiElement, soft);
    }
    return PathReferenceManager.getInstance().createCustomReferences(psiElement,
                                                                soft,
                                                                myProviders);
  }

  private static boolean isInputForward(PsiElement psiElement) {
    if (psiElement instanceof XmlAttribute && ((XmlAttribute)psiElement).getName().equals(INPUT_ATTRIBUTE)) {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(psiElement);
      if (model != null) {
        final Controller controller = model.getMergedModel().getController();
        final Boolean value = controller.getInputForward().getValue();
        if (value == null) {
          for (SetProperty property: controller.getSetProperties()) {
            final String s = property.getProperty().getStringValue();
            if (s != null && s.equals(INPUT_FORWARD)) {
              final String val = property.getValue().getStringValue();
              return val != null && Boolean.parseBoolean(val);
            }
          }
        } else {
          return value.booleanValue();
        }
      }
    }
    return false;
  }
}
