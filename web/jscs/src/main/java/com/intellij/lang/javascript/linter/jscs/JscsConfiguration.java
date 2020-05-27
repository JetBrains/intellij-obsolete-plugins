package com.intellij.lang.javascript.linter.jscs;

import com.intellij.javascript.nodejs.util.JSLinterPackage;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.lang.javascript.linter.JSLinterConfiguration;
import com.intellij.lang.javascript.linter.JSLinterInspection;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author by Irina.Chernushina on 9/22/2014.
 */
@State(name = "JscsConfiguration", storages = @Storage("jsLinters/jscs.xml"))
public class JscsConfiguration extends JSLinterConfiguration<JscsState> {
  public static final NotificationGroup IMPORT_CONFIG_NOTIFICATION = NotificationGroup.logOnlyGroup("JSCS Config Import");

  public static final String LOG_CATEGORY = "#com.intellij.lang.javascript.linter.jscs.Jscs";
  private static final String JSCS_ELEMENT_NAME = "jscs";
  private static final String IS_CUSTOM_CONFIG_FILE_USED_ATTRIBUTE_NAME = "use-custom-config-file";
  private static final String CUSTOM_CONFIG_FILE_PATH_ATTRIBUTE_NAME = "custom-config-file-path";
  private static final String PRESET = "jscs-preset";

  private final JSLinterPackage myPackage;

  private JscsState DEFAULT_STATE;

  public JscsConfiguration(@NotNull Project project) {
    super(project);
    myPackage = new JSLinterPackage(project, "jscs");
  }

  @Override
  protected void savePrivateSettings(@NotNull JscsState state) {
    storeLinterLocalPaths(state);
  }

  @NotNull
  @Override
  public JscsState loadPrivateSettings(@NotNull JscsState state) {
    final JscsState.Builder builder = new JscsState.Builder(state);
    restoreLinterLocalPaths(builder);
    return builder.build();
  }

  @NotNull
  public static JscsConfiguration getInstance(@NotNull final Project project) {
    return JSLinterConfiguration.getInstance(project, JscsConfiguration.class);
  }

  @NotNull
  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return JscsInspection.class;
  }

  @NotNull
  @Override
  protected Element toXml(@NotNull JscsState state) {
    final Element root = new Element(JSCS_ELEMENT_NAME);
    if (state.isCustomConfigFileUsed()) {
      root.setAttribute(IS_CUSTOM_CONFIG_FILE_USED_ATTRIBUTE_NAME, Boolean.TRUE.toString());
    }
    final String customConfigFilePath = state.getCustomConfigFilePath();
    if (! StringUtil.isEmptyOrSpaces(customConfigFilePath)) {
      root.setAttribute(CUSTOM_CONFIG_FILE_PATH_ATTRIBUTE_NAME, FileUtil.toSystemIndependentName(customConfigFilePath));
    }
    final JscsPreset preset = state.getPreset();
    if (preset != null) {
      root.setAttribute(PRESET, preset.name());
    }
    storeLinterLocalPaths(state);
    return root;
  }

  @NotNull
  @Override
  protected JscsState fromXml(@NotNull Element element) {
    final JscsState.Builder builder = new JscsState.Builder();
    builder.setCustomConfigFileUsed(Boolean.parseBoolean(element.getAttributeValue(IS_CUSTOM_CONFIG_FILE_USED_ATTRIBUTE_NAME)));
    String customConfigFilePath = StringUtil.notNullize(element.getAttributeValue(CUSTOM_CONFIG_FILE_PATH_ATTRIBUTE_NAME));
    builder.setCustomConfigFilePath(FileUtil.toSystemDependentName(customConfigFilePath));
    final String presetName = element.getAttributeValue(PRESET);
    if (! StringUtil.isEmptyOrSpaces(presetName)) {
      builder.setPreset(JscsPreset.safeValueOf(presetName));
    }
    restoreLinterLocalPaths(builder);
    return builder.build();
  }

  private void storeLinterLocalPaths(@NotNull JscsState state) {
    myPackage.force(state.getInterpreterRef(), state.getPackagePath());
  }

  private void restoreLinterLocalPaths(JscsState.Builder builder) {
    myPackage.readOrDetect();
    builder.setNodePath(myPackage.getInterpreter());
    NodePackage nodePackage = myPackage.getPackage().getConstantPackage();
    assert nodePackage != null : " Jscs does not support non-constant node package refs";
    builder.setNodePackage(nodePackage);
  }

  @NotNull
  @Override
  protected JscsState getDefaultState() {
    JscsState state = DEFAULT_STATE;
    if (state != null) {
      return state;
    }
    final JscsState.Builder builder = new JscsState.Builder();
    builder.setCustomConfigFileUsed(false);

    state = builder.build();
    DEFAULT_STATE = state;
    return state;
  }
}
