package com.intellij.dmserver.test;

import com.intellij.dmserver.artifacts.ManifestManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.HeaderValue;
import org.jetbrains.lang.manifest.psi.Section;
import org.osmorc.facet.OsmorcFacet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMManifestManagerTest extends DMTestBase {

  public void testCreateBundleManifest() {
    @NonNls String MODULE_NAME = "CreateBundleManifestModule";
    @NonNls String SYMBOLIC_NAME = "test.symbolic.name";
    @NonNls String VERSION = "1.2.3.test";

    Module module = createJavaModule(MODULE_NAME);

    OsmorcFacet osmorcFacet = createOsmorcFacet(module);

    VirtualFile manifestFile = ManifestManager.getBundleInstance()
      .createManifest(module, osmorcFacet, ModuleRootManager.getInstance(module), SYMBOLIC_NAME, VERSION);

    Map<String, Header> name2header = collectHeaders(manifestFile);

    checkHeader(name2header, "Manifest-Version", "1.0.0");
    checkHeader(name2header, "Bundle-ManifestVersion", "2");
    checkHeader(name2header, "Bundle-Name", MODULE_NAME);
    checkHeader(name2header, "Bundle-SymbolicName", SYMBOLIC_NAME);
    checkHeader(name2header, "Bundle-Version", VERSION);
  }

  public void testCreateParManifest() {
    @NonNls String MODULE_NAME = "CreateParManifestModule";
    @NonNls String SYMBOLIC_NAME = "test.symbolic.name";
    @NonNls String VERSION = "1.2.3.test";

    Module module = createJavaModule(MODULE_NAME);

    VirtualFile manifestFile =
      ManifestManager.getParInstance().createManifest(module, module, ModuleRootManager.getInstance(module), SYMBOLIC_NAME, VERSION);

    Map<String, Header> name2header = collectHeaders(manifestFile);

    checkHeader(name2header, "Manifest-Version", "1.0.0");
    checkHeader(name2header, "Bundle-ManifestVersion", "2");
    checkHeader(name2header, "Application-Name", MODULE_NAME);
    checkHeader(name2header, "Application-SymbolicName", SYMBOLIC_NAME);
    checkHeader(name2header, "Application-Version", VERSION);
  }

  public void testUpdateManifestPreservedHeaders() {
    {
      @NonNls String MODULE_NAME = "PreservedHeadersToCreateModule";
      Module module = createJavaModule(MODULE_NAME);
      VirtualFile manifestFile = saveParManifestContent(module, "Fake-Header: fake.value");

      ManifestManager.getParInstance().createManifest(module, module, ModuleRootManager.getInstance(module), null, null);

      Map<String, Header> name2header = collectHeaders(manifestFile);

      checkHeader(name2header, "Manifest-Version", "1.0.0");
      checkHeader(name2header, "Bundle-ManifestVersion", "2");
      checkHeader(name2header, "Application-Name", MODULE_NAME);
    }

    {
      @NonNls String BUNDLE_NAME = "PreservedHeadersToKeepBundle";
      @NonNls String MODULE_NAME = "PreservedHeadersToKeepModule";
      @NonNls String MANIFEST_VERSION = "1.2.3";
      @NonNls String BUNDLE_MANIFEST_VERSION = "3";

      Module module = createJavaModule(MODULE_NAME);
      VirtualFile manifestFile = saveParManifestContent(module, "Manifest-Version: " + MANIFEST_VERSION + "\n" +
                                                                "Bundle-ManifestVersion: " + BUNDLE_MANIFEST_VERSION + "\n" +
                                                                "Application-Name: " + BUNDLE_NAME);

      ManifestManager.getParInstance().createManifest(module, module, ModuleRootManager.getInstance(module), null, null);

      Map<String, Header> name2header = collectHeaders(manifestFile);

      checkHeader(name2header, "Manifest-Version", MANIFEST_VERSION);
      checkHeader(name2header, "Bundle-ManifestVersion", BUNDLE_MANIFEST_VERSION);
      checkHeader(name2header, "Application-Name", BUNDLE_NAME);
    }
  }

  public void testUpdateManifestUpdatedHeaders() {
    {
      @NonNls String MODULE_NAME = "UpdatedHeadersToKeepModule";
      @NonNls String SYMBOLIC_NAME = "test.symbolic.name";
      @NonNls String VERSION = "1.2.3.test";

      Module module = createJavaModule(MODULE_NAME);

      VirtualFile manifestFile = saveParManifestContent(module, "Manifest-Version: 1.0.0\n"
                                                                + "Bundle-ManifestVersion: 2\n"
                                                                + "Application-Name: " + MODULE_NAME + "\n"
                                                                + "Application-SymbolicName: " + SYMBOLIC_NAME + "\n"
                                                                + "Application-Version: " + VERSION);

      ManifestManager.getParInstance().createManifest(module, module, ModuleRootManager.getInstance(module), null, null);

      Map<String, Header> name2header = collectHeaders(manifestFile);

      checkHeader(name2header, "Application-SymbolicName", SYMBOLIC_NAME);
      checkHeader(name2header, "Application-Version", VERSION);
    }

    {
      @NonNls String MODULE_NAME = "UpdatedHeadersToUpdateModule";
      @NonNls String SYMBOLIC_NAME_BEFORE_UPDATE = "test.symbolic.name.before.update";
      @NonNls String SYMBOLIC_NAME_AFTER_UPDATE = "test.symbolic.name.after.update";
      @NonNls String VERSION_BEFORE_UPDATE = "1.2.3.before";
      @NonNls String VERSION_AFTER_UPDATE = "4.5.6.after";

      Module module = createJavaModule(MODULE_NAME);

      VirtualFile manifestFile = saveParManifestContent(module, "Manifest-Version: 1.0.0\n"
                                                                + "Bundle-ManifestVersion: 2\n"
                                                                + "Application-Name: " + MODULE_NAME + "\n"
                                                                + "Application-SymbolicName: " + SYMBOLIC_NAME_BEFORE_UPDATE + "\n"
                                                                + "Application-Version: " + VERSION_BEFORE_UPDATE);

      ManifestManager.getParInstance()
        .createManifest(module, module, ModuleRootManager.getInstance(module), SYMBOLIC_NAME_AFTER_UPDATE, VERSION_AFTER_UPDATE);

      Map<String, Header> name2header = collectHeaders(manifestFile);

      checkHeader(name2header, "Application-SymbolicName", SYMBOLIC_NAME_AFTER_UPDATE);
      checkHeader(name2header, "Application-Version", VERSION_AFTER_UPDATE);
    }
  }

  public void testUpdateManifestCustomHeaders() {
    @NonNls String MODULE_NAME = "CustomHeadersToKeepModule";
    @NonNls String CUSTOM_HEADER_NAME = "Custom-Header-Name";
    @NonNls String CUSTOM_HEADER_VALUE = "custom.header.value";

    Module module = createJavaModule(MODULE_NAME);

    VirtualFile manifestFile = saveParManifestContent(module, CUSTOM_HEADER_NAME + ": " + CUSTOM_HEADER_VALUE);

    ManifestManager.getParInstance().createManifest(module, module, ModuleRootManager.getInstance(module), null, null);

    Map<String, Header> name2header = collectHeaders(manifestFile);

    checkHeader(name2header, CUSTOM_HEADER_NAME, CUSTOM_HEADER_VALUE);
  }

  private VirtualFile saveParManifestContent(Module module, @NonNls String manifestContent) {
    VirtualFile root = getContentRoot(module);
    VirtualFile m = createChildDirectory(root, "META-INF");
    VirtualFile manifestFile = createChildData(m, "MANIFEST.MF");
    setFileText(manifestFile, manifestContent);
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    return manifestFile;
  }

  public void testCustomOsmorcManifestLocation() {
    @NonNls String MODULE_NAME = "CustomOsmorcManifestLocationtModule";
    @NonNls String MANIFEST_LOCATION = "src/META-INF/MANIFEST.MF";

    Module module = createJavaModule(MODULE_NAME);

    OsmorcFacet osmorcFacet = createOsmorcFacet(module);
    osmorcFacet.getConfiguration().setUseProjectDefaultManifestFileLocation(false);
    osmorcFacet.getConfiguration().setManifestLocation(MANIFEST_LOCATION);

    VirtualFile manifestFile =
      ManifestManager.getBundleInstance().createManifest(module, osmorcFacet, ModuleRootManager.getInstance(module), null, null);

    Map<String, Header> name2header = collectHeaders(manifestFile);

    checkHeader(name2header, "Manifest-Version", "1.0.0");

    assertEquals(MANIFEST_LOCATION, VfsUtilCore.getRelativePath(manifestFile, getContentRoot(module), '/'));
  }

  public void testManifestFileWrapper() {
    @NonNls String MODULE_NAME = "ManifestFileWrapperModule";
    @NonNls String SYMBOLIC_NAME = "test.symbolic.name";
    @NonNls String VERSION = "1.2.3.test";

    Module module = createJavaModule(MODULE_NAME);

    VirtualFile manifestFile = saveParManifestContent(module, "Manifest-Version: 1.0.0\n"
                                                              + "Bundle-ManifestVersion: 2\n"
                                                              + "Application-Name: " + MODULE_NAME + "\n"
                                                              + "Application-SymbolicName: " + SYMBOLIC_NAME + "\n"
                                                              + "Application-Version: " + VERSION);

    ManifestManager.FileWrapper manifestWrapper = ManifestManager.getParInstance().findManifest(module);
    assertNotNull(manifestWrapper);

    assertEquals(SYMBOLIC_NAME, manifestWrapper.getSymbolicName());
    assertEquals(VERSION, manifestWrapper.getVersion());
    assertNotNull(manifestWrapper.getFile());
    assertNotNull(manifestWrapper.getFile().getVirtualFile());
    assertEquals(manifestFile.getPath(), manifestWrapper.getFile().getVirtualFile().getPath());
  }

  private Map<String, Header> collectHeaders(VirtualFile manifestFile) {
    assertNotNull(manifestFile);

    PsiFile manifest = PsiManager.getInstance(getProject()).findFile(manifestFile);
    assertNotNull(manifest);

    Section section = assertOneElement(PsiTreeUtil.getChildrenOfTypeAsList(manifest, Section.class));

    List<Header> headers = PsiTreeUtil.getChildrenOfTypeAsList(section, Header.class);

    Map<String, Header> name2header = new HashMap<>();
    for (Header header : headers) {
      name2header.put(header.getName(), header);
    }

    return name2header;
  }

  private static void checkHeader(Map<String, Header> name2header, @NonNls String name, String expected) {
    Header header = name2header.get(name);
    assertNotNull(header);
    HeaderValue value = assertOneElement(header.getHeaderValues());
    assertEquals(expected, value.getUnwrappedText());
  }

  public void testCreateManifestDuringModuleCreation() {
    @NonNls String MODULE_NAME = "CreateManifestDuringModuleCreationModule";
    @NonNls String SYMBOLIC_NAME = "test.symbolic.name";
    @NonNls String VERSION = "1.2.3.test";

    Module module = createJavaModule(MODULE_NAME);

    OsmorcFacet osmorcFacet = createOsmorcFacet(module);

    VirtualFile manifestFile = ManifestManager.getBundleInstance().createManifest(module, osmorcFacet, ModuleRootManager.getInstance(module),
                                                                                  SYMBOLIC_NAME, VERSION);

    Map<String, Header> name2header = collectHeaders(manifestFile);

    checkHeader(name2header, "Manifest-Version", "1.0.0");
    checkHeader(name2header, "Bundle-ManifestVersion", "2");
    checkHeader(name2header, "Bundle-Name", MODULE_NAME);
    checkHeader(name2header, "Bundle-SymbolicName", SYMBOLIC_NAME);
    checkHeader(name2header, "Bundle-Version", VERSION);
  }
}
