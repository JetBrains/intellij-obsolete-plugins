package com.intellij.dmserver.editor;

import com.intellij.dmserver.artifacts.ManifestManager;
import com.intellij.dmserver.artifacts.ManifestManagerListener;
import com.intellij.dmserver.editor.wrapper.ClauseWrapper;
import com.intellij.dmserver.editor.wrapper.HeaderWrapper;
import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.libraries.BundleDefinition;
import com.intellij.dmserver.libraries.BundleWrapper;
import com.intellij.dmserver.libraries.LibraryDefinition;
import com.intellij.dmserver.osmorc.DMSourceBundleFinder;
import com.intellij.dmserver.osmorc.FrameworkUtils;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.dmserver.util.PsiTreeChangedAdapter;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetManagerAdapter;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.messages.SimpleMessageBusConnection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osmorc.settings.ProjectSettings;

import java.util.*;

public final class AvailableBundlesProvider implements Disposable {
  private static final Logger LOG = Logger.getInstance(AvailableBundlesProvider.class);

  @NonNls
  private static final String LIBRARY_EXTENSION = "libd";

  @Override
  public void dispose() {
  }

  public static AvailableBundlesProvider getInstance(@NotNull Project project) {
    return project.getComponent(AvailableBundlesProvider.class);
  }

  private final Project myProject;

  private final RepositoryIndex myRepositoryIndex;
  private final ProjectIndex myProjectIndex;

  private final UnitsCollectorBase myPackagesCollector;
  private final UnitsCollectorBase myBundlesCollector;
  private final UnitsCollectorBase myLibrariesCollector;

  private final DMSourceBundleFinder mySourceFinder = new DMSourceBundleFinder();

  public AvailableBundlesProvider(Project project) {
    myProject = project;

    myRepositoryIndex = new RepositoryIndex();
    myProjectIndex = new ProjectIndex();

    myPackagesCollector = new UnitsCollectorBase() {

      @Override
      protected List<ExportedUnit> collectAvailableUnits() {
        List<ExportedUnit> result = new ArrayList<>(myRepositoryIndex.getPackages());
        result.addAll(myProjectIndex.getPackages());
        return result;
      }
    };
    myBundlesCollector = new UnitsCollectorBase() {
      @Override
      protected List<ExportedUnit> collectAvailableUnits() {
        List<ExportedUnit> result = new ArrayList<>(myRepositoryIndex.getBundles());
        result.addAll(myProjectIndex.getBundles());
        return result;
      }
    };
    myLibrariesCollector = new UnitsCollectorBase() {
      @Override
      protected List<ExportedUnit> collectAvailableUnits() {
        return myRepositoryIndex.getLibraries();
      }
    };

    if (!LightEdit.owns(myProject)) {
      StartupManager.getInstance(myProject).runAfterOpened(() -> {
        DumbService.getInstance(myProject).runWhenSmart(() -> {
          onProjectOpened();
        });
      });
    }
  }

  private List<BundleIndex> collectBundleIndexes() {
    List<BundleIndex> result = new ArrayList<>();
    for (Module module : ProjectFacetManager.getInstance(myProject).getModulesWithFacet(DMBundleFacet.ID)) {
      ManifestManager.FileWrapper manifestWrapper = ManifestManager.getBundleInstance().findManifest(module);
      if (manifestWrapper != null) {
        result.add(new BundleIndex(module, manifestWrapper));
      }
    }
    return result;
  }

  public void resetAll() {
    myRepositoryIndex.build();
    myProjectIndex.build();
    resetCollectors();
  }

  public void resetRepositoryIndex() {
    myRepositoryIndex.build();
    resetCollectors();
  }

  private void resetBundleIndex(BundleIndex bundleIndex) {
    myProjectIndex.buildBundle(bundleIndex);
    resetCollectors();
  }

