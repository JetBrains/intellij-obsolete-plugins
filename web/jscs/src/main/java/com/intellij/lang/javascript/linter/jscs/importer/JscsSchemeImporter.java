package com.intellij.lang.javascript.linter.jscs.importer;

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.javascript.nodejs.util.NodePackageField;
import com.intellij.lang.javascript.linter.JSLinterUtil;
import com.intellij.lang.javascript.linter.NodeModuleConfigurationView;
import com.intellij.lang.javascript.linter.jscs.JscsConfiguration;
import com.intellij.lang.javascript.linter.jscs.JscsBundle;
import com.intellij.lang.javascript.linter.jscs.JscsPreset;
import com.intellij.lang.javascript.linter.jscs.JscsView;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.Alarm;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Irina.Chernushina on 4/20/2015.
 */
public class JscsSchemeImporter implements SchemeImporter<CodeStyleScheme> {
  @Override
  public String @NotNull [] getSourceExtensions() {
    return new String[]{"jscsrc", "json"};
  }

  @Nullable
  @Override
  public CodeStyleScheme importScheme(@NotNull Project project,
                                      @NotNull VirtualFile selectedFile,
                                      @NotNull CodeStyleScheme currentScheme,
                                      @NotNull SchemeFactory<CodeStyleScheme> newSchemeFactory) throws SchemeImportException {
    final OptionsRetriever retriever = new OptionsRetriever();
    final Getter<CodeStyleScheme> schemeFactory = retriever.showOptionsDialog(project, currentScheme, newSchemeFactory);
    if (schemeFactory == null) return null;
    final JscsSchemeImportWorker worker = new JscsSchemeImportWorker(project, selectedFile);
    return worker.importScheme(schemeFactory, retriever.getPreset(), retriever.getPackagePath());
  }

  @Nullable
  @Override
  public String getAdditionalImportInfo(@NotNull CodeStyleScheme scheme) {
    return "<br/>See details in Event Log";
  }

  private static class OptionsRetriever {
    private JscsPreset myPreset;
    private String myPackagePath;

    private Getter<CodeStyleScheme> showOptionsDialog(Project project,
                                                      final CodeStyleScheme currentScheme,
                                                      final SchemeFactory<CodeStyleScheme> schemeFactory) {
      final ComponentWithBrowseButton<ComboBox> presets = JscsView.createPresetWithHelpButton();

      final JEditorPane
        presetHintComp = JSLinterUtil.createHtmlViewer(JscsBundle.message("jscs.configurable.preset.hint.text"), UIUtil.getTitledBorderFont());
      final JPanel presetHint = SwingHelper.wrapWithHorizontalStretch(presetHintComp);

      final FormBuilder formBuilder = FormBuilder.createFormBuilder();
      final JBTextField schemeName = new JBTextField("JSCS");
      final JBRadioButton useCurrent;
      if (currentScheme.isDefault()) {
        useCurrent = null;
        formBuilder.addLabeledComponent(JscsBundle.message("label.create.new.scheme"), schemeName);
      } else {
        useCurrent = new JBRadioButton(JscsBundle.message("radio.update.current.scheme", currentScheme.getName()));
        final JBRadioButton createNew = new JBRadioButton(JscsBundle.message("radio.create.new.scheme"));
        final ButtonGroup group = new ButtonGroup();
        group.add(useCurrent);
        group.add(createNew);
        createNew.setSelected(true);
        formBuilder.addComponent(useCurrent)
          .addLabeledComponent(createNew, schemeName)
          .setFormLeftIndent(0)
          .addVerticalGap(5);
      }
      final JscsConfiguration jscsConfiguration = JscsConfiguration.getInstance(project);
      final NodeJsInterpreterRef nodePath = jscsConfiguration.getExtendedState().getState().getInterpreterRef();
      final NodeModuleConfigurationView configurationView = new NodeModuleConfigurationView(project, "jscs", nodePath);
      final NodeJsInterpreterField nodeField = configurationView.getNodeInterpreterField();
      final NodePackageField packageField = configurationView.getPackageField();
      final NodePackage jscs = NodePackage.findDefaultPackage(project, "jscs", nodePath.resolve(project));
      if (jscs != null) packageField.setSelected(jscs);
      configurationView.setPreferredWidthToComponents();

      formBuilder.addLabeledComponent(NodeJsInterpreterField.getLabelTextForComponent(), nodeField)
        .addLabeledComponent(JscsBundle.message("jscs.configurable.label.package.path"), packageField);

      formBuilder.addLabeledComponent(JscsBundle.message("jscs.configurable.label.preset.label.text"), presets)
        .addLabeledComponent("", presetHint);

      final DialogBuilder builder = new DialogBuilder();
      builder.setTitle(JscsBundle.message("dialog.title.import.from.jscs.config"));
      builder.setNorthPanel(formBuilder.getPanel());
      builder.setDimensionServiceKey(JscsSchemeImportWorker.class.getName());
      builder.setPreferredFocusComponent(schemeName);

      final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
      try {
        final Runnable validation = new Runnable() {
          @Override
          public void run() {
            final DialogWrapper dialogWrapper = builder.getDialogWrapper();
            try {
              if (dialogWrapper == null) return;
              if (presets.getChildComponent().getSelectedItem() != null &&
                  (StringUtil.isEmptyOrSpaces(packageField.getSelected().getSystemDependentPath()))) {
                builder.okActionEnabled(false);
                builder.setErrorText(JscsBundle.message("dialog.message.jscs.package.field.should.be.filled.if.preset.selected"), presets);
              } else {
                builder.okActionEnabled(true);
                builder.setErrorText(null);
              }
            } finally {
              if (!alarm.isDisposed()) {
                alarm.addRequest(this, 200, true);
              }
            }
          }
        };
        alarm.addRequest(validation, 200, ModalityState.any());
        if (!builder.showAndGet()) return null;
      } finally {
        alarm.cancelAllRequests();
        Disposer.dispose(alarm);
      }

      myPreset = (JscsPreset)presets.getChildComponent().getSelectedItem();
      myPackagePath = packageField.getSelected().getSystemDependentPath();

      return () -> {
        if (useCurrent != null && useCurrent.isSelected()) return currentScheme;
        final String text = schemeName.getText();
        return schemeFactory.createNewScheme(StringUtil.isEmptyOrSpaces(text) ? null : text);
      };
    }

    public JscsPreset getPreset() {
      return myPreset;
    }

    public String getPackagePath() {
      return myPackagePath;
    }
  }
}
