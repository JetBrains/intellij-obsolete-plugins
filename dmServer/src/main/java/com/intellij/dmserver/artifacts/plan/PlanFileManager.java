package com.intellij.dmserver.artifacts.plan;

import com.intellij.dmserver.artifacts.PsiConfigManagerBase;
import com.intellij.dmserver.facet.*;
import com.intellij.dmserver.util.ModuleUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.VersionRange;

import java.util.ArrayList;
import java.util.List;

/**
 * @author michael.golubev
 */
public class PlanFileManager extends PsiConfigManagerBase<XmlFile, DMCompositeFacetConfiguration, DMCompositeFacet> {

  @NonNls
  public static final String PLAN_EXTENSION = "plan";

  private static final Logger LOG = Logger.getInstance(PlanFileManager.class);

  @NonNls
  private static final String TEMPLATE = """
    <?xml version="1.0" encoding="UTF-8"?>
    <plan
    \txmlns="http://www.springsource.org/schema/dm-server/plan"
    \txmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    \t\t\txsi:schemaLocation="
    \t\t\thttp://www.springsource.org/schema/dm-server/plan
    \t\t\thttp://www.springsource.org/schema/dm-server/plan/springsource-dm-server-plan.xsd"
    />""";

  private final Module myModule;

  private final Project myProject;
  private final DomManager myDomManager;
  private final PsiManager myPsiManager;

  private boolean myInFileUpdate = false;

  public PlanFileManager(Module module) {
    myModule = module;
    myProject = module.getProject();
    myPsiManager = PsiManager.getInstance(myProject);
    myDomManager = DomManager.getDomManager(myProject);
  }

  public PlanFileCreator createPlan(ModuleRootModel rootModel) {
    return new PlanFileCreator(rootModel);
  }

  private String getFileName() {
    return myModule.getName();
  }

  private String getFileNameWithExt() {
    return getFileName() + "." + PLAN_EXTENSION;
  }

  @Nullable
  private PlanRootElement getFileRootElement(XmlFile planFile) {
    DomFileElement<PlanRootElement> fileElement = myDomManager.getFileElement(planFile, PlanRootElement.class);
    return fileElement == null ? null : fileElement.getRootElement();
  }

  @Nullable
  private PsiDirectory getPlanFileDirectory(@NotNull ModuleRootModel rootModel) {
    VirtualFile planVirtualDirectory = ModuleUtils.getModuleRoot(rootModel);
    return myPsiManager.findDirectory(planVirtualDirectory);
  }

  @Nullable
  private XmlFile getPlanFile(@NotNull ModuleRootModel rootModel) {
    PsiDirectory planDirectory = getPlanFileDirectory(rootModel);
    return planDirectory == null ? null : (XmlFile)planDirectory.findFile(getFileNameWithExt());
  }

  @Nullable
  public XmlFile findPlanFile() {
    return getPlanFile(ModuleRootManager.getInstance(myModule));
  }

  @Override
  protected XmlFile findConfigFile(Module module) {
    return findPlanFile();
  }

  @Override
  protected boolean onConfigFileChanged(DMCompositeFacetConfiguration configuration2update, XmlFile configWrapper) {
    if (myInFileUpdate) {
      return false;
    }

    PlanRootElement rootElement = getFileRootElement(configWrapper);
    if (rootElement == null) {
      rootElement = myDomManager.createMockElement(PlanRootElement.class, null, false);
    }

    configuration2update.setName(safeValue(rootElement.getName().getValue(), ""));
    configuration2update.setVersion(safeValue(rootElement.getVersion().getValue(), ""));
    configuration2update.setScoped(safeValue(rootElement.getScoped().getValue(), false));
    configuration2update.setAtomic(safeValue(rootElement.getAtomic().getValue(), false));

    List<NestedUnitIdentity> nestedUnitIdentities = new ArrayList<>();
    for (PlanArtifactElement artifactElement : rootElement.getArtifacts()) {
      String artifactName = artifactElement.getName().getValue();
      if (artifactName == null) {
        continue;
      }
      Module module = findModuleBySymbolicName(artifactName);
      if (module == null) {
        continue; // can't find a module for the symbolic name entered
      }

      NestedUnitIdentity nestedUnitIdentity = new NestedUnitIdentity();
      nestedUnitIdentity.setModule(module);
      VersionRange versionRange = artifactElement.getVersion().getValue();
      if (versionRange != null) {
        nestedUnitIdentity.setVersionRange(versionRange.toString());
      }
      nestedUnitIdentities.add(nestedUnitIdentity);
    }
    configuration2update.setNestedBundles(nestedUnitIdentities);
    return true;
  }

  private Module findModuleBySymbolicName(@NotNull String symbolicName) {
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      DMUnitDescriptor unitDescriptor = DMUnitDescriptorProvider.getInstance().processModule(module);
      if (unitDescriptor != null && symbolicName.equals(unitDescriptor.getSymbolicName())) {
        return module;
      }
    }
    return null;
  }

  public class PlanFileCreator {

    private final ModuleRootModel myRootModel;

    private final XmlFile myOldFile;
    private final XmlFile myCreatedPlanPsiFile;

    public PlanFileCreator(@NotNull ModuleRootModel rootModel) {
      myRootModel = rootModel;
      myOldFile = getPlanFile(rootModel);
      if (myOldFile == null || getFileRootElement(myOldFile) == null) {
        PsiFile psiFile = PsiFileFactory.getInstance(myProject).createFileFromText(getFileNameWithExt(), TEMPLATE);
        myCreatedPlanPsiFile = (XmlFile)psiFile;
      }
      else {
        myCreatedPlanPsiFile = (XmlFile)myOldFile.copy();
      }
    }

    @Nullable
    public PlanRootElement getRootElement() {
      return getFileRootElement(myCreatedPlanPsiFile);
    }

    public void save() {
      myInFileUpdate = true;
      try {
        PsiDirectory planPsiDirectory = getPlanFileDirectory(myRootModel);
        if (!LOG.assertTrue(planPsiDirectory != null)) {
          return;
        }

        CodeStyleManager.getInstance(myProject).reformat(myCreatedPlanPsiFile);

        XmlFile oldFile = getPlanFile(myRootModel);
        XmlDocument oldDocument = oldFile == null ? null : oldFile.getDocument();
        if (oldDocument != null) {
          XmlDocument newDocument = myCreatedPlanPsiFile.getDocument();
          if (!LOG.assertTrue(newDocument != null)) {
            return;
          }
          oldDocument.replace(newDocument);
        }
        else {
          planPsiDirectory.add(myCreatedPlanPsiFile);
        }
      }
      finally {
        myInFileUpdate = false;
      }
    }
  }
}
