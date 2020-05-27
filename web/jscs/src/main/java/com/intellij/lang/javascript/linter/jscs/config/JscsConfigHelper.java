package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.json.psi.*;
import com.intellij.lang.javascript.linter.jscs.JscsConfiguration;
import com.intellij.lang.javascript.linter.jscs.JscsState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Irina.Chernushina on 10/16/2014.
 */
public class JscsConfigHelper {
  private static final Logger LOG = Logger.getInstance(JscsConfiguration.LOG_CATEGORY);
  public final static String CONFIG_JSCS_JSON = "config.jscs.json";

  public static List<String> getExcludedPaths(@NotNull final Project project, @NotNull final Document document) {
    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (psiFile == null) return null;

    return getExcludedPaths(psiFile);
  }

  public static List<String> getExcludedPaths(PsiFile psiFile) {
    final PsiElement child = psiFile.getFirstChild();

    final JsonValue excludeValue = ObjectUtils.doIfCast(child, JsonObject.class, jsonObject -> {
      final List<JsonProperty> list = jsonObject.getPropertyList();
      for (JsonProperty property : list) {
        if (JscsOption.excludeFiles.name().equals(property.getName())) {
          return property.getValue();
        }
      }
      return null;
    });

    if (excludeValue == null) return null;

    return ObjectUtils.doIfCast(excludeValue, JsonArray.class, array -> {
      final List<String> result = new ArrayList<>();
      final List<JsonValue> valueList = array.getValueList();
      for (JsonValue jsonValue : valueList) {
        final String path = ObjectUtils.doIfCast(jsonValue, JsonStringLiteral.class, str -> ReadAction
          .compute(() -> StringUtil.stripQuotesAroundValue(str.getText())));
        if (path != null) {
          result.add(path);
        }
      }
      return result.isEmpty() ? null : result;
    });
  }

  @Nullable
  public static VirtualFile createConfigBasedOnPreset(@NotNull final Project project) {
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    final VirtualFile dir = project.getBaseDir();
    final File configFile = new File(FileUtil.toSystemDependentName(dir.getPath()), CONFIG_JSCS_JSON);
    if (configFile.exists()) {
      return lfs.refreshAndFindFileByIoFile(configFile);
    }
    final JscsState jscsState = JscsConfiguration.getInstance(project).getExtendedState().getState();
    if (jscsState.getPreset() != null && ! StringUtil.isEmptyOrSpaces(jscsState.getPackagePath())) {
      final File presetsDir = new File(jscsState.getPackagePath(), "/presets");
      if (presetsDir.exists()) {
        final File preset = new File(presetsDir, jscsState.getPreset().getCode() + ".json");
        final VirtualFile vfPreset = lfs.refreshAndFindFileByIoFile(preset);
        if (vfPreset != null && ! vfPreset.isDirectory()) {
          try {
            return VfsUtilCore.copyFile(JscsConfigHelper.class, vfPreset, project.getBaseDir(), CONFIG_JSCS_JSON);
          } catch (IOException e) {
            LOG.info(e);
          }
        }
      }
    }
    try {
      final VirtualFile file = project.getBaseDir().createChildData(JscsConfigHelper.class, CONFIG_JSCS_JSON);
      file.setBinaryContent("{\n}\n".getBytes(StandardCharsets.UTF_8));
      return file;
    }
    catch (IOException e) {
      LOG.info(e);
    }
    return null;
  }
}
