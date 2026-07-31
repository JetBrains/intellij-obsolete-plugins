package com.intellij.gwt.maven;

import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import org.jetbrains.annotations.NonNls;

import java.util.List;

import static com.intellij.gwt.sdk.impl.GwtVersionImpl.LATEST;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_1_0;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_0;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_5;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_6;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_7;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_8;

final class GwtCodehausParameter {

  public static final @NonNls List<GwtCodehausParameter> PARAMETERS = List.of(value("logLevel", "-logLevel", VERSION_1_0, LATEST),
                                                                              value("workDir", "-workDir", VERSION_1_0, LATEST),
                                                                              flag("compileReport", "-compileReport", VERSION_2_0, LATEST,
                                                                                   false), // since VERSION_2_1
                                                                              flag("disableCastChecking", "-XdisableCastChecking",
                                                                                   VERSION_1_0, VERSION_2_5, false),
                                                                              flag("disableCastChecking", "-XnocheckCasts", VERSION_2_6,
                                                                                   VERSION_2_7, false),
                                                                              flag("disableCastChecking", "-XcheckCasts", VERSION_2_8,
                                                                                   LATEST, true),
                                                                              flag("disableClassMetadata", "-XdisableClassMetadata",
                                                                                   VERSION_1_0, VERSION_2_5, false),
                                                                              flag("disableClassMetadata", "-XnoclassMetadata", VERSION_2_6,
                                                                                   LATEST, false),
                                                                              flag("draftCompile", "-draftCompile", VERSION_1_0, LATEST,
                                                                                   false),
                                                                              flag("checkAssertions", "-checkAssertions", VERSION_2_6,
                                                                                   LATEST, false),
                                                                              flag("checkAssertions", "-ea", VERSION_1_0, VERSION_2_5,
                                                                                   false),
                                                                              flag("closureCompiler", "-XclosureCompiler", VERSION_2_6,
                                                                                   VERSION_2_8, false), // removed after 2.8.0-beta1
                                                                              flag("closureCompiler", "-XenableClosureCompiler",
                                                                                   VERSION_2_5, VERSION_2_5, false),
                                                                              value("fragmentCount", "-XfragmentCount", VERSION_2_5,
                                                                                    LATEST),
                                                                              value("gen", "-gen", VERSION_1_0, LATEST),
                                                                              flag("generateJsInteropExports", "-generateJsInteropExports",
                                                                                   VERSION_2_8, LATEST, false),
                                                                              value("methodNameDisplayMode", "-XmethodNameDisplayMode",
                                                                                    VERSION_2_7, LATEST),
                                                                              value("optimizationLevel", "-optimize", VERSION_2_0, LATEST),
                                                                              // since VERSION_2_1
                                                                              flag("saveSource", "-saveSource", VERSION_2_6, LATEST, false),
                                                                              flag("failOnError", "-failOnError", VERSION_2_6, LATEST,
                                                                                   false),
                                                                              flag("failOnError", "-strict", VERSION_2_0, VERSION_2_5,
                                                                                   false), // since VERSION_2_1
                                                                              flag("validateOnly", "-validateOnly", VERSION_1_0, LATEST,
                                                                                   false),
                                                                              value("sourceLevel", "-sourceLevel", VERSION_2_6, LATEST),
                                                                              value("localWorkers", "-localWorkers", VERSION_1_0, LATEST),
                                                                              flag("incremental", "-incremental", VERSION_2_7, LATEST,
                                                                                   false),
                                                                              value("war", "-war", VERSION_1_0, LATEST),
                                                                              value("deploy", "-deploy", VERSION_2_0, LATEST),
                                                                              // since VERSION_2_2, not VERSION_2_3
                                                                              value("extra", "-extra", VERSION_1_0, LATEST),
                                                                              value("saveSourceOutput", "-saveSourceOutput", VERSION_2_6,
                                                                                    LATEST),
                                                                              flag("overlappingSourceWarnings",
                                                                                   "-overlappingSourceWarnings", VERSION_1_0, VERSION_2_7,
                                                                                   false),
                                                                              // following parameters are absent in gwt compiler help message
                                                                              flag("compilerMetrics", "-XcompilerMetrics", VERSION_2_0,
                                                                                   LATEST, false), // since VERSION_2_2, not VERSION_2_5
                                                                              flag("soycDetailed", "-XsoycDetailed", VERSION_2_0,
                                                                                   VERSION_2_5, false), // since VERSION_2_1
                                                                              flag("soycDetailed", "-XdetailedSoyc", VERSION_2_6, LATEST,
                                                                                   false),
                                                                              flag("detailedSoyc", "-XsoycDetailed", VERSION_2_0,
                                                                                   VERSION_2_5, false), // since VERSION_2_1
                                                                              flag("detailedSoyc", "-XdetailedSoyc", VERSION_2_6, LATEST,
                                                                                   false),
                                                                              flag("clusterFunctions", "-XnoclusterFunctions", VERSION_2_6,
                                                                                   LATEST, true),
                                                                              flag("disableRunAsync", "-XnocodeSplitting", VERSION_1_0,
                                                                                   VERSION_2_5, false),
                                                                              flag("disableRunAsync", "-XdisableRunAsync", VERSION_2_6,
                                                                                   LATEST, false),
                                                                              flag("enableJsonSoyc", "-XenableJsonSoyc", VERSION_2_7,
                                                                                   LATEST, false),
                                                                              flag("inlineLiteralParameters", "-XnoinlineLiteralParameters",
                                                                                   VERSION_2_6, LATEST, true),
                                                                              value("jsInteropMode", "-XjsInteropMode", VERSION_2_7,
                                                                                    VERSION_2_7), // removed after 2.8.0-beta1
                                                                              value("modulePathPrefix", "-modulePathPrefix", VERSION_2_7,
                                                                                    LATEST), // since VERSION_2_7, not VERSION_1_0
                                                                              value("namespace", "-Xnamespace", VERSION_2_7, LATEST),
                                                                              flag("optimizeDataflow", "-XnooptimizeDataflow", VERSION_2_6,
                                                                                   LATEST, true),
                                                                              flag("ordinalizeEnums", "-XnoordinalizeEnums", VERSION_2_6,
                                                                                   LATEST, true),
                                                                              flag("removeDuplicateFunctions",
                                                                                   "-XnoremoveDuplicateFunctions", VERSION_2_6, LATEST,
                                                                                   true));

