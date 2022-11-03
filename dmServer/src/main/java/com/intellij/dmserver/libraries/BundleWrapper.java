package com.intellij.dmserver.libraries;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.osgi.jps.build.CachingBundleInfoProvider;

/**
 * @author michael.golubev
 */
public final class BundleWrapper extends BundleDefinition {
  private static final Logger LOG = Logger.getInstance(BundleWrapper.class);

  @NonNls
  private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

  @Nullable
  public static BundleWrapper load(VirtualFile jarFile) {
    String path = jarFile.getPath();
    if (!CachingBundleInfoProvider.isBundle(path)) {
      return null;
    }
    VirtualFile bundleRoot = jarFile.isDirectory() ? jarFile : JarFileSystem.getInstance().getJarRootForLocalFile(jarFile);
    VirtualFile manifestFile = bundleRoot == null ? null : bundleRoot.findFileByRelativePath(MANIFEST_PATH);
    if (manifestFile == null) {
      LOG.error("Manifest is expected to exist");
    }
    return new BundleWrapper(CachingBundleInfoProvider.getBundleSymbolicName(path), CachingBundleInfoProvider.getBundleVersion(path), jarFile, manifestFile);
  }

  private final VirtualFile myJarFile;
  private final VirtualFile myManifestFile;

  private BundleWrapper(String symbolicName, String version, VirtualFile jarFile, VirtualFile manifestFile) {
    super(symbolicName, version);
    myJarFile = jarFile;
    myManifestFile = manifestFile;
  }

  public VirtualFile getJarFile() {
    return myJarFile;
  }

  public VirtualFile getManifestFile() {
    return myManifestFile;
  }
}
