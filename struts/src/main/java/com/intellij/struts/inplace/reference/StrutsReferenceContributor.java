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

package com.intellij.struts.inplace.reference;

import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.lang.properties.ResourceBundleReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.filters.AndFilter;
import com.intellij.psi.filters.PsiMethodCallFilter;
import com.intellij.psi.filters.ScopeFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PathListReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.WebPathReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.dom.PlugIn;
import com.intellij.struts.dom.converters.StrutsPagesReferenceProvider;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.struts.inplace.Filters;
import com.intellij.struts.inplace.reference.code.FindForwardReferenceProvider;
import com.intellij.struts.inplace.reference.config.*;
import com.intellij.struts.inplace.reference.property.FormPropertyReferenceProvider;
import com.intellij.struts.inplace.reference.property.IndexedFormPropertyReferenceProvider;
import com.intellij.struts.inplace.reference.property.SetPropertyReferenceProvider;
import com.intellij.struts.inplace.reference.property.ValidatorFormPropertyReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers all ReferenceProviders.
 *
 * @author Dmitry Avdeev
 */
public class StrutsReferenceContributor extends PsiReferenceContributor {

  private final PsiReferenceProvider actionProvider = new ActionReferenceProvider();
  private final PsiReferenceProvider moduleProvider = new ModuleReferenceProvider();
  private final PsiReferenceProvider forwardProvider = new ForwardReferenceProvider(false);

  private final PsiReferenceProvider pathProvider;
  private final PsiReferenceProvider softPathProvider;
  private final PsiReferenceProvider relativePathProvider;

  private final PsiReferenceProvider rolesProvider = new RolesReferenceProvider(false);
  private final PsiReferenceProvider singleRoleProvider = new RolesReferenceProvider(true);
  private final PsiReferenceProvider tileProvider = new TilesReferenceProvider(false);
  private final PsiReferenceProvider myJspFormPropertyProvider = new FormPropertyReferenceProvider();

  private final PsiReferenceProvider propProvider;
  private final PsiReferenceProvider originalPropProvider;

  @NonNls
  public static final String VALIDATOR_RULES_XML = "/org/apache/struts/validator/validator-rules.xml";

  @NonNls
  private static final String DATA_SOURCE = "data-source";

  @Override
  public void registerReferenceProviders(@NotNull final PsiReferenceRegistrar psiReferenceRegistrar) {
    registerStrutsConfigReferences(psiReferenceRegistrar);

    registerJspReferences(psiReferenceRegistrar);

    registerJspTilesTags(psiReferenceRegistrar);

    registerTilesReferences(psiReferenceRegistrar);

    registerValidationReferences(psiReferenceRegistrar);

    registerWebAppReferences(psiReferenceRegistrar);

    registerCodeReferences(psiReferenceRegistrar);
  }

  public StrutsReferenceContributor() {

    final StrutsPagesReferenceProvider pagesReferenceProvider = new StrutsPagesReferenceProvider();
    pathProvider = new PathReferenceAdapter(pagesReferenceProvider, false);
    softPathProvider = new PathReferenceAdapter(pagesReferenceProvider, true);
    relativePathProvider = new WebPathReferenceProvider(false, true, true);

    originalPropProvider = CommonReferenceProviderTypes.PROPERTIES_FILE_KEY_PROVIDER.getProvider();

    propProvider = new WrappedReferenceProvider(originalPropProvider) {

      @Override
      protected boolean accept(@NotNull final PsiElement psiElement) {
        return isPropertiesValidationEnabled(psiElement);
      }
    };

  }

  private static boolean isPropertiesValidationEnabled(final PsiElement psiElement) {
    final WebFacet webFacet = WebUtil.getWebFacet(psiElement);
    if (webFacet == null) {
      return false;
    }
    final StrutsFacet strutsFacet = StrutsFacet.getInstance(webFacet);
    return strutsFacet == null || !strutsFacet.getConfiguration().getValidationConfiguration().mySuppressPropertiesValidation;
  }

