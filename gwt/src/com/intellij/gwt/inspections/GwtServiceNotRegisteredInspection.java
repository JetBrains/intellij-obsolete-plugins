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

package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.rpc.GwtServletUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.web.GwtWebUtil;
import com.intellij.javaee.constants.JavaeeCommonConstants;
import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.Servlet;
import com.intellij.javaee.web.model.xml.ServletMapping;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.xml.util.XmlTagUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GwtServiceNotRegisteredInspection extends BaseGwtInspection {
  private static final ExtensionPointName<Condition<PsiClass>> UNREGISTERED_SERVLET_FILTERS = ExtensionPointName.create("com.intellij.gwt.unregisteredServletFilter");

  @Override
  public ProblemDescriptor @Nullable [] checkFile(final @NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!JavaeeCommonConstants.WEB_XML.equals(file.getName())) return null;

    WebFacet webFacet = WebUtil.getWebFacet(file);
    if (webFacet == null) return null;

    GwtFacet gwtFacet = GwtFacet.getInstance(webFacet.getModule());
    if (gwtFacet == null) return null;

    WebApp webApp = webFacet.getRoot();
    if (webApp == null || !file.equals(webApp.getContainingFile())) return null;

    Map<CommonServlet, Pair<GwtModule, String>> expectedUrlPatterns = new HashMap<>();
    Map<CommonServlet, String> serviceNames = new HashMap<>();
    final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(manager.getProject());

    for (Servlet servlet : webApp.getServlets()) {
      PsiClass servletClass = servlet.getServletClass().getValue();
      if (servletClass == null || !RemoteServiceUtil.isRemoteServiceImplementation(servletClass)) continue;

      PsiClass serviceInterface = RemoteServiceUtil.findRemoteServiceInterface(servletClass);
      if (serviceInterface == null) continue;

      PsiFile psiFile = serviceInterface.getContainingFile();
      if (psiFile == null) continue;

      VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile == null) continue;

      GwtModule gwtModule = gwtModulesManager.findGwtModuleByClientSourceFile(virtualFile);
      if (gwtModule == null) continue;

      String serviceName = serviceInterface.getName();
      expectedUrlPatterns.put(servlet, Pair.create(gwtModule, GwtServletUtil.getServletPath(gwtModule, serviceName, servletClass)));
      serviceNames.put(servlet, serviceName);
    }

    Map<CommonServlet, ServletMapping> singleMappings = new HashMap<>();
    for (ServletMapping mapping : webApp.getServletMappings()) {
      CommonServlet servlet = mapping.getServletName().getValue();
      if (servlet != null) {
        Pair<GwtModule, String> urlPattern = expectedUrlPatterns.get(servlet);

        if (singleMappings.containsKey(servlet)) {
          singleMappings.remove(servlet);
        }
        else {
          singleMappings.put(servlet, mapping);
        }
        if (urlPattern != null && containsServletPath(mapping, urlPattern.getFirst(), urlPattern.getSecond(), gwtFacet.getModule())) {
          expectedUrlPatterns.remove(servlet);
        }
      }
    }

    if (expectedUrlPatterns.isEmpty()) return null;

    List<ProblemDescriptor> problems = new SmartList<>();

    for (CommonServlet servlet : expectedUrlPatterns.keySet()) {
      ServletMapping mapping = singleMappings.get(servlet);
      String serviceName = serviceNames.get(servlet);
      Pair<GwtModule, String> pair = expectedUrlPatterns.get(servlet);
      String urlPattern = GwtServletUtil.getServletUrlPattern(gwtFacet, pair.getFirst(), pair.getSecond());
      AddServletMappingFix quickfix = new AddServletMappingFix(webApp, servlet, serviceName, urlPattern, mapping);
      List<GenericDomValue<String>> urlPatterns = mapping != null ? mapping.getUrlPatterns() : Collections.emptyList();
      if (urlPatterns.size() != 1) {
        String message = GwtBundle.message("problem.description.correct.servlet.mapping.is.not.specified.for.remote.service.0", serviceName);
        XmlToken token = Objects.requireNonNull(XmlTagUtil.getStartTagNameElement(Objects.requireNonNull(servlet.getXmlTag())));
        problems.add(manager.createProblemDescriptor(token, message, quickfix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
      else {
        XmlTag tag = Objects.requireNonNull(urlPatterns.get(0).getXmlTag());
        String message = GwtBundle.message("problem.description.incorrect.servlet.mapping.for.remote.service.0", serviceName);
        problems.add(manager.createProblemDescriptor(tag, XmlTagUtil.getTrimmedValueRange(tag), message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                     isOnTheFly, quickfix));
      }
    }

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static boolean containsServletPath(final ServletMapping mapping, GwtModule serviceModule, final String relativePath,
                                             Module module) {
    final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(module.getProject());
    for (GenericDomValue<String> pattern : mapping.getUrlPatterns()) {
      final String urlPattern = pattern.getValue();
      if (urlPattern != null && urlPattern.endsWith(relativePath)) {
        String modulePath = StringUtil.trimEnd(StringUtil.trimStart(urlPattern.substring(0, urlPattern.length() - relativePath.length()), "/"), "/");
        final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
        final Collection<GwtModule> gwtModules = gwtModulesManager.findGwtModulesByOutputName(modulePath, scope);
        for (GwtModule gwtModule : gwtModules) {
          if (gwtModulesManager.isInheritedOrSelf(gwtModule, serviceModule)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!shouldCheck(aClass)) return null;

    final Project project = manager.getProject();
    if (!RemoteServiceUtil.isRemoteServiceImplementation(aClass)) {
      return null;
    }

    final PsiClass service = RemoteServiceUtil.findRemoteServiceInterface(aClass);
    if (service == null) return null;

    GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(project);
    final VirtualFile virtualFile = service.getContainingFile().getVirtualFile();
    if (virtualFile == null) return null;

    GwtModule gwtModule = gwtModulesManager.findGwtModuleByClientSourceFile(virtualFile);
    if (gwtModule == null) return null;

    final Module module = gwtModule.getModule();
    if (module == null) return null;

    GwtFacet facet = GwtFacet.findFacetBySourceFile(project, gwtModule.getModuleFile());
    if (facet == null) return null;

    for (Condition<PsiClass> condition : UNREGISTERED_SERVLET_FILTERS.getExtensions()) {
      if (condition.value(aClass)) {
        return null;
      }
    }

    String serviceName = service.getName();
    final WebFacet webFacet = facet.getWebFacet();
    if (webFacet == null) {
      if (!facet.getSdkVersion().isHostedModeRequiresWebXml()) {
        return null;
      }

      String message = GwtBundle.message("problem.description.remote.service.is.not.registered.as.a.servlet.in.web.xml", serviceName);
      final Collection<WebFacet> webFacets = WebFacet.getInstances(module);
      LocalQuickFix[] quickFixes;
      if (webFacets.isEmpty()) {
        quickFixes = new LocalQuickFix[] {
            new CreateWebFacetAndRegisterQuickFix(facet, gwtModule, aClass, serviceName)
        };
      }
      else {
        List<LocalQuickFix> fixes = new ArrayList<>();
        for (WebFacet web : webFacets) {
          final WebApp webApp = web.getRoot();
          if (webApp != null) {
            fixes.add(new RegisterServiceQuickFix(facet, gwtModule, webApp, aClass, serviceName, web));
          }
        }
        quickFixes = fixes.toArray(LocalQuickFix.EMPTY_ARRAY);
      }
      return new ProblemDescriptor[] {
          manager.createProblemDescriptor(getElementToHighlight(aClass), message, isOnTheFly, quickFixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
      };
    }

    final WebApp webApp = webFacet.getRoot();
    if (webApp == null) return null;

    Servlet servlet = GwtServletUtil.findServlet(webApp, aClass);
    if (servlet == null) {
      String message = GwtBundle.message("problem.description.remote.service.is.not.registered.as.a.servlet.in.web.xml", serviceName);
      RegisterServiceQuickFix quickFix = new RegisterServiceQuickFix(facet, gwtModule, webApp, aClass, serviceName, webFacet);
      return new ProblemDescriptor[]{manager.createProblemDescriptor(getElementToHighlight(aClass), message, quickFix,
                                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly)};
    }

    return null;
  }

  private static final class CreateWebFacetAndRegisterQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final GwtFacet myFacet;
    private final GwtModule myGwtModule;
    private final String myServiceName;

    private CreateWebFacetAndRegisterQuickFix(GwtFacet facet, GwtModule gwtModule, PsiClass serviceImpl, String serviceName) {
      super(GwtBundle.message("quickfix.family.name.create.web.facet.and.register.remote.service.in.web.xml"), GwtBundle.message("quickfix.name.create.web.facet.and.register.remote.service.0.in.web.xml", serviceName), serviceImpl);
      myFacet = facet;
      myGwtModule = gwtModule;
      myServiceName = serviceName;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass serviceImpl)) return;

      final VirtualFile root = findContentRoot();
      LOG.assertTrue(root != null, myGwtModule.getModuleFile());
      final WebFacet webFacet = GwtWebUtil.createWebFacet(myFacet, root);

      final WebApp webApp = webFacet.getRoot();
      LOG.assertTrue(webApp != null);
      GwtServletUtil.registerServletForService(myFacet, myGwtModule, webApp, serviceImpl, myServiceName);
    }

    private @Nullable VirtualFile findContentRoot() {
      final VirtualFile[] contentRoots = ModuleRootManager.getInstance(myFacet.getModule()).getContentRoots();
      for (VirtualFile root : contentRoots) {
        if (VfsUtilCore.isAncestor(root, myGwtModule.getModuleFile(), false)) {
          return root;
        }
      }
      return null;
    }
  }

  private static class RegisterServiceQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    protected final GwtFacet myFacet;
    private final GwtModule myGwtModule;
    private final WebApp myWebApp;
    private final String myServiceName;
    private final WebFacet myWebFacet;

    RegisterServiceQuickFix(final GwtFacet facet, final GwtModule gwtModule, final WebApp webApp, final PsiClass serviceImpl,
                                   final String serviceName, @NotNull WebFacet webFacet) {
      super(webFacet.equals(facet.getWebFacet()) ? GwtBundle.message("quickfix.family.name.register.remote.service.in.web.xml")
                                                 : GwtBundle.message("quickfix.family.name.connect.gwt.facet.and.register.remote.service.in.web.xml"),
            webFacet.equals(facet.getWebFacet()) ? GwtBundle.message("quickfix.name.register.remote.service.0.in.web.xml", serviceName)
                                                 : GwtBundle.message("quickfix.name.connect.gwt.facet.to.0.facet.and.register.remote.service.1.in.web.xml", webFacet.getName(), serviceName),
            serviceImpl);
      myFacet = facet;
      myGwtModule = gwtModule;
      myWebApp = webApp;
      myServiceName = serviceName;
      myWebFacet = webFacet;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass serviceImpl)) return;

      if (!ReadonlyStatusHandler.getInstance(project)
        .ensureFilesWritable(Collections.singletonList(DomUtil.getFile(myWebApp).getVirtualFile())).hasReadonlyFiles()) {
        if (!myWebFacet.equals(myFacet.getWebFacet())) {
          myFacet.getConfiguration().setWebFacetName(myWebFacet.getName());
        }
        GwtServletUtil.registerServletForService(myFacet, myGwtModule, myWebApp, serviceImpl, myServiceName);
      }
    }
  }

  private static class AddServletMappingFix implements LocalQuickFix {
    private final WebApp myRoot;
    private final CommonServlet myServlet;
    private final String myServiceName;
    private final String myUrlPattern;
    private final ServletMapping myExistingMapping;

    AddServletMappingFix(final WebApp root, final CommonServlet servlet, final String serviceName, final String urlPattern, ServletMapping existentMapping) {
      myRoot = root;
      myServlet = servlet;
      myServiceName = serviceName;
      myUrlPattern = urlPattern;
      myExistingMapping = existentMapping;
    }

    @Override
    public @NotNull String getName() {
      return myExistingMapping == null || myExistingMapping.getUrlPatterns().size() > 1 ?
             GwtBundle.message("quickfix.name.add.servlet.mapping.for.remote.service.0", myServiceName)
             : GwtBundle.message("quickfix.name.set.correct.servlet.mapping.for.remote.service.0", myServiceName);
    }

    @Override
    public @NotNull String getFamilyName() {
      return myExistingMapping == null || myExistingMapping.getUrlPatterns().size() > 1
             ? GwtBundle.message("quickfix.family.name.add.servlet.mapping.for.remote.service")
             : GwtBundle.message("quickfix.family.name.set.servlet.mapping.for.remote.service");
    }

    @Override
    public void applyFix(final @NotNull Project project, final @NotNull ProblemDescriptor descriptor) {
      if (myExistingMapping == null) {
        GwtServletUtil.addServletMapping(myRoot, myServlet, myUrlPattern);
      }
      else {
        List<GenericDomValue<String>> urlPatterns = myExistingMapping.getUrlPatterns();
        if (urlPatterns.size() == 1) {
          urlPatterns.get(0).setValue(myUrlPattern);
        }
        else {
          myExistingMapping.addUrlPattern().setValue(myUrlPattern);
        }
      }
    }
  }
}
