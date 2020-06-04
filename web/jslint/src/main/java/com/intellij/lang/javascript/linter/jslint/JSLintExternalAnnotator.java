package com.intellij.lang.javascript.linter.jslint;

import com.google.common.base.Supplier;
import com.google.common.io.CharStreams;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.javascript.linter.*;
import com.intellij.lang.javascript.linter.jshint.JSHintExternalAnnotator;
import com.intellij.lang.javascript.linter.rhino.FunctionWithScope;
import com.intellij.lang.javascript.linter.rhino.RhinoFunctionManager;
import com.intellij.lang.javascript.linter.rhino.RhinoUtil;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.util.JSUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JSLintExternalAnnotator extends JSLinterExternalAnnotator<JSLintState> {

  private static final Logger LOG = Logger.getInstance(JSLintExternalAnnotator.class);
  private static final JSLintExternalAnnotator INSTANCE_FOR_BATCH_INSPECTION = new JSLintExternalAnnotator(false);

  public static final RhinoFunctionManager JSLINT_FUNCTION_MANAGER = new RhinoFunctionManager(
    new Supplier<String>() {
      @Override
      public String get() {
        String fileName = "/data/jslint.js";
        InputStream inputStream = getClass().getResourceAsStream(fileName);
        if (inputStream == null) {
          throw new RuntimeException("Resource " + fileName + " is not found!");
        }
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        try {
          return CharStreams.toString(reader);
        }
        catch (IOException e) {
          throw new RuntimeException("Can't read " + fileName + "!", e);
        } finally {
          try {
            reader.close();
          } catch (IOException ignored) {
          }
        }
      }
    },
    "jslint",
    null
  );

  @NotNull
  public static JSLintExternalAnnotator getInstanceForBatchInspection() {
    return INSTANCE_FOR_BATCH_INSPECTION;
  }

  @SuppressWarnings("unused")
  public JSLintExternalAnnotator() {
    this(true);
  }

  public JSLintExternalAnnotator(boolean onTheFly) {
    super(onTheFly);
  }

  @NotNull
  @Override
  protected JSLinterConfigurable<JSLintState> createSettingsConfigurable(@NotNull Project project) {
    return new JSLintConfigurable(project);
  }

  @Override
  protected Class<? extends JSLinterConfiguration<JSLintState>> getConfigurationClass() {
    return JSLintConfiguration.class;
  }

  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return JSLintInspection.class;
  }

  @Override
  protected boolean acceptPsiFile(@NotNull PsiFile file) {
    return file instanceof JSFile && JSUtils.isJavaScriptFile(file);
  }

  @Override
  public JSLinterAnnotationResult annotate(@NotNull JSLinterInput<JSLintState> collectedInfo) {
    Context cx = Context.enter();
    try {
      FunctionWithScope functionWithScope = JSLINT_FUNCTION_MANAGER.getFunctionWithScope();
      Function function = functionWithScope.getFunction();
      Scriptable scope = functionWithScope.getScope();

      JSLintState state = collectedInfo.getState();
      JSLintOptionsState optionsState = state.getOptionsState();
      NativeObject optionsNativeObject = convertOptionsToNativeObject(optionsState);
      String globals = StringUtil.notNullize(ObjectUtils.tryCast(optionsState.getValue(JSLintOption.GLOBALS), String.class));
      NativeArray globalsNativeArray = JSHintExternalAnnotator.convertPredefStrToNativeArray(globals, scope);
      Object[] args = {collectedInfo.getFileContent(), optionsNativeObject, globalsNativeArray};
      try {
        Object result = function.call(cx, scope, scope, args);
        if (result instanceof NativeObject) {
          NativeObject resultObj = (NativeObject)result;
          Object warnings = resultObj.get("warnings", scope);
          if (warnings instanceof NativeArray) {
            List<JSLinterError> errors = convertErrors((NativeArray)warnings);
            return JSLinterAnnotationResult.createLinterResult(collectedInfo, errors, null);
          }
          return JSLinterAnnotationResult.createLinterResult(collectedInfo, Collections.emptyList(), null);
        }
        else {
          LOG.warn("jslint unexpectedly returned " + (result != null ? result.getClass() : null));
        }
      }
      catch (JavaScriptException e) {
        if (!tryPrintStack(e.getValue())) {
          LOG.warn(e);
        }
      }
      return null;
    } finally {
      Context.exit();
    }
  }

  @NotNull
  private static List<JSLinterError> convertErrors(@NotNull NativeArray errorsNativeArray) {
    List<JSLinterError> errors = new ArrayList<>(errorsNativeArray.size());
    for (Object errorObj : errorsNativeArray) {
      if (errorObj instanceof NativeObject) {
        JSLinterError error = toLinterError((NativeObject)errorObj);
        if (error != null) {
          errors.add(error);
        }
      }
      else {
        tryPrintStack(errorObj);
      }
    }
    return errors;
  }

  private static boolean tryPrintStack(@Nullable Object errorObj) {
    if (errorObj instanceof ScriptableObject && "NativeError".equals(errorObj.getClass().getSimpleName())) {
      ScriptableObject nativeError = (ScriptableObject)errorObj;
      LOG.warn("Failed to run jslint: " + RhinoUtil.getStringKey(nativeError, "message") + "\n"
               + RhinoUtil.getStringKey(nativeError, "stack"));
      return true;
    }
    LOG.warn("Unexpected error of type " + (errorObj != null ? errorObj.getClass() : null));
    return false;
  }

  @Nullable
  private static JSLinterError toLinterError(@NotNull NativeObject nativeError) {
    int line = toInt(nativeError.get("line"));
    int character = toInt(nativeError.get("column"));
    if (line < 0 || character < 0) {
      return null;
    }
    String reason = RhinoUtil.getStringKey(nativeError, "message");
    if (reason != null) {
      return new JSLinterError(line + 1, character + 1, reason, null);
    }
    return null;
  }

  private static int toInt(Object obj) {
    if (obj instanceof Number) {
      return ((Number) obj).intValue();
    }
    return -1;
  }

  @NotNull
  private static NativeObject convertOptionsToNativeObject(@NotNull JSLintOptionsState optionsState) {
    NativeObject object = new NativeObject();
    for (JSLintOption option : optionsState.getOptions()) {
      final Object value = optionsState.getValue(option);
      if (option != JSLintOption.GLOBALS && value != null) {
        final Object nativeObj;
        if (value instanceof Boolean || value instanceof String || value instanceof Number) {
          nativeObj = value;
        } else {
          throw new RuntimeException();
        }
        object.defineProperty(option.getOptionName(), nativeObj, ScriptableObject.READONLY);
      }
    }
    return object;
  }

  @Override
  public void apply(@NotNull PsiFile file, JSLinterAnnotationResult annotationResult, @NotNull AnnotationHolder holder) {
    if (annotationResult == null) return;
    new JSLinterAnnotationsBuilder(file, annotationResult, holder,
                                   new JSLintConfigurable(file.getProject()), "JSLint: ", getInspectionClass(), new JSLinterStandardFixes())
      .setHighlightingGranularity(HighlightingGranularity.element)
      .apply();
  }
}