  private void resetCollectors() {
    for (UnitsCollectorBase unitsCollectors : Arrays.asList(myPackagesCollector, myBundlesCollector, myLibrariesCollector)) {
      unitsCollectors.reset();
    }
  }

  public UnitsCollector getBundlesCollector() {
    return myBundlesCollector;
  }

  public UnitsCollector getLibrariesCollector() {
    return myLibrariesCollector;
  }

  public UnitsCollector getPackagesCollector() {
    return myPackagesCollector;
  }

  public List<LibraryDefinition> getRepositoryLibraries(RepositoryPattern repositoryPattern) {
    return myRepositoryIndex.getFolderLibraries(repositoryPattern);
  }

  public List<BundleWrapper> getRepositoryBundles(RepositoryPattern repositoryPattern) {
    return myRepositoryIndex.getFolderBundles(repositoryPattern);
  }

  public List<LibraryDefinition> getAllRepositoryLibraries() {
    return myRepositoryIndex.getAllLibraries();
  }

  public List<BundleWrapper> getAllRepositoryBundles() {
    return myRepositoryIndex.getAllBundles();
  }

  public static List<ExportedUnit> getExportedPackages(PsiFile manifestFile) {
    List<ExportedUnit> result = new ArrayList<>();
    HeaderWrapper exportHeaderWrapper = new HeaderWrapper(manifestFile, Constants.EXPORT_PACKAGE);
    for (ClauseWrapper clause : exportHeaderWrapper.getClauses()) {
      result.add(new ExportedUnitImpl(clause.getName(), clause.getAttributeValue(ManifestUtils.VERSION_ATTRIBUTE_NAME)));
    }
    return result;
  }

  private void onProjectOpened() {
    resetAll();

    PsiManager.getInstance(myProject).addPsiTreeChangeListener(new PsiTreeChangedAdapter() {
      @Override
      protected void treeChanged(PsiTreeChangeEvent event) {
        PsiFile changedFile = event.getFile();
        if (changedFile == null) {
          return;
        }
        BundleIndex bundleIndex = findBundleIndex(changedFile);
        if (bundleIndex == null) {
          return;
        }
        resetBundleIndex(bundleIndex);
      }

      private @Nullable AvailableBundlesProvider.BundleIndex findBundleIndex(PsiFileSystemItem changedFile) {
        for (BundleIndex bundleIndex : collectBundleIndexes()) {
          if (Comparing.equal(bundleIndex.getManifestWrapper().getFile().getVirtualFile(), changedFile.getVirtualFile())) {
            return bundleIndex;
          }
        }
        return null;
      }
    }, this);

    ProjectSettings.getInstance(myProject).addProjectSettingsListener(new ProjectSettings.ProjectSettingsListener() {
      @Override
      public void projectSettingsChanged() {
        resetRepositoryIndex();
      }
    }, this);

    SimpleMessageBusConnection connection = myProject.getMessageBus().simpleConnect();
    connection.subscribe(FacetManager.FACETS_TOPIC, new FacetManagerAdapter() {
      @Override
      public void facetRemoved(@NotNull Facet facet) {
        if (!(facet instanceof DMBundleFacet)) {
          return;
        }
        myProjectIndex.removeBundle(facet.getModule());
        resetCollectors();
      }
    });
    ManifestManager.getBundleInstance().addListener(new ManifestManagerListener() {
      @Override
      public void manifestCreated(Module module, ManifestManager.FileWrapper manifest) {
        if (module.getProject() != myProject) {
          return;
        }
        resetBundleIndex(new BundleIndex(module, manifest));
      }
    }, this);
  }

  private abstract static class UnitsCollectorBase implements UnitsCollector {

    private List<ExportedUnit> myAvailableUnits;

    private Set<String> myAvailableUnitNames;

    public void reset() {
      myAvailableUnits = collectAvailableUnits();
      myAvailableUnitNames = new HashSet<>();
      for (ExportedUnit availableUnit : myAvailableUnits) {
        myAvailableUnitNames.add(availableUnit.getSymbolicName());
      }
    }

