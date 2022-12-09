package com.intellij.dmserver.test;

import com.intellij.dmserver.artifacts.plan.DMArtifactElementType;
import com.intellij.dmserver.artifacts.plan.PlanArtifactElement;
import com.intellij.dmserver.artifacts.plan.PlanFileManager;
import com.intellij.dmserver.artifacts.plan.PlanRootElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.osgi.framework.VersionRange;

/**
 * @author michael.golubev
 */
public class DMPlanFileManagerTest extends DMTestBase {
  public void testCreatePlanFile() throws Throwable {
    @NonNls final String MODULE_NAME = "CreatePlanFileModule";
    @NonNls final String PLAN_NAME = "plan-name";
    @NonNls final String VERSION = "1.2.3.test";
    final boolean SCOPED = false;
    final boolean ATOMIC = true;

    @NonNls final String artifact1SymbolicName = "a-bundle";
    @NonNls final String artifact1VersionRange = "1.0.0";
    @NonNls final String artifact2SymbolicName = "a-par";
    @NonNls final String artifact2VersionRange = "[1.0.0,2.0.0)";
    @NonNls final String artifact3SymbolicName = "a-plan";
    @NonNls final String artifact4SymbolicName = "a-config";
    @NonNls final String artifact4VersionRange = "[1.2.3,4.5.6.test]";

    Module module = createJavaModule(MODULE_NAME);

    PlanFileManager planManager = new PlanFileManager(module);

    final PlanFileManager.PlanFileCreator planCreator = planManager.createPlan(ModuleRootManager.getInstance(module));
    assertNotNull(planCreator);

    PlanRootElement rootElement = planCreator.getRootElement();
    assertNotNull(rootElement);

    rootElement.getName().setValue(PLAN_NAME);
    rootElement.getVersion().setValue(VERSION);
    rootElement.getScoped().setValue(SCOPED);
    rootElement.getAtomic().setValue(ATOMIC);

    PlanArtifactElement artifactElement1 = rootElement.addArtifact();
    artifactElement1.getType().setValue(DMArtifactElementType.TYPE_BUNDLE);
    artifactElement1.getName().setValue(artifact1SymbolicName);
    artifactElement1.getVersion().setValue(new VersionRange(artifact1VersionRange));

    PlanArtifactElement artifactElement2 = rootElement.addArtifact();
    artifactElement2.getType().setValue(DMArtifactElementType.TYPE_PAR);
    artifactElement2.getName().setValue(artifact2SymbolicName);
    artifactElement2.getVersion().setValue(new VersionRange(artifact2VersionRange));

    PlanArtifactElement artifactElement3 = rootElement.addArtifact();
    artifactElement3.getType().setValue(DMArtifactElementType.TYPE_PLAN);
    artifactElement3.getName().setValue(artifact3SymbolicName);

    PlanArtifactElement artifactElement4 = rootElement.addArtifact();
    artifactElement4.getType().setValue(DMArtifactElementType.TYPE_CONFIG);
    artifactElement4.getName().setValue(artifact4SymbolicName);
    artifactElement4.getVersion().setValue(new VersionRange(artifact4VersionRange));

    ApplicationManager.getApplication().runWriteAction(() -> planCreator.save());


    XmlFile planXmlFile = planManager.findPlanFile();
    assertNotNull(planXmlFile);
    VirtualFile planFile = planXmlFile.getVirtualFile();
    assertNotNull(planFile);
    String planFileContent = VfsUtilCore.loadText(planFile);
    @NonNls String expectedPlanFileContent =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<plan\n" +
      "        xmlns=\"http://www.springsource.org/schema/dm-server/plan\"\n" +
      "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "        xsi:schemaLocation=\"\n" +
      "			http://www.springsource.org/schema/dm-server/plan\n" +
      "			http://www.springsource.org/schema/dm-server/plan/springsource-dm-server-plan.xsd\"\n" +
      "        name=\"" + PLAN_NAME + "\" version=\"" + VERSION + "\" scoped=\"false\" atomic=\"true\">\n" +
      "    <artifact type=\"bundle\" name=\"" + artifact1SymbolicName + "\" version=\"" + artifact1VersionRange + "\"/>\n" +
      "    <artifact type=\"par\" name=\"" + artifact2SymbolicName + "\" version=\"" + artifact2VersionRange + "\"/>\n" +
      "    <artifact type=\"plan\" name=\"" + artifact3SymbolicName + "\"/>\n" +
      "    <artifact type=\"configuration\" name=\"" + artifact4SymbolicName + "\" version=\"" + artifact4VersionRange + "\"/>\n" +
      "</plan>";
    assertEquals(expectedPlanFileContent, StringUtil.convertLineSeparators(planFileContent));
  }

  public void testCreatePlanFileDuringModuleCreation() throws Throwable {
    @NonNls final String MODULE_NAME = "CreatePlanFileDuringModuleCreationModule";
    @NonNls final String PLAN_NAME = "plan-name";
    @NonNls final String VERSION = "1.2.3.test";
    final boolean SCOPED = false;
    final boolean ATOMIC = true;

    @NonNls final String artifact1SymbolicName = "a-bundle";
    @NonNls final String artifact1VersionRange = "1.0.0";

    Module module = createJavaModule(MODULE_NAME);

    PlanFileManager planManager = new PlanFileManager(module);

    final PlanFileManager.PlanFileCreator planCreator = planManager.createPlan(ModuleRootManager.getInstance(module));
    assertNotNull(planCreator);

    PlanRootElement rootElement = planCreator.getRootElement();
    assertNotNull(rootElement);

    rootElement.getName().setValue(PLAN_NAME);
    rootElement.getVersion().setValue(VERSION);
    rootElement.getScoped().setValue(SCOPED);
    rootElement.getAtomic().setValue(ATOMIC);

    PlanArtifactElement artifactElement1 = rootElement.addArtifact();
    artifactElement1.getType().setValue(DMArtifactElementType.TYPE_BUNDLE);
    artifactElement1.getName().setValue(artifact1SymbolicName);
    artifactElement1.getVersion().setValue(new VersionRange(artifact1VersionRange));

    ApplicationManager.getApplication().runWriteAction(() -> planCreator.save());


    XmlFile planXmlFile = planManager.findPlanFile();
    assertNotNull(planXmlFile);
    VirtualFile planFile = planXmlFile.getVirtualFile();
    assertNotNull(planFile);
    String planFileContent = VfsUtilCore.loadText(planFile);
    @NonNls String expectedPlanFileContent =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<plan\n" +
      "        xmlns=\"http://www.springsource.org/schema/dm-server/plan\"\n" +
      "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "        xsi:schemaLocation=\"\n" +
      "			http://www.springsource.org/schema/dm-server/plan\n" +
      "			http://www.springsource.org/schema/dm-server/plan/springsource-dm-server-plan.xsd\"\n" +
      "        name=\"" + PLAN_NAME + "\" version=\"" + VERSION + "\" scoped=\"false\" atomic=\"true\">\n" +
      "    <artifact type=\"bundle\" name=\"" + artifact1SymbolicName + "\" version=\"" + artifact1VersionRange + "\"/>\n" +
      "</plan>";
    assertEquals(expectedPlanFileContent, StringUtil.convertLineSeparators(planFileContent));
  }
}
