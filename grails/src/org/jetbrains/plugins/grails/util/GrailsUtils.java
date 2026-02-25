// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.jsp.impl.TldDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.ResolveState;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.NameHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.pluginSupport.webflow.WebFlowUtils;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrAnnotationUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GrClassImplUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import static org.jetbrains.plugins.groovy.transformations.impl.GroovyObjectTransformationSupport.isGroovyObjectSupportMethod;

public final class GrailsUtils {

  private static final Logger LOG = Logger.getInstance(GrailsUtils.class);

  public static final @NonNls String GRAILS_INTEGRATION_TESTS = "test/integration/";
  public static final @NonNls String GRAILS_UNIT_TESTS = "test/unit/";
  public static final @NonNls String GRAILS_APP_DIRECTORY = "grails-app";
  public static final @NonNls String VIEWS_DIRECTORY = "views";
  public static final @NonNls String CONF_DIRECTORY = "conf";
  public static final @NonNls String BUILD_CONFIG = "BuildConfig.groovy";
  public static final @NonNls String CONFIG_GROOVY = "Config.groovy";

  public static final String GRAILS_NOTIFICATION_GROUP = "grails";

  public static final @NonNls String SOURCE_ROOT = "src";
  public static final @NonNls String JAVA_SOURCE_ROOT = "java";

  public static final @NonNls String webAppDir = "web-app";
  public static final @NonNls String metaInfDir = "META-INF";
  public static final @NonNls String webInfDir = "WEB-INF";
  public static final @NonNls String jsDir = "js";
  public static final @NonNls String cssDir = "css";
  public static final @NonNls String imagecDir = "images";

  public static final @NonNls String GRAILS_USER_LIBRARY = "Grails User Library";

  public static final @NonNls String GROOVY_EXTENSION = ".groovy";
  public static final @NonNls String TEST_DIR = "test";
  public static final @NonNls String TEMPLATES_DIR = "templates";

  // Grails run configuration
  public static @NonNls String GRAILS_RUN_DEFAULT_HOST = "localhost";
  public static @NonNls String GRAILS_RUN_DEFAULT_PORT = "8080";

  private static final String[] RESERVED_DIRS = new String[]{metaInfDir, webInfDir, jsDir, cssDir, imagecDir};

  public static final String ENVIRONMENTS = "environments";

  public static final String ENVIRONMENTS_METHOD_KIND = "grails:environments";

  public static final List<String> ENVIRONMENT_LIST = List.of("development", "production", "test");

  /**
   * #CHECK#
   * @see org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils.registerCommonWebProperties(...)
   */
  public static final String COMMON_WEB_PROPERTIES =
    " private org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap getParams() {}" +
    " private org.codehaus.groovy.grails.web.servlet.FlashScope getFlash() {}" +
    " private javax.servlet.http.HttpSession getSession() {}" +
    " private javax.servlet.http.HttpServletRequest getRequest() {}" +
    " private javax.servlet.ServletContext getServletContext() {}" +
    " private javax.servlet.http.HttpServletResponse getResponse() {}" +
    " private org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes getGrailsAttributes() {}" +
    " private org.codehaus.groovy.grails.commons.GrailsApplication getGrailsApplication() {}" +
    " private String getActionName() {}" +
    " private String getControllerName() {}" +
    " private org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest getWebRequest() {};";

  private GrailsUtils() {
  }

  public static @Nullable VirtualFile findParent(VirtualFile virtualFile, String parentFileName) {
    VirtualFile parent = virtualFile.getParent();

    while (parent != null && !parentFileName.equals(parent.getName())) {
      parent = parent.getParent();
    }

    return parent;
  }

