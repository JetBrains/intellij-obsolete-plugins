package com.intellij.play.console;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.play.utils.PlayBundle;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayConfigurable extends BaseConfigurable implements SearchableConfigurable {
  private JPanel myPanel;
  private TextFieldWithBrowseButton myField;
  private JLabel myHomePathLabel;
  private HyperlinkLabel myDownloadLink;
  private JBCheckBox myShowOnStartup;
  private TextFieldWithBrowseButton myPathField;
  private final Project myProject;

  protected PlayConfigurable(Project project) {
    myProject = project;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return getTitle();
  }

  @NotNull
  @Override
  public String getHelpTopic() {
    return "play.paths.configuration";
  }

  @Override
  public JComponent createComponent() {
    myField.addBrowseFolderListener(getTitle(), PlayBundle.message("play.installation.folder.choose.message"), myProject,
                                    FileChooserDescriptorFactory.createSingleFolderDescriptor());
    myField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        setModified(!myField.getText().equals(PlayConfiguration.getConfiguration().myPlayHome));
      }
    });

    myPathField.addBrowseFolderListener(getTitle(), PlayBundle.message("play.working.directory.choose.message"), myProject,
                                        FileChooserDescriptorFactory.createSingleFolderDescriptor());
    myPathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        setModified(!myPathField.getText().equals(PlayConfiguration.getConfiguration().myPath));
      }
    });

    myShowOnStartup.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean showOnRun = PlayConfiguration.getConfiguration().myShowOnRun;
        setModified(showOnRun == null || myShowOnStartup.isSelected() != showOnRun.booleanValue());
      }
    });
    myHomePathLabel.setLabelFor(myField.getTextField());

    myDownloadLink.setHyperlinkTarget(PlayBundle.message("play.download.page"));
    myDownloadLink.setHyperlinkText(PlayBundle.message("play.download.title"));

    return myPanel;
  }

  @Override
  public void apply() {
    PlayConfiguration configuration = PlayConfiguration.getConfiguration();

    configuration.myPlayHome = myField.getText();
    configuration.myPath = myPathField.getText();
    configuration.myShowOnRun = myShowOnStartup.isSelected();
  }

  @Override
  public void reset() {
    PlayConfiguration configuration = PlayConfiguration.getConfiguration();
    myField.setText(configuration.myPlayHome);
    myPathField.setText(configuration.myPath);

    Boolean show = configuration.myShowOnRun;
    myShowOnStartup.setSelected(show == null || show.booleanValue());
  }

  @NotNull
  @Override
  public String getId() {
    return getHelpTopic();
  }

  @Nls
  private static String getTitle() {
    return PlayBundle.message("play.home.configuration");
  }
}
