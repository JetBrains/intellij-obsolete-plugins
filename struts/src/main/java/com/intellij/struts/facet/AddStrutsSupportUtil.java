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

package com.intellij.struts.facet;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.javaee.model.xml.web.Servlet;
import com.intellij.javaee.model.xml.web.ServletMapping;
import com.intellij.javaee.model.xml.web.WebApp;
import com.intellij.javaee.web.CommonParamValue;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.struts.StrutsConstants;
import com.intellij.struts.StrutsFileTemplateGroupFactory;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.*;
import com.intellij.struts.facet.ui.StrutsVersion;
import com.intellij.struts.util.FormatUtil;
import com.intellij.util.descriptors.ConfigFile;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author nik
 */
public class AddStrutsSupportUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.struts.facet.AddStrutsSupportUtil");
  @NonNls public static final String STRUTS_CONFIG_FILE_NAME = "struts-config.xml";
  @NonNls private static final String TILES_REQUEST_PROCESSOR = "org.apache.struts.tiles.TilesRequestProcessor";
  @NonNls private static final String TILES_PLUGIN = "org.apache.struts.tiles.TilesPlugin";
  @NonNls private static final String VALIDATOR_PLUGIN = "org.apache.struts.validator.ValidatorPlugIn";
  @NonNls private static final String DEFAULT_STRUTS_MAPPING = "*.do";
  @NonNls private static final String WEB_INF = "WEB-INF";
  @NonNls private static final String ACTION_SERVLET_NAME = "action";
  @NonNls private static final String STRUTS_CONFIG_LOCATION = "/WEB-INF/struts-config.xml";
  @NonNls private static final String CONFIG_PARAM = "config";
  @NonNls private static final String VERSION_PROPERTY = "version";
  @NonNls private static final String SUBVERSION_PARAMETER = "subversion";
  @NonNls private static final String DEFINITIONS_CONFIG_PROPERTY = "definitions-config";
  @NonNls private static final String TILES_DEFS_XML = "/WEB-INF/tiles-defs.xml";
  @NonNls private static final String PATHNAMES_PROPERTY = "pathnames";
  @NonNls private static final String VALIDATION_XML = ",/WEB-INF/validation.xml";
  @NonNls private static final String DOTTED = "dotted";

  private AddStrutsSupportUtil() {
  }

  private static PsiFile[] getStrutsFiles(@NotNull final WebFacet webFacet) {
    ArrayList<PsiFile> result = new ArrayList<>();
    final ConfigFile deploymentDescriptor = webFacet.getWebXmlDescriptor();
    if (deploymentDescriptor != null) {
      final PsiFile psiFile = deploymentDescriptor.getPsiFile();
      if (psiFile != null) {
        result.add(psiFile);
      }
    }
    final List<StrutsModel> strutsModels = StrutsManager.getInstance().getAllStrutsModels(webFacet.getModule());
    if (!strutsModels.isEmpty()) {
      final Set<XmlFile> files = strutsModels.get(0).getConfigFiles();
      if (!files.isEmpty()) {
        result.add(files.iterator().next());
      }
    }
    return PsiUtilCore.toPsiFileArray(result);
  }

  public static boolean addSupport(final WebFacet webFacet, final boolean hasValidatorSupport, final boolean hasTilesSupport, final boolean isStruts13x) throws Exception {
    final Module module = webFacet.getModule();
    final Project project = module.getProject();
    final WebApp app = webFacet.getRoot();
    if (app == null) {
      return false;
    }

    String configFilesList = patchWebXml(module, app);
    if (configFilesList == null) {
      return false;
    }
    String[] configs = configFilesList.split(",");

    final WebDirectoryUtil util = WebDirectoryUtil.getWebDirectoryUtil(project);
    VirtualFile file = util.findVirtualFileByPath(configs[0].trim(), webFacet);

    // check WEB-INF
    VirtualFile webinf;

    PsiFile configFile;
    if (file == null) {
      webinf = util.findVirtualFileByPath("/WEB-INF", webFacet);
      if (webinf == null) {
        webinf = getOrCreateWebInf(webFacet);
      }

      configFile = createConfigFile(project, webinf, isStruts13x);
    }
    else {
      webinf = file.getParent();
      configFile = PsiManager.getInstance(project).findFile(file);
    }
    if (configFile == null) {
      return false;
    }

    StrutsConfig config = StrutsManager.getInstance().getStrutsConfig(configFile);
    if (config == null) {
      return false;
    }

    StrutsSupportModel support = StrutsSupportModel.checkStrutsSupport(webFacet);
    if (webinf != null) {
      final PsiDirectory directory = PsiManager.getInstance(project).findDirectory(webinf);
      if (directory != null) {
        if (!support.isTiles() && hasTilesSupport) {
          addTilesSupport(directory, config);
        }

        if (!support.isValidation() && hasValidatorSupport) {
          addValidationSupport(config, directory, webFacet.getModule(), isStruts13x);
        }
      }
    }
    deleteUnusedPlugins(config, hasValidatorSupport, hasTilesSupport);

    return true;
  }

  @Nullable
  private static PsiFile createConfigFile(final Project project, final VirtualFile webinf, final boolean isStruts13x) throws Exception {
    final PsiDirectory directory = PsiManager.getInstance(project).findDirectory(webinf);
    if (directory == null) {
      return null;
    }
    final FileTemplate fileTemplate = FileTemplateManager.getInstance(project).getJ2eeTemplate(StrutsFileTemplateGroupFactory.STRUTS_CONFIG_XML);
    final Properties properties = FileTemplateManager.getInstance(project).getDefaultProperties();
    properties.setProperty(VERSION_PROPERTY, "1");
    properties.setProperty(SUBVERSION_PARAMETER, isStruts13x ? "3" : "2");
    return (PsiFile)FileTemplateUtil.createFromTemplate(fileTemplate, STRUTS_CONFIG_FILE_NAME, properties, directory);
  }

  @Nullable
  private static VirtualFile getOrCreateWebInf(final WebFacet webFacet) throws IOException {

    final WebDirectoryUtil util = WebDirectoryUtil.getWebDirectoryUtil(webFacet.getModule().getProject());
    VirtualFile webinf = util.findVirtualFileByPath("/WEB-INF", webFacet);
    if (webinf == null) {
      VirtualFile webXml = webFacet.getWebXmlDescriptor().getVirtualFile();
      if (webXml == null) {
        return null;
      }
      final VirtualFile parent = webXml.getParent();
      if (parent != null && parent.getName().equals(WEB_INF)) {
        // add root with it
        webFacet.addWebRoot(parent, "/" + WEB_INF);
        webinf = parent;
      }
      else {

        WebRoot root;
        List webRoots = webFacet.getWebRoots();
        if (webRoots.isEmpty()) {
          VirtualFile[] roots = ModuleRootManager.getInstance(webFacet.getModule()).getContentRoots();
          if (roots.length == 0) {
            throw new IllegalStateException("The module has no content roots");
          }
          VirtualFile resources = roots[0].createChildDirectory(AddStrutsSupportUtil.class, "resources");
          root = webFacet.addWebRoot(resources, "/");
        }
        else {
          root = (WebRoot)webRoots.get(0);
        }
        final VirtualFile file = root.getFile();
        if (file != null) {
          webinf = file.createChildDirectory(AddStrutsSupportUtil.class, WEB_INF);
        }
      }
    }
    return webinf;
  }

  @Nullable
  private static String patchWebXml(final @NotNull Module module, final @NotNull WebApp app) {
    StrutsModel strutsModel = null;
    List<StrutsModel> strutsModels = StrutsManager.getInstance().getAllStrutsModels(module);
    if (!strutsModels.isEmpty()) {
      strutsModel = strutsModels.get(0);
    }

    if (strutsModel != null && !(strutsModel.getActionServlet() instanceof Servlet)) {
      return null;
    }

    if (!FileModificationService.getInstance().prepareFileForWrite(app.getContainingFile())) {
      return null;
    }

    Servlet servlet = strutsModel == null ? null : (Servlet)strutsModel.getActionServlet();
    if (servlet == null) {
      servlet = app.addServlet();
      servlet.getServletName().setValue(ACTION_SERVLET_NAME);
      servlet.getServletClass().setStringValue(StrutsConstants.ACTION_SERVLET_CLASS);
    }

    Integer loadOnStartup = servlet.getLoadOnStartup().getValue();
    if (loadOnStartup == null) {
      servlet.getLoadOnStartup().setStringValue("2");
    }

    if (strutsModel == null) {
      ServletMapping mapping = app.addServletMapping();
      mapping.getServletName().setValue(servlet);
      mapping.addUrlPattern().setValue(DEFAULT_STRUTS_MAPPING);
    }

    // create struts-config

    CommonParamValue configParam = DomUtil.findByName(servlet.getInitParams(), CONFIG_PARAM);
    if (configParam == null) {
      configParam = servlet.addInitParam();
      configParam.getParamName().setValue(CONFIG_PARAM);
    }
    String configFile = configParam.getParamValue().getValue();
    if (configFile == null || configFile.trim().length() == 0) {
      configParam.getParamValue().setValue(STRUTS_CONFIG_LOCATION);
      configFile = STRUTS_CONFIG_LOCATION;
    }
    return configFile;
  }

  private static void addValidationSupport(final StrutsConfig root, @NotNull final PsiDirectory webinf, final Module module, final boolean isStruts13x) throws Exception {
    addValidationPlugin(root, isStruts13x);

    PsiFile validationXml = webinf.findFile("validation.xml");
    if (validationXml == null) {
      if (isStruts13x) {
        createValidationXml("1_3_0", webinf);
      }
      else {
        copyValidatorRulesAndCreateValidation(webinf);

      }
    }

    addMessageResource(module, root);
  }

  private static void addTilesSupport(final @NotNull PsiDirectory webinf, @NotNull final StrutsConfig root) throws Exception {

    final Project project = root.getManager().getProject();
    final PsiManager manager = PsiManager.getInstance(project);
    Controller controller = root.getController();
    controller.ensureXmlElementExists();
    PsiClass processorClass = controller.getProcessorClass().getValue();
    boolean requestProcessorConfigured = false;
    if (processorClass != null) {
      final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
      final PsiClass tilesClass = JavaPsiFacade.getInstance(manager.getProject()).findClass(TILES_REQUEST_PROCESSOR, scope);
      requestProcessorConfigured = tilesClass != null && InheritanceUtil.isInheritorOrSelf(processorClass, tilesClass, true);
    }
    if (!requestProcessorConfigured) {
      controller.getProcessorClass().setStringValue(TILES_REQUEST_PROCESSOR);
    }

    PlugIn plugin = root.addPlugIn();
    plugin.getClassName().setStringValue(TILES_PLUGIN);

    SetProperty prop = plugin.addSetProperty();
    prop.getProperty().setStringValue(DEFINITIONS_CONFIG_PROPERTY);
    prop.getValue().setStringValue(TILES_DEFS_XML);

    PsiFile tilesDefs = webinf.findFile("tiles-defs.xml");
    if (tilesDefs == null) {
      final FileTemplate fileTemplate = FileTemplateManager.getInstance(webinf.getProject()).getJ2eeTemplate(StrutsFileTemplateGroupFactory.TILES_DEFS_XML);
      FileTemplateUtil.createFromTemplate(fileTemplate, "tiles-defs.xml", null, webinf);
    }
  }

  private static void addValidationPlugin(final StrutsConfig root, final boolean isStruts13x) {
    PlugIn plugin = root.addPlugIn();
    plugin.getClassName().setStringValue(VALIDATOR_PLUGIN);

    SetProperty prop = plugin.addSetProperty();
    prop.getProperty().setStringValue(PATHNAMES_PROPERTY);
    @NonNls String rules = isStruts13x ? "/org/apache/struts/validator/validator-rules.xml" : "/WEB-INF/validator-rules.xml";
    prop.getValue().setStringValue(rules + VALIDATION_XML);
  }

  private static void copyValidatorRulesAndCreateValidation(@NotNull final PsiDirectory webinf) throws Exception {
    // copying validator-rules.xml
    PsiFile rulesXml = webinf.findFile("validator-rules.xml");
    if (rulesXml == null) {
      final FileTemplate fileTemplate = FileTemplateManager.getInstance(webinf.getProject()).getJ2eeTemplate(StrutsFileTemplateGroupFactory.VALIDATOR_RULES_XML);
      rulesXml = (PsiFile)FileTemplateUtil.createFromTemplate(fileTemplate, "validator-rules.xml", null, webinf);
    }

    // creating validation.xml
    if (rulesXml instanceof XmlFile) {
      final XmlDocument document = ((XmlFile)rulesXml).getDocument();
      if (document != null) {
        XmlProlog prolog = document.getProlog();
        if (prolog != null && prolog.getDoctype() != null) {
          String uri = prolog.getDoctype().getDtdUri();
          String v = FormatUtil.getVersion(uri);
          if (v != null) {
            createValidationXml(v, webinf);
          }
        }
      }
    }
  }

  private static void addMessageResource(final Module module, final StrutsConfig config) throws Exception {
    final Project project = module.getProject();

    VirtualFile[] roots = ModuleRootManager.getInstance(module).getSourceRoots();
    if (roots.length > 0) {

      final PsiDirectory directory = PsiManager.getInstance(project).findDirectory(roots[0]);
      if (directory != null) {
        @NonNls String newParam = "MessageResources";
        for (MessageResources res : config.getMessageResources()) {
          String param = res.getParameter().getValue();
          if (param != null && param.equals(newParam)) {
            newParam = "ValidatorResources";
            break;
          }
        }

        String name = newParam;
        int i = 1;
        while (directory.findFile(name + ".properties") != null) {
          name = newParam + i++;
        }

        final FileTemplate fileTemplate =
          FileTemplateManager.getInstance(project).getJ2eeTemplate(StrutsFileTemplateGroupFactory.MESSAGE_RESOURCES_PROPERTIES);
        FileTemplateUtil.createFromTemplate(fileTemplate, name + ".properties", null, directory);

        MessageResources res = config.addMessageResources();
        res.getParameter().setValue(name);
      }
    }
  }

  private static void deleteUnusedPlugins(final StrutsConfig root, final boolean hasValidatorSupport, final boolean hasTilesSupport) {
      for (PlugIn plugin : root.getPlugIns()) {
        final String className = plugin.getClassName().getStringValue();
        if (className == null) {
          continue;
        }
        if (className.equals(VALIDATOR_PLUGIN)) {
          if (!hasValidatorSupport) {
            plugin.undefine();
          }
        }
        else if (className.equals(TILES_PLUGIN)) {
          if (!hasTilesSupport) {
            plugin.undefine();
          }
        }
      }
  }

  private static void createValidationXml(final String v, @NotNull PsiDirectory directory) throws Exception {
    String dotted = FormatUtil.replace(v, '_', '.');

    final FileTemplate fileTemplate = FileTemplateManager.getInstance(directory.getProject()).getJ2eeTemplate(StrutsFileTemplateGroupFactory.VALIDATION_XML);
    final Properties properties = FileTemplateManager.getInstance(directory.getProject()).getDefaultProperties();
    properties.setProperty(VERSION_PROPERTY, v);
    properties.setProperty(DOTTED, dotted);
    FileTemplateUtil.createFromTemplate(fileTemplate, "validation.xml", properties, directory);
  }

  public static void addSupportInWriteCommandAction(final WebFacet webFacet, final boolean hasValidatorSupport, final boolean hasTilesSupport,
                                              final StrutsVersion version) {
    final Project project = webFacet.getModule().getProject();
    DumbService.getInstance(project).runWhenSmart(() -> WriteCommandAction.writeCommandAction(project, getStrutsFiles(webFacet)).run(() -> {
      try {
        addSupport(webFacet, hasValidatorSupport, hasTilesSupport, version == StrutsVersion.Struts1_3_8);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }));
  }
}