  private static void registerCodeReferences(final PsiReferenceRegistrar registrar) {
    // actionMapping.findForward(XXX)
    // TODO: trigger only in Action-subclasses
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(PsiLiteralExpression.class).and
        (new FilterPattern(
          new ParentElementFilter(new PsiMethodCallFilter("org.apache.struts.action.ActionMapping", "findForward"), 2))
        ),
      new FindForwardReferenceProvider());
  }

  private void registerStrutsConfigReferences(final PsiReferenceRegistrar registrar) {

    ReferenceProviderUtils.registerTags(registrar, rolesProvider, "roles", Filters.NAMESPACE_STRUTS_CONFIG, "action");

    ReferenceProviderUtils.registerTags(registrar, new SetPropertyReferenceProvider("className", "type") {

      @Override
      protected String getClassNameAttribute(final XmlTag tag) {
        if (tag.getLocalName().equals(DATA_SOURCE)) {
          return "type";
        } else {
          return super.getClassNameAttribute(tag);
        }
      }
    }, "property", Filters.NAMESPACE_STRUTS_CONFIG, "set-property");

    ReferenceProviderUtils.registerTags(registrar, new PathListReferenceProvider() {

      @NotNull
      @Override
      protected PsiReference[] createReferences(@NotNull final PsiElement element, final String s, final int offset, boolean soft) {
        final PsiElement tag = element.getParent().getParent().getParent();
        soft = tag instanceof XmlTag && !((XmlTag) tag).getName().equals(PlugIn.PLUGIN);
        if (s.equals(VALIDATOR_RULES_XML)) {
          final PsiReferenceBase<PsiElement> reference =
            new PsiReferenceBase<PsiElement>(element, new TextRange(offset, offset + s.length())) {
              @Override
              public PsiElement resolve() {
                return resolveValidatorConfigInJAR(element.getProject());
              }
            };
          return new PsiReference[]{reference};
        }
        return super.createReferences(element, s, offset, soft);
      }
    }, "value", Filters.NAMESPACE_STRUTS_CONFIG, "set-property");

    ReferenceProviderUtils.registerTags(registrar, new PsiReferenceProvider() {

      @Override
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        final XmlTag tag = (XmlTag) element.getParent().getParent();
        final String factory = tag.getAttributeValue("factory");
        final boolean soft = factory != null;
        final ResourceBundleReference reference = new ResourceBundleReference(element, soft);
        return new PsiReference[]{reference};
      }
    }, "parameter", Filters.NAMESPACE_STRUTS_CONFIG, "message-resources");

    ReferenceProviderUtils.registerTags(registrar, propProvider, "key", Filters.NAMESPACE_STRUTS_CONFIG, "exception");

    ReferenceProviderUtils.registerTags(registrar, moduleProvider, "module", Filters.NAMESPACE_STRUTS_CONFIG, "forward");
  }

  private static void registerWebAppReferences(final PsiReferenceRegistrar registrar) {
    XmlUtil.registerXmlTagReferenceProvider(registrar, new String[]{"param-value"},
      new NamespaceFilter(XmlUtil.WEB_XML_URIS), true, new WebAppPathListProvider());
  }

  private void registerJspReferences(final PsiReferenceRegistrar registrar) {

    // Struts html taglib  -------------------------------------------------

    // action + paths stuff
    ReferenceProviderUtils.registerTags(registrar, actionProvider,
      "action", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "form", "frame", "img", "link", "rewrite");

    ReferenceProviderUtils.registerTags(registrar, moduleProvider,
      "module", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "frame", "img", "image", "link", "rewrite");

    ReferenceProviderUtils.registerTags(registrar, forwardProvider,
      "forward", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "frame", "link", "rewrite");

    ReferenceProviderUtils.registerTags(registrar, pathProvider,
      "page", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "frame", "link", "rewrite", "img", "image");

    ReferenceProviderUtils.registerTags(registrar, softPathProvider,
      "href", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "frame", "link", "rewrite");

    // img, image
    ReferenceProviderUtils.registerTags(registrar, softPathProvider,
      "src", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "img", "image");

    // form stuff
    ReferenceProviderUtils.registerTags(registrar, myJspFormPropertyProvider,
      "property", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "button", "cancel", "checkbox", "file", "hidden", "img", "link", "multibox",
      "option", "options", "optionsCollection", "password", "radio", "select", "text", "textarea");

    ReferenceProviderUtils.registerTags(registrar, myJspFormPropertyProvider,
      "labelProperty", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "options");

    ReferenceProviderUtils.registerTags(registrar, myJspFormPropertyProvider,
      "focus", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "form");

    ReferenceProviderUtils.registerTags(registrar, new FormReferenceProvider(),
      "formName", Filters.NAMESPACE_TAGLIB_STRUTS_HTML,
      "javascript");


    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{"altKey", "titleKey", "pageKey", "srcKey"},
      new ScopeFilter(new ParentElementFilter(new AndFilter(Filters.NAMESPACE_TAGLIB_STRUTS_HTML, ReferenceProviderUtils.TAG_CLASS_FILTER), 2)), propProvider);

    // Struts logic/bean taglib -------------------------------------------------

    // <logic:redirect>
    ReferenceProviderUtils.registerTags(registrar, actionProvider,
      "action", Filters.NAMESPACE_TAGLIB_STRUTS_LOGIC,
      "redirect");

    ReferenceProviderUtils.registerTags(registrar, softPathProvider,
      "href", Filters.NAMESPACE_TAGLIB_STRUTS_LOGIC,
      "redirect");

    ReferenceProviderUtils.registerTags(registrar, pathProvider,
      "page", Filters.NAMESPACE_TAGLIB_STRUTS_LOGIC,
      "redirect");

    ReferenceProviderUtils.registerTags(registrar, actionProvider,
      "mapping", Filters.NAMESPACE_TAGLIB_STRUTS_BEAN,
      "struts");

    // forwards
    ReferenceProviderUtils.registerTags(registrar, forwardProvider,
      "forward", Filters.NAMESPACE_TAGLIB_STRUTS_LOGIC,
      "redirect");

    ReferenceProviderUtils.registerTags(registrar, forwardProvider,
      "forward", Filters.NAMESPACE_TAGLIB_STRUTS_BEAN,
      "include", "struts");

    ReferenceProviderUtils.registerTags(registrar, forwardProvider,
      "name", Filters.NAMESPACE_TAGLIB_STRUTS_LOGIC,
      "forward");

    // <bean:define>
    ReferenceProviderUtils.registerTags(registrar, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement,
                                                   @NotNull final ProcessingContext processingContext) {
        final JavaClassReferenceProvider classReferenceProvider = new JavaClassReferenceProvider();
        classReferenceProvider.setOption(JavaClassReferenceProvider.ADVANCED_RESOLVE, Boolean.TRUE);
        return classReferenceProvider.getReferencesByElement(psiElement, processingContext);
      }
    },
      "type", Filters.NAMESPACE_TAGLIB_STRUTS_BEAN, "define");
  }

  private void registerJspTilesTags(final PsiReferenceRegistrar registrar) {

    ReferenceProviderUtils.registerSubclass(registrar, Filters.NAMESPACE_TAGLIB_STRUTS_TILES, "insert", "controllerClass",
      "org.apache.struts.tiles.Controller",
      "org.apache.struts.tiles.ControllerSupport",
      "org.apache.struts.action.Action");

    // <tiles:insert>
    ReferenceProviderUtils.registerTags(registrar, tileProvider,
      "definition", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "insert");

    ReferenceProviderUtils.registerAttributes(registrar, relativePathProvider,
      "insert", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "page", "component", "template");

    ReferenceProviderUtils.registerTags(registrar, pathProvider,
      "controllerUrl", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "insert");

    ReferenceProviderUtils.registerTags(registrar, singleRoleProvider,
      "role", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "insert");

    // <tiles:definition>
    ReferenceProviderUtils.registerTags(registrar, tileProvider,
      "extends", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "definition");

    ReferenceProviderUtils.registerAttributes(registrar, pathProvider,
      "definition", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "page", "template");

    ReferenceProviderUtils.registerTags(registrar, singleRoleProvider,
      "role", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "definition");

    // <tiles:put>
    ReferenceProviderUtils.registerTags(registrar, new TilesValueReferenceProvider(),
      "value", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "put");

    ReferenceProviderUtils.registerTags(registrar, new TilesJSPPutsReferenceProvider(),
      "name", Filters.NAMESPACE_TAGLIB_STRUTS_TILES,
      "put");

    ReferenceProviderUtils.registerSubclass(registrar, Filters.NAMESPACE_TAGLIB_STRUTS_TILES, "useAttribute", "classname");

    ReferenceProviderUtils.registerSubclass(registrar, Filters.NAMESPACE_TAGLIB_STRUTS_TILES, "initComponentDefinitions", "classname",
      "org.apache.struts.tiles.DefinitionsFactory");
  }

  private void registerTilesReferences(final PsiReferenceRegistrar registrar) {
    ReferenceProviderUtils.registerAttributes(registrar, softPathProvider, "item", Filters.NAMESPACE_TILES_CONFIG, "link", "icon");
/*   TODO what's up with this?
    registerSubclass(Filters.NAMESPACE_TILES_CONFIG, "definition", new String[]{"controllerClass"}, new String[]{
      "org.apache.struts.tiles.Controller", "org.apache.struts.tiles.ControllerSupport", "org.apache.struts.action.Action"});
*/
    ReferenceProviderUtils.registerTags(registrar, new SetPropertyReferenceProvider("classtype", null),
      "property", Filters.NAMESPACE_TILES_CONFIG,
      "set-property");

    // <put>
    ReferenceProviderUtils.registerTags(registrar, new TilesValueReferenceProvider(),
      "value", Filters.NAMESPACE_TILES_CONFIG,
      "put");

    ReferenceProviderUtils.registerTags(registrar, new TilesValueReferenceProvider(),
      "value", Filters.NAMESPACE_TILES_CONFIG,
      "put-attribute");

    ReferenceProviderUtils.registerTags(registrar, new TilesPutsReferenceProvider(),
      "name", Filters.NAMESPACE_TILES_CONFIG,
      "put");
  }

  private void registerValidationReferences(final PsiReferenceRegistrar registrar) {

    // form, fields
    ReferenceProviderUtils.registerTags(registrar, new ValidatorFormReferenceProvider(),
      "name", Filters.NAMESPACE_VALIDATOR_CONFIG,
      "form");

    ReferenceProviderUtils.registerTags(registrar, new ValidatorFormPropertyReferenceProvider(),
      "property", Filters.NAMESPACE_VALIDATOR_CONFIG,
      "field");

    ReferenceProviderUtils.registerTags(registrar, new IndexedFormPropertyReferenceProvider(),
      "indexedListProperty", Filters.NAMESPACE_VALIDATOR_CONFIG,
      "field");

    // TODO move to DOM w/ customized DependsConverter
    final ValidatorReferenceProvider myWrappedValidatorProvider = new ValidatorReferenceProvider() {

      @Override
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement, @NotNull final ProcessingContext context) {
        final String value = ((XmlAttributeValue) psiElement).getValue();
        if (StringUtil.isEmpty(value)) {
          return PsiReference.EMPTY_ARRAY;
        }
        return super.getReferencesByElement(psiElement, context);
      }
    };

    ReferenceProviderUtils.registerTags(registrar, myWrappedValidatorProvider,
      "depends", Filters.NAMESPACE_VALIDATOR_CONFIG,
      "validator");

    // messages
    final WrappedReferenceProvider msgProvider = new WrappedReferenceProvider(originalPropProvider) {
      @Override
      protected boolean accept(@NotNull final PsiElement psiElement) {
        if (!isPropertiesValidationEnabled(psiElement)) {
          return false;
        }
        final PsiElement element = psiElement.getContext();
        assert element != null;
        final PsiElement context = element.getContext();
        if (context instanceof XmlTag) {
          @NonNls final String res = ((XmlTag) context).getAttributeValue("classname");
          if (res == null || res.trim().isEmpty()) {
            return false;
          }
        }
        return true;
      }
    };
    ReferenceProviderUtils.registerTags(registrar, msgProvider,
      "msg", Filters.NAMESPACE_VALIDATOR_CONFIG,
      "validator");


    final WrappedReferenceProvider myWrappedPropProvider = new WrappedReferenceProvider(originalPropProvider) {
      @Override
      protected boolean accept(@NotNull final PsiElement psiElement) {
        if (!isPropertiesValidationEnabled(psiElement)) {
          return false;
        }
        final PsiElement element = psiElement.getContext();
        assert element != null;
        final PsiElement context = element.getContext();
        if (context instanceof XmlTag) {
          @NonNls final String res = ((XmlTag) context).getAttributeValue("resource");
          if (res != null && res.equals("false")) {
            return false;
          }
        }
        return true;
      }
    };

    ReferenceProviderUtils.registerTags(registrar, myWrappedPropProvider,
      "key", Filters.NAMESPACE_VALIDATOR_CONFIG,
      "msg", "arg", "arg0", "arg1", "arg2", "arg3");
  }

  /**
   * Struts 1.3: Resolves the default {@code validator-rules.xml} configuration file located in struts.jar.
   *
   * @param project Project.
   * @return Reference to file or null if not resolved.
   */
  @Nullable
  public static PsiFile resolveValidatorConfigInJAR(final Project project) {

    final PsiClass psiClass =
      JavaPsiFacade.getInstance(project).findClass("org.apache.struts.validator.ValidatorForm", GlobalSearchScope.allScope(project));
    if (psiClass == null) {
      return null;
    }
    final VirtualFile file = psiClass.getContainingFile().getVirtualFile();
    if (file == null) {
      return null;
    }
    String formPath = file.getUrl();
    formPath = StringUtil.replace(formPath, "ValidatorForm.class", "validator-rules.xml");

    final VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(formPath);
    if (virtualFile == null) {
      return null;
    } else {
      return PsiManager.getInstance(project).findFile(virtualFile);
    }
  }

}
