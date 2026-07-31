package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.InspectionWrapperUtil;
import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.JavaInspectionTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class GwtInspectionsTestCase extends JavaInspectionTestCase {

  private final LightProjectDescriptor myProjectDescriptor = createDescriptor(getVersion());

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return myProjectDescriptor;
  }

  protected abstract GwtVersionImpl getVersion();

  @Override
  @NonNls
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath() + "inspections";
  }

  protected DefaultLightProjectDescriptor createDescriptor(final GwtVersionImpl version) {
    return new DefaultLightProjectDescriptor() {

      @Override
      public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
        super.configureModule(module, model, contentEntry);
        contentEntry.addSourceFolder(contentEntry.getUrl() + "/ext_src", false);
        contentEntry.addSourceFolder(contentEntry.getUrl() + "/test_src", true);
        GwtFacet gwtFacet = GwtTestCase.addGwtFacet(module, version);
        
        String path = GwtTestCase.getMockGwtUserJarPath(version);
        final VirtualFile jarFile = JarFileSystem.getInstance().findFileByPath(path);
        assertNotNull(jarFile);
        LibraryEx library = (LibraryEx)model.getModuleLibraryTable().createLibrary("gwt");
        LibraryEx.ModifiableModelEx libraryModel = library.getModifiableModel();
        libraryModel.addRoot(jarFile, OrderRootType.CLASSES);
        libraryModel.commit();

        String webXmlPath = getTestName(true) + "/src/web.xml";
        String webXmlPathFullPath = getTestDataPath() + "/" + webXmlPath;
        if (LocalFileSystem.getInstance().findFileByPath(webXmlPathFullPath) != null) {
          WebFacet webFacet = GwtTestCase.addWebFacet(gwtFacet, getTestDataPath(), webXmlPath);
          assertSame(webFacet, gwtFacet.getWebFacet());
        }
      }
    };
  }

  protected void doTest(InspectionProfileEntry inspectionTool) {
    try {
      doTest(getTestName(true), InspectionWrapperUtil.wrapTool(inspectionTool));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
