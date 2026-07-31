package org.jetbrains.jps.gwt;

import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;
import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.gwt.model.impl.JpsGwtCompilerOutputPackagingElement;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.JpsArtifactService;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsProjectData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.intellij.testFramework.UsefulTestCase.assertOneElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class JpsGwtSerializationTest {
  private static final String PROJECT_PATH = "plugins/GwtStudio/jps-plugin/testData/serialization/gwt-project/gwt-project.ipr";

  @Test
  public void testLoadProject() {
    JpsProjectData projectData = JpsProjectData.loadFromTestData(PROJECT_PATH, getClass());
    JpsProject project = projectData.getProject();
    List<JpsModule> modules = project.getModules();
    assertEquals(2, modules.size());
    JpsModule dep = modules.get(0);
    assertEquals("dep", dep.getName());
    JpsModule main = modules.get(1);
    assertEquals("gwt-project", main.getName());

    JpsGwtModuleExtension depExt = JpsGwtExtensionService.getInstance().getExtension(dep);
    assertNotNull(depExt);
    assertEquals(256, depExt.getCompilerMaximumHeapSize());
    assertEquals("-Xmx239m", depExt.getAdditionalCompilerVMParameters());
    assertEquals("-draftCompile", depExt.getCompilerParameters());
    assertSame(GwtJavaScriptOutputStyle.DETAILED, depExt.getOutputStyle());

    JpsGwtModuleExtension mainExt = JpsGwtExtensionService.getInstance().getExtension(main);
    assertNotNull(mainExt);
    assertEquals(128, mainExt.getCompilerMaximumHeapSize());

    JpsArtifact artifact = assertOneElement(JpsArtifactService.getInstance().getArtifacts(project));
    List<JpsPackagingElement> children = artifact.getRootElement().getChildren();
    assertEquals(2, children.size());
    JpsGwtCompilerOutputPackagingElement output = UsefulTestCase.assertInstanceOf(children.get(0), JpsGwtCompilerOutputPackagingElement.class);
    assertSame(JpsGwtCompilerOutputPackagingElement.OutputKind.REGULAR, output.getOutputKind());
    assertSame(main, output.getModuleReference().resolve());
    JpsGwtCompilerOutputPackagingElement deploy = UsefulTestCase.assertInstanceOf(children.get(1), JpsGwtCompilerOutputPackagingElement.class);
    assertSame(JpsGwtCompilerOutputPackagingElement.OutputKind.DEPLOY, deploy.getOutputKind());
    assertSame(main, deploy.getModuleReference().resolve());
  }
}
