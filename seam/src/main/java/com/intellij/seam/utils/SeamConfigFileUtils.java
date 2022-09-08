package com.intellij.seam.utils;

import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.psi.jsp.WebDirectoryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamModel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SeamConfigFileUtils {


  @NonNls public static final String CONFIG_DEFAULT_DIRECTORY = "WEB-INF";
  @NonNls public static final String CONFIG_RESOURCES_PATH = "META-INF/components.xml";
  @NonNls public static final String CONFIG_DEFAULT_PATH = "WEB-INF/components.xml";

  private SeamConfigFileUtils() {
  }

  public static Set<XmlFile> getConfigurationFiles(Module module) {
    Set<XmlFile> componentsFiles = new HashSet<>();

    // add files from WEB-INF
    componentsFiles.addAll(getConfigsFromWebInf(module));

    // add files from META-INF
    componentsFiles.addAll(getConfigsFromMetaInf(module));

    // add files from resources (http://docs.jboss.com/seam/1.1GA/reference/en/html/xml.html part 4.2)
    componentsFiles.addAll(getConfigsFromResources(module));


    return componentsFiles;
  }

  public static List<XmlFile> getConfigsFromWebInf(final Module module) {
    List<XmlFile> files = new ArrayList<>();
    final Collection<WebFacet> webFacet = WebFacet.getInstances(module);
    for (WebFacet facet : webFacet) {
      XmlFile xmlFile = getConfigsFromWebInf(facet);
      if (xmlFile != null) {
        files.add(xmlFile);
      }
    }
    return files;
  }

  @Nullable
  public static XmlFile getConfigsFromWebInf(final WebFacet webFacet) {
    return getWebDirectoryElementConfig(webFacet, CONFIG_DEFAULT_PATH);
  }

  @Nullable
  private static XmlFile getWebDirectoryElementConfig(final @NotNull WebFacet webFacet, String relativePath) {
    Module module = webFacet.getModule();

    final WebDirectoryElement element =
      WebDirectoryUtil.getWebDirectoryUtil(module.getProject()).findWebDirectoryElementByPath(relativePath, webFacet);
    if (element != null) {
      final PsiFile psiFile = element.getOriginalFile();
      if (psiFile instanceof XmlFile) return (XmlFile)psiFile;
    }

    return null;
  }

  @NotNull
  public static List<XmlFile> getConfigsFromMetaInf(final Module module) {
    final PsiManager psiManager = PsiManager.getInstance(module.getProject());

    List<XmlFile> resourceFiles = new ArrayList<>();
    for (VirtualFile file : OrderEnumerator.orderEntries(module).getAllLibrariesAndSdkClassesRoots()) {
      final VirtualFile candidate = file.findFileByRelativePath(CONFIG_RESOURCES_PATH);
      if (candidate != null) {
        final PsiFile psiFile = psiManager.findFile(candidate);
        if (psiFile instanceof XmlFile) {
          resourceFiles.add((XmlFile)psiFile);
        }
      }
    }
    return resourceFiles;
  }

  @NotNull
  // add files from resources (http://docs.jboss.com/seam/1.1GA/reference/en/html/xml.html part 4.2)
  public static List<XmlFile> getConfigsFromResources(final Module module) {
    List<XmlFile> resourceFiles = new ArrayList<>();
    Set<VirtualFile> directories = new HashSet<>();

    for (SeamJamComponent seamJamComponent : SeamJamModel.getModel(module).getSeamComponents(false)) {
      PsiFile containingFile = seamJamComponent.getContainingFile();
      if (containingFile != null) {
        PsiDirectory psiDirectory = containingFile.getParent();
        if (psiDirectory != null) {
          directories.add(psiDirectory.getVirtualFile());
        }
      }
    }

    final PsiManager psiManager = PsiManager.getInstance(module.getProject());
    for (VirtualFile directory : directories) {
      VirtualFile file = directory.findChild(SeamConstants.SEAM_CONFIG_FILENAME);
      if (file != null) {
        PsiFile psiFile = psiManager.findFile(file);
        if (psiFile instanceof XmlFile) {
          resourceFiles.add((XmlFile)psiFile);
        }
      }
    }

    return resourceFiles;
  }
}
