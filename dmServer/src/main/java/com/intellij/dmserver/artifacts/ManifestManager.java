package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.*;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.dmserver.util.ModuleUtils;
import com.intellij.dmserver.util.PathUtils;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EventDispatcher;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osmorc.facet.OsmorcFacet;

import java.io.IOException;
import java.util.jar.Attributes;

public abstract class ManifestManager<C extends DMFacetConfigurationBase<C>, F extends DMFacetBase<C>, P>
  extends PsiConfigManagerBase<ManifestFile, C, F> {

  private static final Logger LOG = Logger.getInstance(ManifestManager.class);

  @NonNls
  public static final String META_INF_DIR = "META-INF";

  @NonNls
  public static final String MANIFEST_FILE = "MANIFEST.MF";

  private final static ManifestManager<DMBundleFacetConfiguration, DMBundleFacet, OsmorcFacet> ourBundleInstance =
    new ManifestManager<>(
      Constants.BUNDLE_NAME,
      Constants.BUNDLE_SYMBOLICNAME,
      Constants.BUNDLE_VERSION) {

      @Override
      protected String getManifestPathFromSource(@NotNull OsmorcFacet osmorcFacet, @NotNull VirtualFile contentRoot) {
        String result = osmorcFacet.getManifestLocation();
        if (!osmorcFacet.getConfiguration().isUseProjectDefaultManifestFileLocation()) {
          String fullPath = getManifestFullPath(contentRoot, result);
          VirtualFile locationFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(fullPath);
          if (locationFile != null && locationFile.isDirectory()) {
            result = PathUtils.concatPaths(result, MANIFEST_FILE);
            osmorcFacet.getConfiguration().setManifestLocation(FileUtil.toSystemDependentName(result));
          }
        }
        return result;
      }

      @Override
      protected String getManifestPathFromModule(@NotNull Module module, @NotNull VirtualFile contentRoot) {
        return getManifestPathFromSource(OsmorcFacet.getInstance(module), contentRoot);
      }

      @Override
      protected boolean onConfigFileChanged(DMBundleFacetConfiguration configuration2update, ManifestFile configFile) {
        return false;
      }
    };

  private static final String PAR_MANIFEST_PATH = PathUtils.concatPaths(META_INF_DIR, MANIFEST_FILE);

  private final static ManifestManager<DMCompositeFacetConfiguration, DMCompositeFacet, Module> ourParInstance =
    new ManifestManager<>(
      "Application-Name",
      "Application-SymbolicName",
      "Application-Version") {

      @Override
      protected String getManifestPathFromSource(@NotNull Module module, @NotNull VirtualFile contentRoot) {
        return PAR_MANIFEST_PATH;
      }

      @Override
      protected String getManifestPathFromModule(@NotNull Module module, @NotNull VirtualFile contentRoot) {
        return getManifestPathFromSource(module, contentRoot);
      }

      @Override
      protected boolean onConfigFileChanged(DMCompositeFacetConfiguration configuration2update, ManifestFile configFile) {
        if (isInFileUpdate()) {
          return false;
        }
        FileWrapper fileWrapper = new FileWrapper(configFile);
        configuration2update.setName(safeValue(fileWrapper.getSymbolicName(), ""));
        configuration2update.setVersion(safeValue(fileWrapper.getVersion(), ""));
        return true;
      }
    };

  public static ManifestManager<DMBundleFacetConfiguration, DMBundleFacet, OsmorcFacet> getBundleInstance() {
    return ourBundleInstance;
  }

  public static ManifestManager<DMCompositeFacetConfiguration, DMCompositeFacet, Module> getParInstance() {
    return ourParInstance;
  }

  private final String myNameHeader;
  private final String mySymbolicNameHeader;
  private final String myVersionHeader;

  private boolean myInFileUpdate = false;

  private final EventDispatcher<ManifestManagerListener> myEventDispatcher = EventDispatcher.create(ManifestManagerListener.class);

  private ManifestManager(@NonNls String nameHeader, @NonNls String symbolicNameHeader, @NonNls String versionHeader) {
    myNameHeader = nameHeader;
    mySymbolicNameHeader = symbolicNameHeader;
    myVersionHeader = versionHeader;
  }

  protected final boolean isInFileUpdate() {
    return myInFileUpdate;
  }

  protected final String getManifestFullPath(VirtualFile contentRoot, String manifestPath) {
    return PathUtils.concatPaths(contentRoot.getPath(), manifestPath);
  }

  public VirtualFile createManifest(@NotNull final Module module,
                                    @NotNull final P manifestPathSource,
                                    @NotNull final ModuleRootModel rootModel,
                                    @Nullable final String symbolicName,
                                    @Nullable final String version) {
    return WriteCommandAction.runWriteCommandAction(module.getProject(), (Computable<VirtualFile>)() -> {
      try {
        return doCreateManifest(module, manifestPathSource, rootModel, symbolicName, version);
      }
      catch (IOException e) {
        LOG.error(e);
        return null;
      }
    });
  }

  private VirtualFile doCreateManifest(@NotNull final Module module,
                                       @NotNull P manifestPathSource,
                                       @NotNull ModuleRootModel rootModel,
                                       @Nullable String symbolicName,
                                       @Nullable String version) throws IOException {
    VirtualFile contentRoot = ModuleUtils.getModuleRoot(rootModel);
    String fullPath = getManifestFullPath(contentRoot, getManifestPathFromSource(manifestPathSource, contentRoot));
    String parentPath = PathUtil.getParentPath(fullPath);
    String filename = PathUtil.getFileName(fullPath);
    VirtualFile parentFolder = VfsUtil.createDirectories(parentPath);

    VirtualFile manifestVirtualFile = parentFolder.findChild(filename);
    if (manifestVirtualFile == null) {
      manifestVirtualFile = parentFolder.createChildData(this, filename);
    }

    String moduleName = module.getName();

    final String versionValue;
    if (version == null) {
      Version moduleVersion = null;
      int nextDotPos = moduleName.indexOf('.');
      while (moduleVersion == null && nextDotPos >= 0) {
        try {
          moduleVersion = new Version(moduleName.substring(nextDotPos + 1));
          moduleName = moduleName.substring(0, nextDotPos);
        }
        catch (IllegalArgumentException e) {
          // Retry after next dot.
        }
        nextDotPos = moduleName.indexOf('.', nextDotPos + 1);
      }
      versionValue = moduleVersion == null ? "1.0.0" : moduleVersion.toString();
    }
    else {
      versionValue = version;
    }

    final String symbolicNameValue = symbolicName == null ? moduleName : symbolicName;

    final VirtualFile finalManifestVirtualFile = manifestVirtualFile;
    final String finalModuleName = moduleName;
    final boolean keepSymbolicName = symbolicName == null;
    final boolean keepVersion = version == null;

    Runnable updateRunnable = () -> {
      myInFileUpdate = true;
      try {
        ManifestFile manifestFile = (ManifestFile)PsiManager.getInstance(module.getProject()).findFile(finalManifestVirtualFile);
        if (manifestFile == null) {
          LOG.error("Can't find manifest PSI file");
          return;
        }

        ManifestUpdater manifestUpdater = new ManifestUpdater(manifestFile);
        manifestUpdater.updateHeader(Attributes.Name.MANIFEST_VERSION.toString(), "1.0.0", true);
        manifestUpdater.updateHeader(Constants.BUNDLE_MANIFESTVERSION, "2", true);
        manifestUpdater.updateHeader(myNameHeader, finalModuleName, true);
        manifestUpdater.updateHeader(mySymbolicNameHeader, symbolicNameValue, keepSymbolicName);
        manifestUpdater.updateHeader(myVersionHeader, versionValue, keepVersion);

        myEventDispatcher.getMulticaster().manifestCreated(module, new FileWrapper(manifestFile));
      }
      finally {
        myInFileUpdate = false;
      }
    };

    StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(updateRunnable);

    return manifestVirtualFile;
  }

  @Nullable
  public FileWrapper findManifest(@NotNull Module module) {
    ManifestFile manifestPsiFile = findManifestFile(module);
    if (manifestPsiFile == null) {
      return null;
    }
    return new FileWrapper(manifestPsiFile);
  }

  private VirtualFile findManifestVirtualFile(@NotNull Module module) {
    VirtualFile contentRoot = ModuleUtils.getModuleRoot(module);
    return contentRoot.findFileByRelativePath(getManifestPathFromModule(module, contentRoot));
  }

  @Nullable
  public VirtualFile findManifestFolder(@NotNull Module module) {
    VirtualFile manifestVirtualFile = findManifestVirtualFile(module);
    if (manifestVirtualFile == null) {
      return null;
    }
    return manifestVirtualFile.getParent();
  }

  private ManifestFile findManifestFile(@NotNull Module module) {
    VirtualFile manifestVirtualFile = findManifestVirtualFile(module);
    if (manifestVirtualFile == null) {
      return null;
    }
    return (ManifestFile)PsiManager.getInstance(module.getProject()).findFile(manifestVirtualFile);
  }

  @Override
  protected ManifestFile findConfigFile(Module module) {
    return findManifestFile(module);
  }

  public void addListener(@NotNull ManifestManagerListener listener, @NotNull Disposable disposable) {
    myEventDispatcher.addListener(listener);
    Disposer.register(disposable, () -> {
      myEventDispatcher.removeListener(listener);
    });
  }

  protected abstract String getManifestPathFromSource(@NotNull P manifestPathSource, @NotNull VirtualFile contentRoot);

  protected abstract String getManifestPathFromModule(@NotNull Module module, @NotNull VirtualFile contentRoot);

  public final class FileWrapper {

    private final ManifestFile myFile;

    private FileWrapper(@NotNull ManifestFile file) {
      myFile = file;
    }

    public String getVersion() {
      return ManifestUtils.getInstance().getHeaderValue(myFile, myVersionHeader);
    }

    public String getSymbolicName() {
      return ManifestUtils.getInstance().getHeaderValue(myFile, mySymbolicNameHeader);
    }

    public ManifestFile getFile() {
      return myFile;
    }
  }
}
