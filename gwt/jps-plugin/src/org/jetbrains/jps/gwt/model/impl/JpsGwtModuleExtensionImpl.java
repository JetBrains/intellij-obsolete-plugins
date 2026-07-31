package org.jetbrains.jps.gwt.model.impl;

import com.intellij.util.PathUtilRt;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtGradleSdkPaths;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtMavenSdkPaths;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtSdkPathsImpl;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinMavenSdkPaths;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinSdkPaths;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

public class JpsGwtModuleExtensionImpl extends JpsCompositeElementBase<JpsGwtModuleExtensionImpl> implements JpsGwtModuleExtension {
  public static final JpsElementChildRole<JpsGwtModuleExtension> ROLE = JpsElementChildRoleBase.create("GWT");
  public static final int DEFAULT_COMPILER_HEAP_SIZE = 1024;
  private final GwtModuleExtensionProperties myProperties;

  public JpsGwtModuleExtensionImpl(GwtModuleExtensionProperties properties) {
    myProperties = properties;
  }

  private JpsGwtModuleExtensionImpl(JpsGwtModuleExtensionImpl original) {
    super(original);
    myProperties = XmlSerializerUtil.createCopy(original.myProperties);
  }

  public GwtModuleExtensionProperties getProperties() {
    return myProperties;
  }

  @Override
  public @NotNull JpsModule getModule() {
    return (JpsModule)myParent;
  }

  @Override
  public String getCompilerParameters() {
    return myProperties.myCompilerParameters;
  }

  @Override
  public String getAdditionalCompilerVMParameters() {
    return myProperties.myAdditionalCompilerVMParameters;
  }

  @Override
  public int getCompilerMaximumHeapSize() {
    return myProperties.myCompilerMaxHeapSize > 0 ? myProperties.myCompilerMaxHeapSize : DEFAULT_COMPILER_HEAP_SIZE;
  }

  @Override
  public GwtJavaScriptOutputStyle getOutputStyle() {
    return myProperties.myOutputStyle != null ? myProperties.myOutputStyle : GwtJavaScriptOutputStyle.DETAILED;
  }

  @Override
  public String getPackagingRelativePath(JpsGwtModule module) {
    for (GwtModulePackagingProperties state : myProperties.myPackagingStates) {
      if (state.myName.equals(module.getQualifiedName()) && state.myPath != null) {
        return state.myPath;
      }
    }
    return "/" + module.getOutputName();
  }

  @Override
  public boolean isModuleCompilationEnabled(JpsGwtModule module) {
    for (GwtModulePackagingProperties state : myProperties.myPackagingStates) {
      if (state.myName.equals(module.getQualifiedName())) {
        return state.myEnabled;
      }
    }
    return true;
  }

  @Override
  public @NotNull GwtSdkPaths getSdkPaths() {
    String sdkUrl = myProperties.mySdkUrl;
    String sdkPath = JpsPathUtil.urlToPath(sdkUrl);
    String sdkType = myProperties.mySdkType;
    if (GwtMavenSdkPaths.TYPE_ID.equals(sdkType)) {
      String version = PathUtilRt.getFileName(sdkPath);
      return new GwtMavenSdkPaths(sdkPath, version);
    }
    if (GwtGradleSdkPaths.TYPE_ID.equals(sdkType)) {
      String version = PathUtilRt.getFileName(sdkPath);
      return new GwtGradleSdkPaths(sdkPath, version);
    }
    if (GwtVaadinSdkPaths.TYPE_ID.equals(sdkType)) {
      return new GwtVaadinSdkPaths(sdkPath);
    }
    if (GwtVaadinMavenSdkPaths.TYPE_ID.equals(sdkType)) {
      return new GwtVaadinMavenSdkPaths(sdkPath);
    }
    return new GwtSdkPathsImpl(sdkUrl);
  }

  @Override
  public @NotNull JpsGwtModuleExtensionImpl createCopy() {
    return new JpsGwtModuleExtensionImpl(this);
  }
}
