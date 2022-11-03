package com.intellij.dmserver.test;

import com.intellij.dmserver.artifacts.DMBundleArtifactType;
import com.intellij.dmserver.artifacts.DMContainerPackagingElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElement;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;

public class DMFacetsTest extends DMTestBase {

  public void testBundleManifest() throws Throwable {
    @NonNls final String MODULE_NAME = "mbundle1";

    Module bundleModule = initBundleModule(MODULE_NAME);

    VirtualFile contentRoot = getContentRoot(bundleModule);
    VirtualFile manifestFile = contentRoot.findFileByRelativePath("META-INF/MANIFEST.MF");
    assertNotNull(manifestFile);
  }

  public void testBundleArtifact() throws Throwable {
    @NonNls final String MODULE_NAME = "mbundle1";

    Module bundleModule = initBundleModule(MODULE_NAME);

    Collection<? extends Artifact> bundleArtifacts =
      ArtifactManager.getInstance(getProject()).getArtifactsByType(DMBundleArtifactType.getInstance());
    Artifact bundleArtifact = assertOneElement(bundleArtifacts);

    CompositePackagingElement<?> rootElement = bundleArtifact.getRootElement();
    assertTrue(rootElement instanceof ArtifactRootElement<?>);

    ArtifactRootElement<?> artifactRootElement = (ArtifactRootElement<?>)rootElement;
    PackagingElement<?> rootChild = assertOneElement(artifactRootElement.getChildren());
    assertTrue(rootChild instanceof DMContainerPackagingElement);

    DMContainerPackagingElement containerElement = (DMContainerPackagingElement)rootChild;
    assertEquals(bundleModule, containerElement.findModule());

    PackagingElement<?> containerChild = assertOneElement(containerElement.getChildren());
    assertTrue(containerChild instanceof ModuleOutputPackagingElement);
  }

  public void testPlanFile() throws Throwable {
    @NonNls final String PLAN_MODULE_NAME = "mPlan";
    @NonNls final String PLAN_NAME = "the-plan";
    @NonNls final String NESTED_MODULE_1_NAME = "mNestedBundle1";
    @NonNls final String NESTED_MODULE_2_NAME = "mNestedBundle2";

    Module[] nestedBundleModules = new Module[]{initBundleModule(NESTED_MODULE_1_NAME), initBundleModule(NESTED_MODULE_2_NAME)};

    Module planModule = initPlanModule(PLAN_MODULE_NAME, PLAN_NAME, nestedBundleModules);

    VirtualFile contentRoot = getContentRoot(planModule);
    VirtualFile planFile = contentRoot.findFileByRelativePath(PLAN_MODULE_NAME + ".plan");
    assertNotNull(planFile);

    String planFileText = VfsUtilCore.loadText(planFile);

    @NonNls String expectedPlanFileText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                          "<plan\n" +
                                          "        xmlns=\"http://www.springsource.org/schema/dm-server/plan\"\n" +
                                          "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                          "        xsi:schemaLocation=\"\n" +
                                          "			http://www.springsource.org/schema/dm-server/plan\n" +
                                          "			http://www.springsource.org/schema/dm-server/plan/springsource-dm-server-plan.xsd\"\n" +
                                          "        name=\"" + PLAN_NAME + "\" version=\"1.0.0\" scoped=\"false\" atomic=\"false\">\n" +
                                          "    <artifact type=\"bundle\" name=\"" + NESTED_MODULE_1_NAME + "\" version=\"0.0.0\"/>\n" +
                                          "    <artifact type=\"bundle\" name=\"" + NESTED_MODULE_2_NAME + "\" version=\"0.0.0\"/>\n" +
                                          "</plan>";
    assertEquals(expectedPlanFileText, StringUtil.convertLineSeparators(planFileText));
  }

  public void testParManifest() throws Throwable {
    @NonNls final String MODULE_NAME = "ParModule";
    @NonNls final String PAR_NAME = "the-par";
    @NonNls final String NESTED_MODULE_1_NAME = "NestedBundle1Module";
    @NonNls final String NESTED_MODULE_2_NAME = "NestedBundle2Module";

    Module[] nestedBundleModules = new Module[]{initBundleModule(NESTED_MODULE_1_NAME), initBundleModule(NESTED_MODULE_2_NAME)};

    Module parModule = initParModule(MODULE_NAME, PAR_NAME, nestedBundleModules);

    VirtualFile contentRoot = getContentRoot(parModule);
    VirtualFile manifestFile = contentRoot.findFileByRelativePath("META-INF/MANIFEST.MF");
    assertNotNull(manifestFile);
  }
}
