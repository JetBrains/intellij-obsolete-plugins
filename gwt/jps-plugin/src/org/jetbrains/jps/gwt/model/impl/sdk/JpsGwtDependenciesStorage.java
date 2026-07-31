package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.storage.BuildDataPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores dependencies of GWT artifacts (gwt-dev and gwt-user) which are required to compile the project and start Dev Mode configurations.
 * Since user's projects sometimes don't declare dependencies on these artifacts explicitly and sometimes users exclude some transitive dependencies
 * of these artifacts, we need to store actual dependencies during importing project from Maven, and then use them inside JPS build process
 * and to start GWT Run configuration.
 */
public class JpsGwtDependenciesStorage {
  private static final Logger LOG = Logger.getInstance(JpsGwtDependenciesStorage.class);

  public static final String GWT_DEPENDENCIES_CACHE_FILE_NAME = "gwt-dependencies.xml";

  protected final Path gwtDependenciesFile;
  protected final GwtDependencies myDependencies;

  public JpsGwtDependenciesStorage(@NotNull BuildDataPaths buildDataPaths) {
    this(buildDataPaths.getDataStorageDir().resolve(GWT_DEPENDENCIES_CACHE_FILE_NAME));
  }

  protected JpsGwtDependenciesStorage(@NotNull Path gwtDependenciesFile) {
    this.gwtDependenciesFile = gwtDependenciesFile;
    GwtDependencies dependencies = new GwtDependencies();
    if (Files.exists(gwtDependenciesFile)) {
      try {
        dependencies = deserializeManually(JDOMUtil.load(gwtDependenciesFile));
      }
      catch (JDOMException | IOException e) {
        LOG.error("Unable to load file " + gwtDependenciesFile, e);
      }
    }
    myDependencies = dependencies;
  }

  private static @NotNull GwtDependencies deserializeManually(@NotNull Element gwtDependenciesRoot) {
    // XmlSerializer seems cannot load `<files><list>` content

    GwtDependencies result = new GwtDependencies();

    Element dependenciesChild = gwtDependenciesRoot.getChild("dependencies");
    if (dependenciesChild != null) {
      for (Element filesItem : dependenciesChild.getChildren("files")) {
        String mavenId = filesItem.getAttributeValue("mavenId");
        if (mavenId != null) {
          List<String> paths = new ArrayList<>();
          Element listChild = filesItem.getChild("list");
          if (listChild != null) {
            for (Element optionItem : listChild.getChildren("option")) {
              String value = optionItem.getAttributeValue("value");
              if (value != null && !value.isBlank()) {
                paths.add(value);
              }
            }
          }

          result.myDependenciesMap.put(mavenId, paths);
        }
      }
    }

    return result;
  }

  public @NotNull List<String> getDependenciesPaths(@NotNull String groupId,
                                                    @NotNull String artifactId,
                                                    @NotNull String version) throws GwtDependenciesNotFoundException {
    String coordinates = groupId + ":" + artifactId + ":" + version;
    if (myDependencies == null) {
      throw new GwtDependenciesNotFoundException(coordinates);
    }
    List<String> paths = myDependencies.myDependenciesMap.get(coordinates);
    if (paths == null) {
      throw new GwtDependenciesNotFoundException(coordinates);
    }
    return paths;
  }

  public void storeDependenciesPaths(@NotNull String groupId,
                                     @NotNull String artifactId,
                                     @NotNull String version,
                                     @NotNull List<String> dependencies) {
    String coordinates = groupId + ":" + artifactId + ":" + version;
    myDependencies.myDependenciesMap.put(coordinates, dependencies);

    Element element = XmlSerializer.serialize(myDependencies);
    try {
      JDOMUtil.write(element, gwtDependenciesFile);
    }
    catch (IOException e) {
      LOG.error("Unable to save file " + gwtDependenciesFile, e);
    }
  }

  public static class GwtDependenciesNotFoundException extends Exception {
    public GwtDependenciesNotFoundException(String coordinates) {
      super(String.format("Dependencies of %s artifact weren't computed accurately so classpath of GWT application may be incorrect. "
                          + "Reimport the project from Maven to actualize dependencies of GWT artifacts.", coordinates));
    }
  }

  @Tag("maven-dependencies")
  public static class GwtDependencies {

    @Tag("dependencies")
    @MapAnnotation(keyAttributeName = "mavenId",
      entryTagName = "files",
      surroundKeyWithTag = false,
      surroundValueWithTag = false,
      surroundWithTag = false)
    public Map<String, List<String>> myDependenciesMap = new HashMap<>();
  }
}
