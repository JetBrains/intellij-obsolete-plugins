package org.jetbrains.jps.gwt.build;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.CustomBuilderMessage;

public final class GwtBuilderMessages {
  public static final String BUILDER_ID = "GWT";
  public static final @NonNls String LOGGING_STARTED_TYPE = "!start!";
  public static final @NonNls String LOGGING_FINISHED_TYPE = "!finish!";
  public static final @NonNls String COMPILE_REPORT_PREFIX = "!compile-report!";

  public static void sendCompilationStartedMessage(CompileContext context, JpsGwtModule module) {
    context.processMessage(new CustomBuilderMessage(BUILDER_ID, LOGGING_STARTED_TYPE, module.getQualifiedName()));
  }

  public static void sendCompilationFinishedMessage(CompileContext context, JpsGwtModule module) {
    context.processMessage(new CustomBuilderMessage(BUILDER_ID, LOGGING_FINISHED_TYPE, module.getQualifiedName()));
  }

  public static void sendLogMessage(CompileContext context, JpsGwtModule module, String text) {
    context.processMessage(new CustomBuilderMessage(BUILDER_ID, module.getQualifiedName(), text));
  }

  public static void sendCompileReportGenerated(CompileContext context, JpsGwtModule module, String reportPath) {
    context.processMessage(new CustomBuilderMessage(BUILDER_ID, COMPILE_REPORT_PREFIX + module.getQualifiedName(), reportPath));
  }
}