    @Override
    public List<ExportedUnit> getAvailableUnits() {
      return myAvailableUnits;
    }

    @Override
    public boolean isUnitAvailable(String symbolicName) {
      return myAvailableUnitNames.contains(symbolicName);
    }

    protected abstract List<ExportedUnit> collectAvailableUnits();
  }

  private class RepositoryIndex {

    private LibrariesCollector myLibrariesCollector;
    private BundlesCollector myBundlesCollector;

    private List<ExportedUnit> myLibraries;
    private List<ExportedUnit> myBundles;
    private List<ExportedUnit> myPackages;

    public void build() {
      myLibrariesCollector = new LibrariesCollector();
      myLibrariesCollector.collect();
      myBundlesCollector = new BundlesCollector();
      myBundlesCollector.collect();
      myLibraries = collectLibraries(myLibrariesCollector.getAllUnits());
      List<BundleWrapper> bundleWrappers = myBundlesCollector.getAllUnits();
      myBundles = collectBundles(bundleWrappers);
      myPackages = collectPackages(bundleWrappers);
    }

    public List<ExportedUnit> getLibraries() {
      return myLibraries;
    }

    public List<ExportedUnit> getBundles() {
      return myBundles;
    }

    public List<ExportedUnit> getPackages() {
      return myPackages;
    }

    public List<LibraryDefinition> getFolderLibraries(RepositoryPattern repositoryPattern) {
      return myLibrariesCollector.getFolderUnits(repositoryPattern);
    }

    public List<BundleWrapper> getFolderBundles(RepositoryPattern repositoryPattern) {
      return myBundlesCollector.getFolderUnits(repositoryPattern);
    }

    public List<LibraryDefinition> getAllLibraries() {
      return myLibrariesCollector.getAllUnits();
    }

    public List<BundleWrapper> getAllBundles() {
      return myBundlesCollector.getAllUnits();
    }

    private List<ExportedUnit> collectPackages(List<BundleWrapper> bundleWrappers) {
      List<ExportedUnit> result = new ArrayList<>();
      for (BundleWrapper bundleWrapper : bundleWrappers) {
        PsiFile manifestFile = PsiManager.getInstance(myProject).findFile(bundleWrapper.getManifestFile());
        if (manifestFile == null) {
          LOG.error("Manifest PSI file is expected to exist");
          continue;
        }
        result.addAll(getExportedPackages(manifestFile));
      }
      return result;
    }

    private List<ExportedUnit> collectLibraries(List<LibraryDefinition> libraryDefinitions) {
      List<ExportedUnit> result = new ArrayList<>();
      for (LibraryDefinition libraryDefinition : libraryDefinitions) {
        result.add(new ExportedUnitImpl(libraryDefinition.getSymbolicName(), libraryDefinition.getVersion()));
      }
      return result;
    }

    private List<ExportedUnit> collectBundles(List<BundleWrapper> bundleWrappers) {
      List<ExportedUnit> result = new ArrayList<>();
      for (BundleDefinition bundleDef : bundleWrappers) {
        result.add(new ExportedUnitImpl(bundleDef.getSymbolicName(), bundleDef.getVersion()));
      }
      return result;
    }

    private @Nullable DMServerInstallation getInstallation() {
      return FrameworkUtils.getInstance().getActiveDMServerInstallation(myProject);
    }

    private abstract class RepositoryUnitsCollector<U> {

      private Map<String, List<U>> myFolder2Units;
      private List<U> myAllUnits;

      public void collect() {
        DMServerInstallation installation = getInstallation();
        if (installation == null) {
          myFolder2Units = Collections.emptyMap();
          myAllUnits = Collections.emptyList();
          return;
        }

        myFolder2Units = new HashMap<>();
        myAllUnits = new ArrayList<>();

        for (RepositoryPattern repositoryPattern : installation.collectRepositoryPatterns()) {
          List<U> folderUnits = collectUnits(repositoryPattern.collectFiles());
          myFolder2Units.put(repositoryPattern.getFullPattern(), folderUnits);
          myAllUnits.addAll(folderUnits);
        }
      }

