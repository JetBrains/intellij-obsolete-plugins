package com.intellij.gwt.sdk.impl;

import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public enum GwtVersionImpl implements GwtVersion {
  VERSION_1_0,
  VERSION_FROM_1_1_TO_1_3,
  VERSION_1_4,
  VERSION_1_5,
  VERSION_1_6,
  VERSION_2_0,
  VERSION_2_4,
  VERSION_2_5,
  VERSION_2_6,
  VERSION_2_7,
  VERSION_2_8,
  VERSION_2_9,
  VERSION_2_10,
  VERSION_2_11;

  public static final GwtVersionImpl LATEST = VERSION_2_11;

  private static final @NonNls String[] GWT_15_SAMPLE_TEMPLATES = {
      "java/client/App.java.ft",
      "java/client/AppService.java.ft",
      "java/client/AppServiceAsync.java.ft",
      "java/public/App.html.ft",
      "java/server/AppServiceImpl.java.ft",
      "java/App.gwt.xml.ft",
  };
  private static final @NonNls String[] GWT_16_SAMPLE_TEMPLATES = {
      "java/client/App.java.ft",
      "java/client/AppService.java.ft",
      "java/client/AppServiceAsync.java.ft",
      "java/server/AppServiceImpl.java.ft",
      "java/App.gwt.xml.ft",
      "war/App.html.ft",
  };
  private static final @NonNls String[] GWT_20_SAMPLE_TEMPLATES = {
      "java/client/App.java.ft",
      "java/client/AppService.java.ft",
      "java/client/AppServiceAsync.java.ft",
      "java/server/AppServiceImpl.java.ft",
      "java/App.gwt.xml.ft",
      "war/App.html.ft",
      "war/App.css.ft",
  };
  private static final @NonNls String GWT_MODULE_OLD_DOCTYPE_FORMAT =
    "<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit ${VERSION}//EN\" " +
    "\"http://google-web-toolkit.googlecode.com/svn/releases/${VERSION}/distro-source/core/src/gwt-module.dtd\">";

  private static final @NonNls String GWT_MODULE_NEW_DOCTYPE_FORMAT =
    "<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit ${VERSION}//EN\" " +
    "\"http://gwtproject.org/doctype/${VERSION}/gwt-module.dtd\">";
  private static final @NonNls String VERSION_PLACEHOLDER = "${VERSION}";

  @Override
  public @NotNull String getGwtModuleHtmlTemplate() {
    return isAtLeast(VERSION_1_6) ? GwtTemplates.GWT_MODULE_HTML_1_6 :
                       isAtLeast(VERSION_1_4) ? GwtTemplates.GWT_MODULE_HTML_1_4 : GwtTemplates.GWT_MODULE_HTML;
  }

  @Override
  public @NotNull String getGwtServiceJavaTemplate() {
    return this == VERSION_1_0 ? GwtTemplates.GWT_SERVICE_JAVA_1_0 :
           isAtLeast(VERSION_1_5) ? GwtTemplates.GWT_SERVICE_JAVA_1_5 : GwtTemplates.GWT_SERVICE_JAVA;
  }

  @Override
  public @NotNull String getGwtModuleXmlTemplate() {
    if (isAtLeast(VERSION_1_6)) return GwtTemplates.GWT_MODULE_1_6_GWT_XML;
    if (this == VERSION_1_5) return GwtTemplates.GWT_MODULE_1_5_GWT_XML;
    return GwtTemplates.GWT_MODULE_1_4_GWT_XML;
  }

  public boolean isAtLeast(GwtVersionImpl another) {
    return compareTo(another) >= 0;
  }

  @Override
  public String @NotNull [] getGwtSampleAppTemplates() {
    String[] templates;
    String samplesVersion;
    if (isAtLeast(VERSION_2_0)) {
      samplesVersion = "20";
      templates = GWT_20_SAMPLE_TEMPLATES;
    }
    else if (isAtLeast(VERSION_1_6)) {
      samplesVersion = "16";
      templates = GWT_16_SAMPLE_TEMPLATES;
    }
    else {
      samplesVersion = "15";
      templates = GWT_15_SAMPLE_TEMPLATES;
    }
    @NonNls String root = "/sampleApps/gwt" + samplesVersion + "/";
    final String[] result = new String[templates.length];
    for (int i = 0; i < templates.length; i++) {
      result[i] = root + templates[i];
    }
    return result;
  }

  @Override
  public boolean isJavaIoSerializableSupported() {
    return isAtLeast(VERSION_1_4);
  }

  @Override
  public boolean isPrivateNoArgConstructorInSerializableClassAllowed() {
    return isAtLeast(VERSION_1_5);
  }

  @Override
  public boolean isGenericsSupported() {
    return isAtLeast(VERSION_1_5);
  }

  @Override
  public boolean isNewExpressionInJavaScriptSupported() {
    return isAtLeast(VERSION_1_5);
  }

  @Override
  public boolean isShortClassReferencesInJavaScriptSupported() {
    return isAtLeast(VERSION_2_7);
  }

  @Override
  public boolean isWildcardMethodReferencesInJavaScriptSupported() {
    return isAtLeast(VERSION_2_5);
  }

  @Override
  public @NotNull String getCompilerClassName() {
    return isAtLeast(VERSION_1_6) ? GwtSdkUtil.GWT_16_COMPILER_MAIN_CLASS : GwtSdkUtil.GWT_15_COMPILER_MAIN_CLASS;
  }

  @Override
  public @NotNull String getDevModeClass() {
    if (isAtLeast(VERSION_2_0)) {
      return GwtSdkUtil.GWT_20_DEV_MODE_CLASS;
    }
    return isAtLeast(VERSION_1_6) ? "com.google.gwt.dev.HostedMode" : GwtSdkUtil.GWT_15_DEV_MODE_CLASS;
  }

  @Override
  public boolean isModulesToLoadSpecifiedInDevMode() {
    return isAtLeast(VERSION_1_6);
  }

  @Override
  public boolean isHostedModeRequiresWebXml() {
    return isAtLeast(VERSION_1_6);
  }

  @Override
  public boolean isHtmlFilesOutsideSourcesAreAllowed() {
    return isAtLeast(VERSION_1_6);
  }

  @Override
  public boolean isEventHandlersSupported() {
    return isAtLeast(VERSION_1_6);
  }

  @Override
  public boolean isDevModeSupportsOutputStyleOption() {
    return !isAtLeast(VERSION_2_0);
  }

  @Override
  public boolean isOutOfProcessHostedModeSupported() {
    return isAtLeast(VERSION_2_0);
  }

  @Override
  public boolean isUseSystemIndependentGwtDevJar() {
    return isAtLeast(VERSION_2_0);
  }

  @Override
  public boolean isUiBinderSupported() {
    return isAtLeast(VERSION_2_0);
  }

  @Override
  public boolean isUiRendererSupported() {
    return isAtLeast(VERSION_2_5);
  }

  @Override
  public String getGwtModuleDocTypeString() {
    if (isAtLeast(VERSION_2_4)) {
      String version = isAtLeast(VERSION_2_8) ? "2.8.0"
                     : isAtLeast(VERSION_2_7) ? "2.7.0"
                     : isAtLeast(VERSION_2_6) ? "2.6.0"
                     : isAtLeast(VERSION_2_5) ? "2.5.0"
                                              : "2.4.0";
      return StringUtil.replace(GWT_MODULE_NEW_DOCTYPE_FORMAT, VERSION_PLACEHOLDER, version);
    } else {
      String version = isAtLeast(VERSION_2_0) ? "2.0" : "1.6";
      return StringUtil.replace(GWT_MODULE_OLD_DOCTYPE_FORMAT, VERSION_PLACEHOLDER, version);
    }
  }

  @Override
  public boolean isBrowserSupportedInDevMode(@NotNull WebBrowser browser) {
    return !WebBrowserManager.isOpera(browser);
  }

  @Override
  public boolean isSuperDevModeSupported() {
    return isAtLeast(VERSION_2_5);
  }

  @Override
  public boolean isLegacyJarForNewSuperDevModeRequired() {
    return isAtLeast(VERSION_2_5) && !isAtLeast(VERSION_2_7);
  }

  @Override
  public boolean isSuperDevModeUsedByDefault() {
    return isAtLeast(VERSION_2_7);
  }

  @Override
  public @NotNull String getCompilerOutputDirParameterName() {
    return isAtLeast(VERSION_1_6) ? "-war" : "-out";
  }

  public static GwtVersionImpl getDefaultVersion() {
    return VERSION_2_5;
  }

  @Override
  public @NotNull LanguageLevel getHighestSupportedLanguageLevel() {
    if (isAtLeast(VERSION_2_11)) {
      return LanguageLevel.JDK_17;
    }
    if (isAtLeast(VERSION_2_10)) {
      return LanguageLevel.JDK_11;
    }
    if (isAtLeast(VERSION_2_8)) {
      return LanguageLevel.JDK_1_8;
    }
    if (isAtLeast(VERSION_2_6)) {
      return LanguageLevel.JDK_1_7;
    }
    return LanguageLevel.JDK_1_6;
  }
}
