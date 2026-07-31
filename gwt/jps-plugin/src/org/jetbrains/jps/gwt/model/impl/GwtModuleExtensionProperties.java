package org.jetbrains.jps.gwt.model.impl;

import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;

import java.util.ArrayList;
import java.util.List;

public class GwtModuleExtensionProperties {
  @OptionTag(value = "additionalCompilerParameters", tag = "setting")
  public String myAdditionalCompilerVMParameters = "";

  @OptionTag(value = "compilerParameters", tag = "setting")
  public String myCompilerParameters = "";

  @OptionTag(value = "compilerMaxHeapSize", tag = "setting")
  public int myCompilerMaxHeapSize;

  @OptionTag(value = "gwtScriptOutputStyle", tag = "setting")
  public GwtJavaScriptOutputStyle myOutputStyle;

  @OptionTag(value = "gwtSdkUrl", tag = "setting")
  public String mySdkUrl = "";

  @OptionTag(value = "gwtSdkType", tag = "setting")
  public String mySdkType;

  @OptionTag(value = "webFacet", tag = "setting")
  public String myWebFacetName;

  @XCollection(propertyElementName = "packaging", elementName = "module")
  public List<GwtModulePackagingProperties>
    myPackagingStates = new ArrayList<>();

  public @Nullable String getClientLanguageLevel() {
    List<String> parameters = ParametersListUtil.parse(myCompilerParameters);

    int sourceParameterIndex = parameters.indexOf("-sourceLevel") + 1;
    if (sourceParameterIndex != 0 && sourceParameterIndex != parameters.size()) {
      return parameters.get(sourceParameterIndex);
    }
    return null;
  }
}
