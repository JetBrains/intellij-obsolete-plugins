package com.intellij.vaadin.actions;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.PackageUtil;
import com.intellij.javaee.web.CommonParamValue;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.Servlet;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vaadin.VaadinBundle;
import com.intellij.vaadin.framework.VaadinVersion;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import com.intellij.vaadin.templates.VaadinTemplateNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CreateWidgetAction extends VaadinCreateElementActionBase {
  public CreateWidgetAction() {
    super(VaadinBundle.message("action.vaadin.widget.text"), VaadinBundle.message("action.create.new.vaadin.widget.description"));
  }

  @Override
  protected PsiElement @NotNull [] invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory) {
    MyInputValidator validator = new MyInputValidator(project, directory);
    Module module = ModuleUtilCore.findModuleForFile(directory.getVirtualFile(), project);
    if (module == null) return PsiElement.EMPTY_ARRAY;

    new CreateWidgetDialog(module, validator).show();
    return validator.getCreatedElements();
  }

  @Override
  protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory) throws Exception {
    Project project = directory.getProject();
    Module module = ModuleUtilCore.findModuleForFile(directory.getVirtualFile(), project);
    if (module == null) return PsiElement.EMPTY_ARRAY;
    PsiUtil.checkIsIdentifier(directory.getManager(), newName);
    JavaDirectoryService.getInstance().checkCreateClass(directory, newName);
    VaadinVersion version = VaadinVersionUtil.getVaadinVersion(module);
    VaadinTemplateNames templateNames = version.getTemplateNames();

    GwtModule widgetSetModule = findWidgetSetModule(module);
    PsiPackage basePackage = JavaDirectoryService.getInstance().getPackage(directory);
    String basePackageQualifiedName = basePackage != null ? basePackage.getQualifiedName() : null;
    PsiDirectory clientDirectory = null;
    PsiElement widgetSetFile = null;
    if (widgetSetModule != null) {
      VirtualFile clientDir = ContainerUtil.getFirstItem(widgetSetModule.getSourceRoots(false));
      if (clientDir != null) {
        clientDirectory = PsiManager.getInstance(project).findDirectory(clientDir);
      }
    }

    if (clientDirectory == null) {
      clientDirectory = PackageUtil.findOrCreateSubdirectory(directory, "client");
    }
    for (VaadinTemplateNames.ClientWidgetClassTemplate template : templateNames.getClientWidgetClasses()) {
      JavaDirectoryService.getInstance().checkCreateClass(clientDirectory, newName + template.getNameSuffix());
    }

    if (widgetSetModule == null) {
      FileTemplate template = FileTemplateManager.getInstance(project).getJ2eeTemplate(templateNames.getWidgetSetModule());
      Properties properties = new Properties(FileTemplateManager.getInstance(project).getDefaultProperties());
      properties.setProperty(GwtTemplates.GWT_MODULE_DOCTYPE_VAR, GwtFacet.getGwtVersion(module).getGwtModuleDocTypeString());
      String widgetSetName = "WidgetSet";
      widgetSetFile = FileTemplateUtil.createFromTemplate(template, widgetSetName + ".gwt.xml", properties, directory);
      String widgetSetQualifiedName = StringUtil.getQualifiedName(basePackageQualifiedName, widgetSetName);
      registerWidgetSetInServlet(module, version, widgetSetQualifiedName);
    }

    PsiPackage clientPackage = JavaDirectoryService.getInstance().getPackage(clientDirectory);
    String clientPackageQualifiedName = clientPackage != null ? clientPackage.getQualifiedName() : null;

    Map<String, String> properties = new HashMap<>();
    properties.put("BASE_PACKAGE_NAME", basePackageQualifiedName);
    properties.put("CLIENT_PACKAGE_NAME", clientPackageQualifiedName);
    properties.put("WIDGET_NAME", newName);
    List<PsiElement> result = new ArrayList<>();
    ContainerUtil.addIfNotNull(result, widgetSetFile);
    result.add(JavaDirectoryService.getInstance().createClass(directory, newName, templateNames.getWidget(),
                                                              false, properties));
    for (VaadinTemplateNames.ClientWidgetClassTemplate template : templateNames.getClientWidgetClasses()) {
      result.add(JavaDirectoryService.getInstance().createClass(clientDirectory, newName + template.getNameSuffix(),
                                                                template.getTemplateName(), false, properties));
    }

    return result.toArray(PsiElement.EMPTY_ARRAY);
  }

  private static void registerWidgetSetInServlet(Module module, VaadinVersion version, String widgetSetQualifiedName) {
    GwtFacet facet = GwtFacet.getInstance(module);
    if (facet != null) {
      WebFacet webFacet = facet.getWebFacet();
      if (webFacet != null) {
        WebApp webApp = webFacet.getRoot();
        if (webApp != null) {
          for (Servlet servlet : webApp.getServlets()) {
            if (version.getServletClass().equals(servlet.getServletClass().getStringValue())) {
              CommonParamValue param = servlet.addInitParam();
              param.getParamName().setValue("widgetset");
              param.getParamValue().setValue(widgetSetQualifiedName);
            }
          }
        }
      }
    }
  }

  @Nullable
  private static GwtModule findWidgetSetModule(Module module) {
    VaadinVersion version = VaadinVersionUtil.getVaadinVersion(module);
    GwtModulesManager modulesManager = GwtModulesManager.getInstance(module.getProject());
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    List<GwtModule> widgetSetBaseModules = modulesManager.findGwtModulesByQualifiedName(version.getWidgetSetModuleName(), scope);

    for (GwtModule gwtModule : modulesManager.getGwtModules(module, false)) {
      if (widgetSetBaseModules.isEmpty() || modulesManager.isInheritedOrSelf(gwtModule, widgetSetBaseModules)) {
        return gwtModule;
      }
    }
    return null;
  }

  @Override
  protected String getErrorTitle() {
    return VaadinBundle.message("dialog.title.cannot.create.widget");
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return VaadinBundle.message("command.name.creating.vaadin.widget", newName);
  }
}