  public static final List<GwtCodehausParameter> PROPERTIES =
    List.of(property("persistentunitcache", "gwt.persistentunitcache", VERSION_2_5, LATEST),
            property("persistentunitcachedir", "gwt.persistentunitcachedir", VERSION_2_5, LATEST));

  private final String myTagName;
  private final String myLabel;
  private final GwtVersionImpl mySinceVersion;
  private final GwtVersionImpl myUntilVersion;
  private final boolean myIsFlag;
  private final boolean myDefaultValue;
  private final boolean myIsProperty;

  private GwtCodehausParameter(@NonNls String tagName, @NonNls String label, GwtVersionImpl sinceVersion, GwtVersionImpl untilVersion,
                               boolean isFlag, boolean defaultValue, boolean isProperty) {
    myTagName = tagName;
    myLabel = label;
    mySinceVersion = sinceVersion;
    myUntilVersion = untilVersion;
    myIsFlag = isFlag;
    myDefaultValue = defaultValue;
    myIsProperty = isProperty;
  }

  private static GwtCodehausParameter flag(@NonNls String tagName, @NonNls String label, GwtVersionImpl since, GwtVersionImpl until, boolean defaultValue) {
    return new GwtCodehausParameter(tagName, label, since, until, true, defaultValue, false);
  }

  private static GwtCodehausParameter value(@NonNls String tagName, @NonNls String label, GwtVersionImpl since, GwtVersionImpl until) {
    return new GwtCodehausParameter(tagName, label, since, until, false, false, false);
  }

  private static GwtCodehausParameter property(@NonNls String tagName, @NonNls String label, GwtVersionImpl since, GwtVersionImpl until) {
    return new GwtCodehausParameter(tagName, label, since, until, false, false, true);
  }

  public void append(GwtVersionImpl version, @NonNls List<String> compilerParameters, String value) {
    if (value != null) {
      if (version.isAtLeast(mySinceVersion) && myUntilVersion.isAtLeast(version)) {
        if (myIsFlag) {
          if (Boolean.parseBoolean(value) != myDefaultValue) {
            compilerParameters.add(myLabel);
          }
        } else if (myIsProperty) {
          compilerParameters.add("-D" + myLabel + "=" + value);
        } else {
          compilerParameters.add(myLabel);
          compilerParameters.add(value);
        }
      }
    }
  }

  public String getTagName() {
    return myTagName;
  }
}
