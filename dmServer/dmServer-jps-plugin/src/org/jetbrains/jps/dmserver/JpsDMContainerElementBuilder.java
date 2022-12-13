package org.jetbrains.jps.dmserver;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jps.dmserver.model.JpsDMBundleArtifactType;
import org.jetbrains.jps.dmserver.model.JpsDMContainerPackagingElement;
import org.jetbrains.jps.incremental.artifacts.builders.LayoutElementBuilderService;
import org.jetbrains.jps.incremental.artifacts.instructions.ArtifactCompilerInstructionCreator;
import org.jetbrains.jps.incremental.artifacts.instructions.ArtifactInstructionsBuilderContext;
import org.jetbrains.jps.javaee.model.JpsJavaeeExtensionService;
import org.jetbrains.jps.javaee.model.web.JpsWebModuleExtension;
import org.jetbrains.jps.javaee.model.web.JpsWebRoot;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.osgi.jps.model.JpsOsmorcExtensionService;
import org.jetbrains.osgi.jps.model.JpsOsmorcModuleExtension;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.List;

/**
 * @author michael.golubev
 */
public class JpsDMContainerElementBuilder extends LayoutElementBuilderService<JpsDMContainerPackagingElement> {

  private static final Logger LOG = Logger.getInstance(JpsDMContainerElementBuilder.class);

  @NonNls
  private static final String META_INF = "META-INF";

  public JpsDMContainerElementBuilder() {
    super(JpsDMContainerPackagingElement.class);
  }

  @Override
  public void generateInstructions(JpsDMContainerPackagingElement element,
                                   ArtifactCompilerInstructionCreator instructionCreator,
                                   ArtifactInstructionsBuilderContext builderContext) {
    JpsModule module = element.getModuleReference().resolve();
    if (module == null) {
      LOG.warn("module not found");
      return;
    }

    JpsOsmorcModuleExtension osmorcExtension = JpsOsmorcExtensionService.getExtension(module);
    if (osmorcExtension == null) {
      LOG.warn("osmorc facet not found");
      return;
    }

    String jarFileLocation = osmorcExtension.getJarFileLocation();

    if (jarFileLocation.isEmpty()) {
      LOG.warn("jar location is empty");
      return;
    }

    File jarFile = new File(jarFileLocation);
    if (!jarFile.exists()) {
      LOG.warn("jar file does not exist");
      return;
    }

    List<JpsWebModuleExtension> webExtensions = JpsJavaeeExtensionService.getInstance().getWebExtensions(module);

    String jarFileName = PathUtilRt.getFileName(jarFileLocation);

    if (webExtensions.isEmpty()) {
      instructionCreator.addFileCopyInstruction(jarFile, jarFileName);
    }
    else {
      ArtifactCompilerInstructionCreator warCreator =
        instructionCreator.archive(FileUtilRt.getNameWithoutExtension(jarFileName) + "." + JpsDMBundleArtifactType.WAR_EXTENSION);

      ArtifactCompilerInstructionCreator webInfCreator = warCreator.subFolder("WEB-INF");

      webInfCreator.subFolder("classes").addExtractDirectoryInstruction(jarFile, "/", path -> !(META_INF + "/MANIFEST.MF").equals(StringUtil.trimStart(path, "/")));

      warCreator.subFolder(META_INF).addExtractDirectoryInstruction(jarFile, "/" + META_INF + "/");

      JpsWebModuleExtension webExtension = webExtensions.get(0);

      for (JpsWebRoot webRoot : webExtension.getWebRoots()) {
        warCreator.subFolderByRelativePath(webRoot.getRelativePath()).addDirectoryCopyInstructions(JpsPathUtil.urlToFile(webRoot.getUrl()));
      }
    }

    LOG.info("DM container #generateInstructions finished");
  }
}
