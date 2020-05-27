package com.intellij.lang.javascript.linter.jscs.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings;
import com.intellij.lang.javascript.linter.NavigateToPropertyInConfig;
import com.intellij.lang.javascript.linter.jscs.JscsConfiguration;
import com.intellij.lang.javascript.linter.jscs.JscsPreset;
import com.intellij.lang.javascript.linter.jscs.config.JscsOption;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Irina.Chernushina on 4/22/2015.
 */
public class JscsSchemeImportWorker {
  private Map<JscsOption, ImportRule> myRulesMap;
  @NotNull
  private final Project myProject;
  @NotNull
  private final VirtualFile myFile;
  private final Map<JscsOption, VirtualFile> myRulesSources;
  private final Set<String> myUnknown;
  private HashMap<JscsOption, PairImportRule> myPairRules;

  public JscsSchemeImportWorker(@NotNull Project project, @NotNull VirtualFile selectedFile) {
    myProject = project;
    myFile = selectedFile;
    myRulesSources = new HashMap<>();
    myUnknown = new HashSet<>();

  }

  @Nullable
  public CodeStyleScheme importScheme(final Getter<CodeStyleScheme> factory, JscsPreset preset, String jscsPath) throws SchemeImportException {
    final Map<JscsOption, JsonElement> map = combineConfigs(preset, jscsPath);
    if (map.isEmpty()) {
      final String message = "JSCS scheme was not imported from '" + jscsPath + "': no rules were found.";
      JscsConfiguration.IMPORT_CONFIG_NOTIFICATION.createNotification(message, NotificationType.INFORMATION).setImportant(false).notify(myProject);
      throw new SchemeImportException(message).setWarning();
    }

    final CodeStyleScheme scheme = factory.get();
    final CodeStyleSettings settings = scheme.getCodeStyleSettings();
    final CommonCodeStyleSettings languageSettings = settings.getCommonSettings(JavascriptLanguage.INSTANCE);
    final JSCodeStyleSettings jsCodeStyleSettings = settings.getCustomSettings(JSCodeStyleSettings.class);

    final Map<JscsOption, AppliedRule> applied = new HashMap<>();
    final Set<JscsOption> skipped = EnumSet.noneOf(JscsOption.class);

    for (Map.Entry<JscsOption, JsonElement> entry : map.entrySet()) {
      final ImportRule rule = myRulesMap.get(entry.getKey());
      if (rule != null && rule.apply(entry.getValue(), languageSettings, jsCodeStyleSettings)) {
        applied.put(entry.getKey(), rule);
      } else {
        skipped.add(entry.getKey());
      }
    }
    for (Map.Entry<JscsOption, PairImportRule> entry : myPairRules.entrySet()) {
      final PairImportRule rule = entry.getValue();
      final JscsOption pairRule = rule.getPairRule();
      final JscsOption option = entry.getKey();

      if (skipped.contains(option) && skipped.contains(pairRule)) {
        if (rule.apply(map.get(option), map.get(pairRule), languageSettings, jsCodeStyleSettings)) {
          skipped.remove(option);
          skipped.remove(pairRule);
          applied.put(option, rule);
        }
      }
    }
    showReport(scheme.getName(), applied, new ArrayList<>(skipped));
    return scheme;
  }

  private Map<JscsOption, JsonElement> combineConfigs(JscsPreset preset, String jscsPath) throws SchemeImportException {
    myRulesMap = new HashMap<>();
    myPairRules = new HashMap<>();
    JscsRulesForCodeStyle.fillRules(myRulesMap, myPairRules);

    final Map<JscsOption, JsonElement> map = new HashMap<>();
    if (preset != null) {
      if (StringUtil.isEmptyOrSpaces(jscsPath)) throw new SchemeImportException("Path to JSCS not provided");
      File file = new File(jscsPath);
      file = !file.isAbsolute() ? new File(myProject.getBasePath(), jscsPath) : file;
      final File presetFile = new File(file, "presets/" + preset.name() + ".json");
      if (! presetFile.exists()) throw new SchemeImportException("Can not find " + preset.getDisplayName() + " file in " + presetFile.getPath());
      final VirtualFile presetVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(presetFile);
      if (presetVf == null) throw new SchemeImportException("Can not find " + preset.getDisplayName() + " file in " + presetFile.getPath());
      map.putAll(readConfig(presetVf));
      for (JscsOption option : map.keySet()) {
        myRulesSources.put(option, presetVf);
      }
    }
    final Map<JscsOption, JsonElement> configMap = readConfig(myFile);
    map.putAll(configMap);
    for (JscsOption option : configMap.keySet()) {
      myRulesSources.put(option, myFile);
    }
    return map;
  }