      public List<U> getAllUnits() {
        return myAllUnits;
      }

      public List<U> getFolderUnits(RepositoryPattern repositoryPattern) {
        List<U> folderUnits = myFolder2Units.get(repositoryPattern.getFullPattern());
        return new ArrayList<>(folderUnits == null ? Collections.emptyList() : folderUnits);
      }

      protected abstract List<U> collectUnits(List<VirtualFile> files);
    }

    private class LibrariesCollector extends RepositoryUnitsCollector<LibraryDefinition> {

      @Override
      protected List<LibraryDefinition> collectUnits(List<VirtualFile> files) {
        List<LibraryDefinition> result = new ArrayList<>();
        for (VirtualFile file : files) {
          if (!LIBRARY_EXTENSION.equalsIgnoreCase(file.getExtension())) {
            continue;
          }
          LibraryDefinition libraryDefinition = LibraryDefinition.load(myProject, file);
          if (libraryDefinition != null) {
            result.add(libraryDefinition);
          }
        }
        return result;
      }
    }

    private class BundlesCollector extends RepositoryUnitsCollector<BundleWrapper> {

      @Override
      protected List<BundleWrapper> collectUnits(List<VirtualFile> files) {
        List<BundleWrapper> result = new ArrayList<>();
        for (VirtualFile file : files) {
          BundleWrapper bundle = BundleWrapper.load(file);
          if (bundle != null && !mySourceFinder.containsOnlySources(file)) {
            result.add(bundle);
          }
        }
        return result;
      }
    }
  }

  private class ProjectIndex {

    private List<ExportedUnit> myBundles;
    private List<ExportedUnit> myPackages;

    private Map<Module, BundleIndex> myModule2BundleIndex;

    public List<ExportedUnit> getBundles() {
      return myBundles;
    }

    public List<ExportedUnit> getPackages() {
      return myPackages;
    }

    public void build() {
      myModule2BundleIndex = new HashMap<>();
      for (BundleIndex bundleIndex : collectBundleIndexes()) {
        doBuildBundle(bundleIndex);
      }
      reset();
    }

    private void reset() {
      myBundles = new ArrayList<>();
      myPackages = new ArrayList<>();
      for (BundleIndex bundleIndex : myModule2BundleIndex.values()) {
        myBundles.add(bundleIndex.getBundle());
        myPackages.addAll(bundleIndex.getPackages());
      }
    }

    public void buildBundle(BundleIndex bundleIndex) {
      doBuildBundle(bundleIndex);
      reset();
    }

    private void doBuildBundle(BundleIndex bundleIndex) {
      bundleIndex.build();
      myModule2BundleIndex.put(bundleIndex.getModule(), bundleIndex);
    }

    public void removeBundle(Module module) {
      myModule2BundleIndex.remove(module);
      reset();
    }
  }

  private final static class BundleIndex {
    private final Module myModule;

    private final ManifestManager.FileWrapper myManifestWrapper;

    private ExportedUnit myBundle;

    private List<ExportedUnit> myPackages;

    BundleIndex(Module module, ManifestManager.FileWrapper manifestWrapper) {
      myModule = module;
      myManifestWrapper = manifestWrapper;
    }

    public Module getModule() {
      return myModule;
    }

    public ManifestManager.FileWrapper getManifestWrapper() {
      return myManifestWrapper;
    }

    public void build() {
      myBundle = new ExportedUnitImpl(myManifestWrapper.getSymbolicName(), myManifestWrapper.getVersion());
      myPackages = getExportedPackages(myManifestWrapper.getFile());
    }

    public ExportedUnit getBundle() {
      return myBundle;
    }

    public List<ExportedUnit> getPackages() {
      return myPackages;
    }
  }
}