  public static boolean isUnderWebAppDirectory(@NotNull GrailsApplication application, @NotNull PsiDirectory directory) {
    VirtualFile virtualFile = directory.getVirtualFile();

    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0)) {
      return isUnderWebAppDirectory3x(application.getRoot(), virtualFile);
    }

    if (virtualFile.getName().equals(webAppDir) && virtualFile.isDirectory()) {
      return true;
    }
    VirtualFile parent = findParent(virtualFile, webAppDir);
    if (parent == null || ArrayUtil.contains(virtualFile.getName(), RESERVED_DIRS)) return false;

    for (String reservedDir : RESERVED_DIRS) {
      if (findParent(virtualFile, reservedDir) != null) {
        return false;
      }
    }

    return true;
  }

  private static boolean isUnderWebAppDirectory3x(VirtualFile root, VirtualFile file) {
    VirtualFile srcMain = VfsUtil.findRelativeFile(root, "src", "main");
    if (srcMain == null) {
      return false;
    }
    VirtualFile webapp = srcMain.findChild("webapp");
    if (webapp != null && VfsUtilCore.isAncestor(webapp, file, false)) {
      return true;
    }
    VirtualFile resources = srcMain.findChild("resources");
    if (resources != null && VfsUtilCore.isAncestor(resources, file, false)) {
      return true;
    }
    return false;
  }

  public static boolean isConfigGroovyFile(@Nullable PsiElement file) {
    return isConfigFile(file, CONFIG_GROOVY);
  }

  public static boolean isConfigFile(@Nullable PsiElement file, String name) {
    if (!(file instanceof GroovyFileBase)) return false;

    VirtualFile virtualFile = ((GroovyFileBase)file).getOriginalFile().getVirtualFile();
    if (virtualFile == null || !name.equals(virtualFile.getName())) return false;

    VirtualFile conf = virtualFile.getParent();
    if (conf == null || !CONF_DIRECTORY.equals(conf.getName())) return false;

    VirtualFile grailsApp = conf.getParent();
    if (grailsApp == null || !GRAILS_APP_DIRECTORY.equals(grailsApp.getName())) return false;

    return ((GroovyFileBase)file).isScript();
  }

  public static boolean isBuildConfigFile(@Nullable PsiFile file) {
    return isConfigFile(file, BUILD_CONFIG);
  }

  public static @Nullable VirtualFile getViewDirectory(@Nullable VirtualFile gspFile) {
    for (VirtualFile f = gspFile; f != null; f = f.getParent()) {
      if (f.getName().equals(VIEWS_DIRECTORY) && f.isDirectory()) {
        VirtualFile parent = f.getParent();
        if (parent != null && parent.getName().equals(GRAILS_APP_DIRECTORY)) {
          return f;
        }
      }
    }

    return null;
  }

  public static boolean isUnderGrailsViewsDirectory(PsiDirectory directory) {
    return getViewDirectory(directory.getVirtualFile()) != null;
  }

  /**
   * @deprecated Use GrailsFramework.getInstance().findAppRoot(PsiElement), don't find appRoot by module,
   * because module can has several appRoots (e.g. common plugin module)
   */
  @Deprecated
  public static @Nullable VirtualFile findGrailsAppRoot(@Nullable Module module) {
    return GrailsFramework.getInstance().findAppRoot(module);
  }

  public static boolean hasSupport(@NotNull Module module) {
    return GrailsCommandExecutor.getGrailsExecutor(GrailsApplicationManager.findApplication(module)) != null;
  }

  public static boolean isGrailsPluginModule(@Nullable Module module) {
    return extractGrailsPluginName(module) != null;
  }

  public static @Nullable String extractGrailsPluginName(@Nullable Module module) {
    final VirtualFile root = findGrailsAppRoot(module);
    return extractGrailsPluginName(root);
  }

  public static @Nullable String extractGrailsPluginName(@Nullable VirtualFile root) {
    if (root == null) return null;

    for (VirtualFile child : root.getChildren()) {
      final String name = child.getName();
      if (name.endsWith("GrailsPlugin.groovy")) {
        return StringUtil.trimEnd(name, "GrailsPlugin.groovy");
      }
    }
    return null;
  }

  public static boolean isInGrailsTests(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile().getOriginalFile();
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return false;

    return isInGrailsTests(virtualFile, file.getProject());
  }

  public static boolean isInGrailsTests(@NotNull VirtualFile virtualFile, @NotNull Project project) {
    GrailsApplication application = GrailsApplicationManager.getInstance(project).findApplication(virtualFile);
    if (application == null) return false;
    return application.getScope(false, true).contains(virtualFile)
           || GlobalSearchScopesCore.projectTestScope(project).contains(virtualFile); // fallback to IDEA-based checking
  }

  public static @Nullable VirtualFile findViewsDirectory(@Nullable PsiElement element) {
    GrailsApplication application = GrailsApplicationManager.findApplication(element);
    if (application == null) return null;
    return application.getAppRoot().findChild(VIEWS_DIRECTORY);
  }

  public static @Nullable VirtualFile findI18nDirectory(@Nullable PsiElement element) {
    VirtualFile appDir = GrailsFramework.getInstance().findAppDirectory(element);
    if (appDir == null) return null;
    return appDir.findChild("i18n");
  }

  public static @Nullable VirtualFile findConfDirectory(@Nullable Module module) {
    VirtualFile root = GrailsFramework.getInstance().findAppDirectory(module);
    if (root == null) return null;
    return root.findChild(CONF_DIRECTORY);
  }

  public static @Nullable VirtualFile findConfDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild(CONF_DIRECTORY);
  }

  public static @Nullable String getTemplateName(String fileName) {
    if (fileName.length() > 5 && fileName.charAt(0) == '_' && fileName.endsWith(".gsp")) {
      return fileName.substring(1, fileName.length() - ".gsp".length());
    }

    return null;
  }

  public static String getFileNameByTemplateName(String templateName) {
    return '_' + templateName + ".gsp";
  }

  public static PsiMember toField(@Nullable PsiMethod getter) {
    return getter instanceof GrAccessorMethod ? ((GrAccessorMethod)getter).getProperty() : getter;
  }

  public static Map<String, PsiMethod> getControllerActions(@Nullable String controllerName, @NotNull Module module) {
    return getControllerActions(GrailsArtifact.CONTROLLER.getInstances(module, controllerName), module);
  }

  public static Map<String, PsiMethod> getControllerActions(Collection<? extends GrTypeDefinition> classes, @NotNull Module module) {
    GrailsStructure structure = GrailsStructure.getInstance(module);
    Boolean isGrails1_4 = structure == null || structure.isAtLeastGrails1_4();

    Map<String, PsiMethod> res = new LinkedHashMap<>();

    try {
      List<PsiMethod> allMethods = GrClassImplUtil.getAllMethods(classes);
      List<PsiMethod> viableMethods = ContainerUtil.filter(allMethods, m -> !isGroovyObjectSupportMethod(m));
      for (PsiMethod method : viableMethods) {
        String name = getActionName0(method, false, isGrails1_4);

        if (name != null) {
          PsiMethod oldValue = res.put(name, method);
          if (oldValue != null) res.put(name, oldValue);
        }
      }
    }
    catch (IndexNotReadyException ignored) {
    }

    return res;
  }

  public static Map<String, PsiMethod> collectClosureProperties(Collection<? extends PsiClass> classes) {
    if (classes.isEmpty()) return Collections.emptyMap();

    ClosurePropertiesProcessor processor = new ClosurePropertiesProcessor();

    for (PsiClass controllerClass : classes) {
      controllerClass.processDeclarations(processor, ResolveState.initial(), null, controllerClass);
    }

    return processor.getResults();
  }

  public static @Nullable PsiMethod getClosureProperty(Collection<? extends PsiClass> classes, @NotNull String propertyName) {
    if (classes.isEmpty()) return null;

    ClosureOnePropertyProcessor processor = new ClosureOnePropertyProcessor(propertyName);

    for (PsiClass controllerClass : classes) {
      if (!controllerClass.processDeclarations(processor, ResolveState.initial(), null, controllerClass)) break;
    }

    return processor.getResult();
  }

  public static class ClosurePropertiesProcessor implements PsiScopeProcessor, ElementClassHint {

    private Map<String, PsiMethod> results;

    @Override
    public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
      if (element instanceof PsiMethod method) {

        if (PsiUtil.isClosurePropertyGetter(method)) {
          if (results == null) results = new HashMap<>();
          String name = GroovyPropertyUtils.getPropertyNameByGetterName(method.getName(), false);
          PsiMethod oldValue = results.put(name, method);
          if (oldValue != null) results.put(name, oldValue);
        }
      }
      return true;
    }

    public Map<String, PsiMethod> getResults() {
      return results == null ? Collections.emptyMap() : results;
    }

    @Override
    public <T> T getHint(@NotNull Key<T> hintKey) {
      if (hintKey == ElementClassHint.KEY) {
        //noinspection unchecked
        return (T)this;
      }

      return null;
    }

    @Override
    public boolean shouldProcess(@NotNull DeclarationKind resolveKind) {
      return resolveKind == DeclarationKind.METHOD;
    }
  }

  public static class ClosureOnePropertyProcessor implements PsiScopeProcessor, ElementClassHint, NameHint {

    private final String myGetterName;
    private final String myPropertyName;

    private PsiMethod result;

    public ClosureOnePropertyProcessor(@NotNull String propertyName) {
      myGetterName = GroovyPropertyUtils.getGetterNameNonBoolean(propertyName);
      myPropertyName = propertyName;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
      if (element instanceof PsiMethod method) {

        if (PsiUtil.isClosurePropertyGetter(method)) {
          //if (myGetterName.equals(method.getName())) {  - don't use this
          if (myPropertyName.equals(GroovyPropertyUtils.getPropertyNameByGetterName(method.getName(), false))) {
            result = method;
            return false;
          }
        }
      }
      return true;
    }

    public PsiMethod getResult() {
      return result;
    }

    @Override
    public <T> T getHint(@NotNull Key<T> hintKey) {
      if (hintKey == NameHint.KEY || hintKey == ElementClassHint.KEY) {
        //noinspection unchecked
        return (T)this;
      }

      return null;
    }

    @Override
    public boolean shouldProcess(@NotNull DeclarationKind resolveKind) {
      return resolveKind == DeclarationKind.METHOD;
    }

    @Override
    public String getName(@NotNull ResolveState state) {
      return myGetterName;
    }
  }

  public static @Nullable VirtualFile getControllerDirByGsp(VirtualFile gspFile) {
    VirtualFile parent1 = gspFile.getParent();
    if (parent1 == null) return null;

    VirtualFile parent2 = parent1.getParent();
    if (parent2 == null) return null;

    VirtualFile parent3 = parent2.getParent();
    if (parent3 == null) return null;

    while (!GRAILS_APP_DIRECTORY.equals(parent3.getName()) || !VIEWS_DIRECTORY.equals(parent2.getName())) {
      parent1 = parent2;
      parent2 = parent3;
      parent3 = parent3.getParent();

      if (parent3 == null) return null;
    }

    return parent1;
  }

  public static @Nullable String getExistingControllerNameDirByGsp(VirtualFile gspFile, Project project) {
    String controllerName = getControllerNameByGsp(gspFile);
    if (controllerName == null) return null;

    Module module = ModuleUtilCore.findModuleForFile(gspFile, project);

    if (module != null && !GrailsArtifact.CONTROLLER.getInstances(module, controllerName).isEmpty()) {
      return controllerName;
    }

    return null;
  }

  public static @Nullable String getControllerNameByGsp(@NotNull VirtualFile gspFile) {
    VirtualFile controllerFolder = getControllerDirByGsp(gspFile);
    return controllerFolder == null ? null : controllerFolder.getName();
  }

  public static Object[] createPluginVariants(@NotNull Module module, boolean withVersion) {
    Collection<VirtualFile> roots = GrailsFramework.getInstance().getAllPluginRoots(module, false);

    Object[] res = new Object[roots.size()];

    int i = 0;

    for (VirtualFile root : roots) {
      String pluginName = extractGrailsPluginName(root);
      if (pluginName != null) {
        if (withVersion) {
          pluginName = root.getName();
        }
        else {
          pluginName = GrailsNameUtils.getScriptName(pluginName);
        }

        res[i++] = LookupElementBuilder.create(pluginName).withIcon(GroovyMvcIcons.Groovy_mvc_plugin);
      }
    }

    return res;
  }

  public static boolean isControllerAction(@Nullable PsiElement element) {
    return getActionName(element) != null;
  }

  public static @Nullable String getActionName(@Nullable PsiElement element) {
    return getActionName0(element, true, null);
  }

  public static @Nullable String getActionName0(@Nullable PsiElement element, boolean needCheckArtifact, @Nullable Boolean isGrails1_4) {
    GrMember member;

    if (element instanceof GrAccessorMethod) {
      if (((GrAccessorMethod)element).isSetter()) return null;
      member = ((GrAccessorMethod)element).getProperty();
    }
    else {
      if (!(element instanceof GrMember)) return null;
      member = (GrMember)element;
    }

    if (needCheckArtifact && !GrailsArtifact.CONTROLLER.isInstance(member.getContainingClass())) {
      return null;
    }

    if (member.hasModifierProperty(PsiModifier.STATIC)) {
      return null;
    }

    if (member instanceof GrField field) {
      if (!(field.getInitializerGroovy() instanceof GrClosableBlock)) return null;

      GrModifierList modifierList = field.getModifierList();
      if (modifierList == null || (modifierList.hasExplicitVisibilityModifiers() && !member.hasModifierProperty(PsiModifier.PUBLIC))) {
        return null;
      }

      String fieldName;

      if (element instanceof GrAccessorMethod) {
        fieldName = GroovyPropertyUtils.getPropertyNameByGetterName(((GrAccessorMethod)element).getName(), false);
      }
      else {
        fieldName = member.getName();
      }

      if (fieldName != null
          && fieldName.endsWith(WebFlowUtils.FLOW_SUFFIX)
          && fieldName.length() > WebFlowUtils.FLOW_SUFFIX.length()
          && WebFlowUtils.isWebFlowEnabled(GrailsStructure.getInstance(member))) {
        return fieldName.substring(0, fieldName.length() - WebFlowUtils.FLOW_SUFFIX.length());
      }

      return fieldName;
    }

    if (member instanceof GrMethod) {
      if (isGrails1_4 == null) {
        GrailsStructure structure = GrailsStructure.getInstance(member);
        isGrails1_4 = (structure == null || structure.isAtLeastGrails1_4());
      }

      if (!isGrails1_4) return null; // Before Grails 1.4 action could not be defined via methods.

      if (!member.hasModifierProperty(PsiModifier.PUBLIC)) return null;

      return member.getName();
    }

    return null;
  }

  /**
   * @return Returns a appropriate GSP directory or null if controllerClass is not a controller class or appropriate GSP directory does not exists.
   */
  public static @Nullable VirtualFile getControllerGspDir(@NotNull PsiClass controllerClass) {
    VirtualFile grailsApp = GrailsArtifact.CONTROLLER.getGrailsApp(controllerClass);
    if (grailsApp == null) return null;

    VirtualFile viewDirectory = grailsApp.findChild(VIEWS_DIRECTORY);

    if (viewDirectory == null) return null;

    String categoryName = GrailsArtifact.CONTROLLER.getArtifactName(controllerClass);

    return viewDirectory.findChild(categoryName);
  }

  /**
   * @return Returns a appropriate GSP/JSP/Gson file or null if controllerClass is not a controller class or appropriate GSP/JSP does not exists.
   */
  public static @NotNull List<VirtualFile> getViewsByAction(@NotNull PsiElement action) {
    String name = getActionName(action);
    if (name == null) return ContainerUtil.emptyList();

    PsiClass aClass = ((GrMember)action).getContainingClass();
    assert aClass != null;

    return getViewsByAction(aClass, name);
  }

  /**
   * @return Returns a appropriate GSP/JSP/Gson file or null if controllerClass is not a controller class or appropriate GSP/JSP does not exists.
   */
  public static @NotNull List<PsiFile> getViewPsiByAction(@NotNull PsiElement action) {
    List<VirtualFile> files = getViewsByAction(action);
    return ContainerUtil.mapNotNull(files, file -> action.getManager().findFile(file));
  }

  /**
   * @return Returns a appropriate GSP/JSP/Gson file or null if controllerClass is not a controller class or appropriate GSP/JSP does not exists.
   */
  public static @NotNull List<VirtualFile> getViewsByAction(@NotNull PsiClass controllerClass, String actionName) {
    VirtualFile controllerViewFolder = getControllerGspDir(controllerClass);
    if (controllerViewFolder == null) return ContainerUtil.emptyList();

    List<VirtualFile> result = new SmartList<>();
    ContainerUtil.addIfNotNull(result, controllerViewFolder.findChild(actionName + ".gsp"));
    ContainerUtil.addIfNotNull(result, controllerViewFolder.findChild(actionName + ".gson"));
    ContainerUtil.addIfNotNull(result, controllerViewFolder.findChild(actionName + ".jsp"));
    return result;
  }

  public static boolean isBootStrapClass(@NotNull PsiClass aClass) {
    String className = aClass.getName();
    if (className == null || !className.endsWith("BootStrap")) return false;

    PsiFile psiFile = aClass.getContainingFile();
    if (psiFile == null) return false;

    VirtualFile vfile = psiFile.getOriginalFile().getVirtualFile();
    if (vfile == null) return false;

    VirtualFile confDir = vfile.getParent();
    if (confDir == null || !confDir.getName().equals(CONF_DIRECTORY)) return false;

    VirtualFile grailsApp = confDir.getParent();
    if (grailsApp == null || !grailsApp.getName().equals(GRAILS_APP_DIRECTORY)) return false;

    return true;
  }

  public static @Nullable TldDescriptor getTldDescriptor(@Nullable XmlFile xmlFile) {
    if (xmlFile == null) return null;

    final XmlDocument document = xmlFile.getDocument();
    if (document == null) return null;

    final PsiMetaData metaData = document.getMetaData();
    if (!(metaData instanceof TldDescriptor)) return null;

    return (TldDescriptor)metaData;
  }

  /**
   * @return crc of plugin dependencies files or {@code 0} if there are no plugin "dependencies.groovy" files.
   */
  public static int getPluginDependenciesCrc(@NotNull Module module) {
    List<Integer> crcList = new ArrayList<>();
    CRC32 crc = new CRC32();

    for (VirtualFile pluginRoot : GrailsFramework.getInstance().getCommonPluginRoots(module, false)) {
      VirtualFile dependenciesFile = pluginRoot.findChild("dependencies.groovy");
      if (dependenciesFile != null) {
        try {
          String fileText = VfsUtilCore.loadText(dependenciesFile).trim();
          if (!fileText.isEmpty()) {
            crc.reset();
            crc.update(fileText.getBytes(StandardCharsets.UTF_8));
            crcList.add((int)crc.getValue());
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }

    if (crcList.isEmpty()) return 0; // Don't return crcList.hashCode(), because it is 1 (not 0)

    Collections.sort(crcList);

    return crcList.hashCode();
  }


  /**
   * @return crc of grails plugin dependencies of the module or {@code 0} if there are no such module dependencies.
   */
  public static int getPluginsCrc(@NotNull Module module) {
    List<Integer> crcList = new ArrayList<>();
    CRC32 crc = new CRC32();

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    for (OrderEntry entry : moduleRootManager.getOrderEntries()) {
      if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
        for (VirtualFile root : libraryOrderEntry.getRootFiles(OrderRootType.CLASSES)) {
          final VirtualFile dependenciesFile = ContainerUtil.find(root.getChildren(), file -> !file.isDirectory()
                                                                                          && StringUtil.equals(file.getExtension(), "groovy")
                                                                                          && file.getName().endsWith("GrailsPlugin.groovy"));
          if (dependenciesFile != null) {
            try {
              String fileText = VfsUtilCore.loadText(dependenciesFile).trim();
              if (!fileText.isEmpty()) {
                crc.reset();
                crc.update(fileText.getBytes(StandardCharsets.UTF_8));
                crcList.add((int)crc.getValue());
              }
            }
            catch (IOException e) {
              LOG.error(e);
            }
          }
        }
      }
    }

    if (crcList.isEmpty()) return 0; // Don't return crcList.hashCode(), because it is 1 (not 0)
    Collections.sort(crcList);
    return crcList.hashCode();
  }

  public static @Nullable GrClosableBlock getClosureArgument(@NotNull GrMethodCall callExpression) {
    GrArgumentList argumentList = callExpression.getArgumentList();
    PsiElement[] allArguments = argumentList.getAllArguments();
    if (allArguments.length == 0) {
      GrClosableBlock[] closureArguments = callExpression.getClosureArguments();
      if (closureArguments.length == 1) return closureArguments[0];
    }
    else {
      if (allArguments.length == 1 && allArguments[0] instanceof GrClosableBlock) {
        return (GrClosableBlock)allArguments[0];
      }
    }

    return null;
  }

  public static boolean isGrailsPluginClass(@Nullable PsiClass aClass) {
    if (aClass == null) return false;

    String className = aClass.getName();
    if (className == null || !className.endsWith("GrailsPlugin")) return false;

    String qname = aClass.getQualifiedName();
    if (qname == null || (qname.contains(".") && !qname.startsWith("org.codehaus.groovy.grails.plugins."))) return false;

    return true;
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> createMap(Object ... data) {
    int length = data.length;
    assert (length & 1) == 0;

    if (length == 0) return Collections.emptyMap();
    if (length == 2) return Collections.singletonMap((K)data[0], (V)data[1]);

    Map res = new HashMap();

    for (int i = 0; i < length; i += 2) {
      res.put(data[i], data[i+1]);
    }

    return res;
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> createEnumMap(Object ... data) {
    int length = data.length;
    assert (length & 1) == 0;

    if (length == 0) return Collections.emptyMap();
    if (length == 2) return Collections.singletonMap((K)data[0], (V)data[1]);

    Map res = new EnumMap(data[0].getClass());

    for (int i = 0; i < length; i += 2) {
      res.put(data[i], data[i+1]);
    }

    return res;
  }

  public static boolean isValidatedClass(@NotNull PsiClass aClass) {
    return CachedValuesManager.getCachedValue(aClass, () -> {
      boolean value = isCommandClass(aClass)
                  || InheritanceUtil.isInheritor(aClass, "grails.validation.Validateable")
                  || TypesUtil.isAnnotatedCheckHierarchyWithCache(aClass, "org.codehaus.groovy.grails.validation.Validateable")
                  || TypesUtil.isAnnotatedCheckHierarchyWithCache(aClass, "grails.validation.Validateable");
      return CachedValueProvider.Result.create(value, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public static boolean isCommandClass(@NotNull PsiClass aClass) {
    String name = aClass.getName();
    if (name == null || !name.endsWith("Command")) return false;

    PsiFile file = aClass.getContainingFile().getOriginalFile();
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return false;

    VirtualFile sourceRoot = ProjectRootManager.getInstance(aClass.getProject()).getFileIndex().getSourceRootForFile(virtualFile);
    if (sourceRoot == null || !sourceRoot.getName().equals("controllers")) return false;

    VirtualFile appRoot = sourceRoot.getParent();
    if (appRoot == null || !appRoot.getName().equals(GRAILS_APP_DIRECTORY)) return false;

    return true;
  }

  public static @Nullable GrailsArtifact calculateArtifactByAnnotation(@NotNull PsiClass aClass) {
    PsiModifierList modifierList = aClass.getModifierList();
    if (modifierList != null) {
      PsiAnnotation annotation = modifierList.findAnnotation("grails.artefact.Artefact");
      if (annotation != null) {
        final String s = GrAnnotationUtil.inferStringAttribute(annotation, "value");
        if ("TagLibrary".equals(s)) {
          return GrailsArtifact.TAGLIB;
        }
        else if ("Controller".equals(s)) {
          return GrailsArtifact.CONTROLLER;
        }
        else if ("Domain".equals(s)) {
          return GrailsArtifact.DOMAIN;
        }
      }
    }

    return null;
  }

  public static @Nullable GrailsArtifact calculateArtifactType(@NotNull PsiClass aClass) {
    GrailsArtifact res = GrailsArtifact.getType(aClass);
    if (res != null) return res;

    if (GormUtils.isStandaloneGormBean(aClass)) {
      return GrailsArtifact.DOMAIN;
    }

    return calculateArtifactByAnnotation(aClass);
  }

  public static @Nullable File findLatestJarInIvyRepository(String artifactFolderPath, String jarPrefix) {
    File jarDirectory = new File(artifactFolderPath, "jars");
    if (!jarDirectory.exists()) {
      File artifactFolderFile = new File(artifactFolderPath);
      String[] versions = artifactFolderFile.list();
      if (versions == null || versions.length == 0) return null;

      Arrays.sort(versions);

      jarDirectory = new File(artifactFolderFile, versions[versions.length - 1] + "/jar");
    }

    File[] files = jarDirectory.listFiles();
    if (files == null) return null;

    File res = null;
    for (File file : files) {
      String name = file.getName();
      if (!name.startsWith(jarPrefix) || !name.endsWith(".jar")) continue;

      if (res == null || name.compareTo(res.getName()) > 0) {
        res = file;
      }
    }

    return res;
  }

  public static void addSystemPropertyIfNotExists(ParametersList parametersList, @NotNull String property) {
    int idx = property.indexOf('=');
    assert idx > 0;

    String leftPart = property.substring(0, idx + 1);

    for (String param : parametersList.getParameters()) {
      if (param != null && param.startsWith(leftPart)) {
        return;
      }
    }

    parametersList.add(property);
  }

  private static boolean isAlreadyInsideEnvironmentDefinition(GrReferenceExpression ref) {
    PsiElement parent = ref.getParent();
    if (parent instanceof GrMethodCall) {
      parent = parent.getParent();
    }

    for (GrMethodCall call = PsiTreeUtil.getParentOfType(parent, GrMethodCall.class);
         call != null;
         call = PsiTreeUtil.getParentOfType(call, GrMethodCall.class)) {

      String methodName = PsiUtil.getUnqualifiedMethodName(call);
      if (ENVIRONMENTS.equals(methodName)) {
        GrExpression[] arguments = PsiUtil.getAllArguments(call);
        if (arguments.length == 1 && arguments[0] instanceof GrClosableBlock) {
          return true; // We are already in environments block.
        }
      }

    }

    return false;
  }

  public static boolean processEnvironmentDefinition(PsiScopeProcessor processor, GrReferenceExpression ref) {
    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null || nameHint.equals(ENVIRONMENTS)) {

      ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);
      if (!ResolveUtil.shouldProcessMethods(classHint)) return true;

      if (ref.isQualified()) return true;
      if (isAlreadyInsideEnvironmentDefinition(ref)) return true;

      GrLightMethodBuilder envMethod = new GrLightMethodBuilder(ref.getManager(), ENVIRONMENTS);
      envMethod.setMethodKind(ENVIRONMENTS_METHOD_KIND);
      envMethod.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);

      return processor.execute(envMethod, ResolveState.initial());
    }

    return true;
  }

  public static @Nullable PsiClass findApplicationClass(final Module module) {
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, new CachedValueProvider<>() {

                                                                    private final PsiShortNamesCache myNamesCache = PsiShortNamesCache.getInstance(project);

                                                                    @Override
                                                                    public @Nullable Result<PsiClass> compute() {
                                                                      return Result.create(
                                                                        doCompute(),
                                                                        ModuleManager.getInstance(project),
                                                                        PsiModificationTracker.MODIFICATION_COUNT
                                                                      );
                                                                    }

                                                                    private PsiClass doCompute() {
                                                                      final PsiClass[] classes = myNamesCache.getClassesByName("Application", module.getModuleScope());
                                                                      for (PsiClass clazz : classes) {
                                                                        if (!PsiClassUtil.isRunnableClass(clazz, true)) continue;
                                                                        if (!InheritanceUtil.isInheritor(clazz, "grails.boot.config.GrailsAutoConfiguration")) continue;
                                                                        return clazz;
                                                                      }
                                                                      return null;
                                                                    }
                                                                  }
    );
  }
}
