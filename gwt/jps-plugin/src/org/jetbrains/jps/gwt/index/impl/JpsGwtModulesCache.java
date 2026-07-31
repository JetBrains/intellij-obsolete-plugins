package org.jetbrains.jps.gwt.index.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gwt.build.GwtBuildTargetType;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JpsGwtModulesCache {
  private static final Logger LOG = Logger.getInstance(JpsGwtModulesCache.class);
  private final Path cacheFile;

  public JpsGwtModulesCache(@NotNull BuildDataPaths buildDataPaths) {
    cacheFile = buildDataPaths.getTargetTypeDataRootDir(GwtBuildTargetType.INSTANCE).resolve("gwt-modules.xml");
  }

  public JpsGwtModulesCache(@NotNull Path projectSystemDirectory) {
    this(new BuildDataPathsImpl(projectSystemDirectory));
  }

  public void saveGwtModules(@NotNull GwtModulesConfiguration configuration) {
    try {
      Element rootElement = XmlSerializer.serialize(configuration);
      JDOMUtil.write(rootElement, cacheFile, "\n");
    }
    catch (IOException e) {
      LOG.info("Cannot save cache: " + e.getMessage(), e);
      clear();
    }
  }

  public @NotNull List<JpsGwtModule> loadGwtModules(@NotNull JpsModel model) {
    if (Files.notExists(cacheFile)) {
      LOG.debug("Cache file doesn't exist, scanning source roots to find GWT modules");
      return findGwtModules(model);
    }
    try {
      return loadModulesFromCache(model);
    }
    catch (Exception e) {
      LOG.info("Cannot load cache: " + e.getMessage(), e);
      return findGwtModules(model);
    }
  }

  private @NotNull List<JpsGwtModule> loadModulesFromCache(JpsModel model) throws JDOMException, IOException {
    GwtModulesConfiguration configuration = XmlSerializer.deserialize(JDOMUtil.load(cacheFile), GwtModulesConfiguration.class);

    List<JpsGwtModule> result = new ArrayList<>();
    for (GwtModuleData gwtModuleData : configuration.myGwtModules) {
      if (gwtModuleData.myModuleName != null) {
        JpsModule module = model.getProject().findModuleByName(gwtModuleData.myModuleName);
        if (module != null) {
          result.add(new JpsGwtModuleImpl(Path.of(gwtModuleData.myFilePath), module, new File(gwtModuleData.mySrcRootPath), gwtModuleData.myInTests));
        }
      }
    }
    return result;
  }

  private static List<JpsGwtModule> findGwtModules(JpsModel model) {
    List<JpsGwtModule> result = new ArrayList<>();
    for (JpsModule module : model.getProject().getModules()) {
      JpsGwtModuleExtension extension = JpsGwtExtensionService.getInstance().getExtension(module);
      if (extension != null) {
        for (JpsModuleSourceRoot root : module.getSourceRoots()) {
          collectGwtModules(root, module, root.getRootType().equals(JavaSourceRootType.TEST_SOURCE), result);
        }
      }
    }
    return result;
  }

  private static void collectGwtModules(final JpsModuleSourceRoot sourceRoot, final JpsModule module, final boolean tests,
                                        final List<JpsGwtModule> result) {
    FileUtil.processFilesRecursively(sourceRoot.getFile(), file -> {
      if (file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX)) {
        try {
          result.add(new JpsGwtModuleImpl(file.toPath(), module, sourceRoot.getFile(), tests));
        }
        catch (Exception e) {
          LOG.info("Cannot parse " + file.getAbsolutePath() + " file: " + e.getMessage(), e);
        }
      }
      return true;
    });
  }

  public void clear() {
    try {
      Files.deleteIfExists(cacheFile);
    }
    catch (IOException ignored) {
    }
  }

  @Tag("module")
  public static final class GwtModuleData {
    @Attribute("path")
    public String myFilePath;

    @Attribute("root")
    public String mySrcRootPath;

    @Attribute("inTests")
    public boolean myInTests;

    @Attribute("module-name")
    public String myModuleName;

    public GwtModuleData() {
    }

    public GwtModuleData(String filePath, String srcRootPath, boolean inTests, String moduleName) {
      myFilePath = filePath;
      mySrcRootPath = srcRootPath;
      myInTests = inTests;
      myModuleName = moduleName;
    }
  }

  @Tag("configuration")
  public static final class GwtModulesConfiguration {
    @XCollection(propertyElementName = "gwt-modules", elementName = "module")
    public List<GwtModuleData> myGwtModules = new ArrayList<>();
  }
}