  private Map<JscsOption, JsonElement> readConfig(@NotNull final VirtualFile file) throws SchemeImportException {
    final String json = loadJson(file);
    JsonObject object;
    try {
      final JsonElement rootElement = new JsonParser().parse(json);
      object = rootElement == null || !rootElement.isJsonObject() ? null : rootElement.getAsJsonObject();
    } catch (JsonParseException e) {
      object = null;
    }
    if (object == null) {
      canNotParse(file);
      return null;
    }
    final Set<Map.Entry<String, JsonElement>> properties = object.entrySet();
    final Iterator<Map.Entry<String, JsonElement>> iterator = properties.iterator();
    final Map<JscsOption, JsonElement> map = new HashMap<>();
    while (iterator.hasNext()) {
      final Map.Entry<String, JsonElement> next = iterator.next();
      final JscsOption option = JscsOption.safeValueOf(next.getKey());
      if (option == null) {
        myUnknown.add(next.getKey());
      } else {
        map.put(option, next.getValue());
      }
    }
    return map;
  }

  private static void canNotParse(@NotNull final VirtualFile file) throws SchemeImportException {
    throw new SchemeImportException("Can not import from JSCS config file (" + file.getPath() + "): can not parse file.");
  }

  private static String loadJson(@NotNull VirtualFile selectedFile) throws SchemeImportException {
    try {
      return VfsUtilCore.loadText(selectedFile);
    }
    catch (IOException e) {
      throw new SchemeImportException(e);
    }
  }

  private void showReport(String schemeName,
                          final Map<JscsOption, AppliedRule> applied,
                          final List<JscsOption> skipped) {
    final StringBuilder sb = new StringBuilder("<b>JSCS: ").append(myFile.getName())
      .append(" (").append(myFile.getParent().getPath()).append(") successfully imported into '").append(schemeName).append("' scheme.</b><br/><br/>");
    createReportContent(applied, skipped, myUnknown, sb);
    JscsConfiguration.IMPORT_CONFIG_NOTIFICATION.
      createNotification("", sb.toString(), NotificationType.INFORMATION, new NotificationListener() {
        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
          final String description = event.getDescription();
          if (description.startsWith("#")) {
            final JscsOption option = JscsOption.safeValueOf(description.substring(1));
            if (option != null) {
              VirtualFile source = myRulesSources.get(option);
              assert source != null;
              if (source != null) {
                NavigateToPropertyInConfig.navigate(myProject, source, description.substring(1));
              }
            }
          }
        }
      })
      .setImportant(false)
      .notify(myProject);
  }

  private static void createReportContent(final Map<JscsOption, AppliedRule> applied,
                                          final List<JscsOption> skipped,
                                          final Set<String> unknownSet,
                                          final StringBuilder sb) {
    if (applied.isEmpty() && skipped.isEmpty() && unknownSet.isEmpty()) {
      sb.append("No rules were found.");
      return;
    }
    boolean contentBefore = false;
    if (! applied.isEmpty()) {
      reportApplied(applied, sb);
      contentBefore = true;
    }
    if (! skipped.isEmpty()) {
      if (contentBefore) sb.append("<br/><br/>");
      Collections.sort(skipped);
      sb.append("Skipped: ");
      sb.append(StringUtil.join(skipped, option -> ruleUrl(option), ", "));
    }
    if (! unknownSet.isEmpty()) {
      final List<String> unknown = new ArrayList<>(unknownSet);
      if (contentBefore) sb.append("<br/><br/>");
      Collections.sort(unknown);
      sb.append("Unknown:<br/>");
      sb.append(StringUtil.join(unknown, "<br/>"));
    }
  }

  private static void reportApplied(Map<JscsOption, AppliedRule> applied, StringBuilder sb) {
    final List<JscsOption> options = new ArrayList<>(applied.keySet());
    Collections.sort(options);
    sb.append("Applied rules:<br/><br/>");
    for (JscsOption option : options) {
      final AppliedRule rule = applied.get(option);
      sb.append("<br/>").
        append(ruleUrl(option)).
        append(":<br/>").
        append(rule.getResultDescription()).
        append("<br/><br/>").
        append("(from ").append(rule.getTextValue()).append(") ");
    }
  }

  private static String ruleUrl(final JscsOption option) {
    return "<a href='#" + option.name() + "'>" + option.name() + "</a>";
  }
}
