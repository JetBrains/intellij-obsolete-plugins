package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.javascript.linter.JSLinterConfiguration;
import com.intellij.lang.javascript.linter.JSLinterInspection;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jdom.Text;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Sergey Simonchik
 */
@State(name = "JSClosureLinterConfiguration", storages = @Storage("jsLinters/gjslint.xml"))
public class GjsLintConfiguration extends JSLinterConfiguration<GjsLintState> {

  public static final String DEFAULT_EXE_FILE_BASE_NAME = SystemInfo.isWindows ? "gjslint.exe" : "gjslint";
  private static final String LINTER_EXE_FILE_PATH = "linter-exe-file-path";
  private static final String ROOT_ELEMENT_NAME = "gjslint";
  private static final String CONFIG_FILE_PATH = "config-file";

  private volatile GjsLintState DEFAULT_STATE;

  public GjsLintConfiguration(@NotNull Project project) {
    super(project);
  }

  @Override
  protected void savePrivateSettings(@NotNull GjsLintState state) {
  }

  @NotNull
  @Override
  protected GjsLintState loadPrivateSettings(@NotNull GjsLintState state) {
    return state;
  }

  @NotNull
  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return GjsLintInspection.class;
  }

  @NotNull
  public static GjsLintConfiguration getInstance(@NotNull Project project) {
    return JSLinterConfiguration.getInstance(project, GjsLintConfiguration.class);
  }

  @NotNull
  @Override
  protected Element toXml(@NotNull GjsLintState state) {
    Element root = new Element(ROOT_ELEMENT_NAME);
    String configFilePath = state.getConfigFilePath();
    Element child = new Element(CONFIG_FILE_PATH);
    child.setContent(new Text(FileUtil.toSystemIndependentName(configFilePath)));
    root.addContent(child);
    storeLinterExeFilePath(state.getLinterExePath());
    return root;
  }

  private static void storeLinterExeFilePath(@NotNull String linterExeFilePath) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    propertiesComponent.setValue(LINTER_EXE_FILE_PATH, linterExeFilePath);
  }

  @NotNull
  private static String restoreLinterExeFilePath() {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    return StringUtil.notNullize(propertiesComponent.getValue(LINTER_EXE_FILE_PATH));
  }

  @NotNull
  @Override
  protected GjsLintState fromXml(@NotNull Element element) {
    GjsLintState.Builder builder = new GjsLintState.Builder();
    Element configFileElement = element.getChild(CONFIG_FILE_PATH);
    String configFilePath = "";
    if (configFileElement != null) {
      configFilePath = FileUtil.toSystemDependentName(configFileElement.getText());
    }
    builder.setConfigFilePath(configFilePath);
    String linterExeFilePath = restoreLinterExeFilePath();
    if (linterExeFilePath.isEmpty()) {
      linterExeFilePath = getDefaultState().getLinterExePath();
    }
    builder.setLinterExePath(linterExeFilePath);
    return builder.build();
  }

  @NotNull
  @Override
  protected GjsLintState getDefaultState() {
    GjsLintState state = DEFAULT_STATE;
    if (state == null) {
      GjsLintState.Builder builder = new GjsLintState.Builder();
      File exeFile = PathEnvironmentVariableUtil.findInPath(DEFAULT_EXE_FILE_BASE_NAME);
      if (exeFile != null) {
        builder.setLinterExePath(exeFile.getAbsolutePath());
      }
      state = builder.build();
      DEFAULT_STATE = state;
    }
    return state;
  }

}
