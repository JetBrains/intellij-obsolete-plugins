package com.intellij.gwt.rpc;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.Servlet;
import com.intellij.javaee.web.model.xml.ServletMapping;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.deployment.DeploymentUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierList;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GwtServletUtil {
  private GwtServletUtil() {
  }

  public static @NotNull String getDefaultServletPath(GwtModule module, String serviceName) {
    return "/" + serviceName;
  }

  public static @NotNull String getServletPath(GwtModule gwtModule, String serviceName, PsiClass serviceImpl) {
    final PsiClass serviceInterface = RemoteServiceUtil.findRemoteServiceInterface(serviceImpl);
    if (serviceInterface != null) {
      final PsiModifierList modifierList = serviceInterface.getModifierList();
      if (modifierList != null) {
        final PsiAnnotation servicePath = modifierList.findAnnotation(RemoteServiceUtil.SERVICE_PATH_ANNOTATION_NAME);
        if (servicePath != null) {
          final PsiAnnotationMemberValue value = servicePath.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
          if (value != null) {
            final String path = JamCommonUtil.getObjectValue(value, String.class);
            if (path != null) {
              return path;
            }
          }
        }
      }
    }

    List<GwtServlet> list = gwtModule.getServlets();
    final String serviceImplName = serviceImpl.getQualifiedName();
    if (serviceImplName != null) {
      for (GwtServlet servlet : list) {
        if (serviceImplName.equals(servlet.getServletClass().getValue())) {
          String path = servlet.getPath().getValue();
          if (path != null) {
            if (!path.startsWith("/")) {
              path = "/" + path;
            }
            return path;
          }
        }
      }
    }

    return getDefaultServletPath(gwtModule, serviceName);
  }

  public static String getServletUrlPattern(GwtFacet gwtFacet, GwtModule module, String serviceName, PsiClass serviceImpl) {
    return getServletUrlPattern(gwtFacet, module, getServletPath(module, serviceName, serviceImpl));
  }

  public static String getServletUrlPattern(GwtFacet gwtFacet, GwtModule module, final String servletPath) {
    String base = gwtFacet.getConfiguration().getPackagingRelativePath(module);
    return DeploymentUtil.concatPaths(base, servletPath);
  }

  public static void registerServletForService(GwtFacet gwtFacet, final GwtModule gwtModule, final WebApp root, final PsiClass servletImpl,
                                         final String serviceName) {
    registerServletForService(gwtModule, root, servletImpl, serviceName,
                              getServletUrlPattern(gwtFacet, gwtModule, serviceName, servletImpl));
  }

  public static void registerServletForService(final GwtModule gwtModule, final @NotNull WebApp root, final PsiClass servletImpl, final String serviceName,
                                               final String servletUrlPattern) {
    final Servlet servlet = root.addServlet();
    servlet.getServletClass().setValue(servletImpl);
    final String servletName = gwtModule.getQualifiedName() + " " + serviceName;
    servlet.getServletName().setValue(servletName);

    addServletMapping(root, servlet, servletUrlPattern);
  }

  public static void addServletMapping(final WebApp root, final CommonServlet servlet, final String servletUrlPattern) {
    final ServletMapping mapping = root.addServletMapping();
    mapping.getServletName().setValue(servlet);
    mapping.addUrlPattern().setValue(servletUrlPattern);
  }

  public static @Nullable CommonServlet findServletByPath(GwtModule gwtModule, String relativePath) {
    final GwtFacet facet = GwtFacet.getInstance(gwtModule);
    if (facet == null) return null;

    final WebFacet webFacet = facet.getWebFacet();
    if (webFacet == null) return null;

    final WebApp root = webFacet.getRoot();
    if (root == null) return null;

    final String urlPattern = getServletUrlPattern(facet, gwtModule, relativePath);
    for (ServletMapping mapping : root.getServletMappings()) {
      final List<GenericDomValue<String>> urlPatterns = mapping.getUrlPatterns();
      for (GenericDomValue<String> pattern : urlPatterns) {
        if (urlPattern.equals(pattern.getStringValue())) {
          return mapping.getServletName().getValue();
        }
      }
    }
    return null;
  }

  public static @NotNull List<String> getAllGwtServletsPaths(@NotNull GwtModule gwtModule) {
    final GwtFacet facet = GwtFacet.getInstance(gwtModule);
    if (facet == null) return Collections.emptyList();

    final WebFacet webFacet = facet.getWebFacet();
    if (webFacet == null) return Collections.emptyList();

    final WebApp root = webFacet.getRoot();
    if (root == null) return Collections.emptyList();

    final String rootPath = facet.getConfiguration().getPackagingRelativePath(gwtModule) + "/";
    List<String> paths = new ArrayList<>();
    for (ServletMapping servletMapping : root.getServletMappings()) {
      final CommonServlet servlet = servletMapping.getServletName().getValue();
      if (servlet != null && RemoteServiceUtil.isRemoteServiceImplementation(servlet.getPsiClass())) {
        for (GenericDomValue<String> pattern : servletMapping.getUrlPatterns()) {
          final String value = pattern.getValue();
          if (value != null && value.startsWith(rootPath)) {
            paths.add(value.substring(rootPath.length()));
          }
        }
      }
    }
    return paths;
  }

  public static boolean hasServlets(@NotNull GwtFacet facet) {
    final WebFacet webFacet = facet.getWebFacet();
    if (webFacet == null) return false;

    final WebApp root = webFacet.getRoot();
    if (root == null) return false;

    for (Servlet servlet : root.getServlets()) {
      if (RemoteServiceUtil.isRemoteServiceImplementation(servlet.getServletClass().getValue())) {
        return true;
      }
    }
    return false;
  }

  public static @Nullable Servlet findServlet(WebApp root, PsiClass servletImpl) {
    final List<Servlet> servlets = root.getServlets();
    final PsiManager psiManager = servletImpl.getManager();
    for (Servlet servlet : servlets) {
      if (psiManager.areElementsEquivalent(servletImpl, servlet.getServletClass().getValue())) {
        return servlet;
      }
    }
    return null;
  }
}
