// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsConfigUtils;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.grails.util.version.Range;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.grails.util.version.VersionImpl;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import java.util.Objects;

public class GrailsSDKHomeForm {

  private static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false) {
    @Override
    public void validateSelectedFiles(VirtualFile @NotNull [] files) throws Exception {
      assert files.length == 1;
      final VirtualFile file = files[0];
      final String version = GrailsConfigUtils.getInstance().getSDKVersionOrNull(file.getPath());
      if (version == null) {
        throw new Exception(ProjectBundle.message(
          file.isDirectory() ? "sdk.configure.home.invalid.error" : "sdk.configure.home.file.invalid.error", "Grails SDK"
        ));
      }
    }
  }.withShowHiddenFiles(true);

  private JPanel myComponent;
  private TextFieldWithBrowseButton myPath;
  private JBLabel myVersionLabel;

  private @Nullable Range<Version> myVersionRange;
  private @Nullable Runnable myChangedCallback;

  private GrailsSDK mySelectedSdk;

  public GrailsSDKHomeForm() {
    myPath.addBrowseFolderListener(new TextBrowseFolderListener(FILE_CHOOSER_DESCRIPTOR) {
      @Override
      protected void onFileChosen(@NotNull VirtualFile chosenFile) {
        super.onFileChosen(chosenFile);
        validate();
        if (myChangedCallback != null) myChangedCallback.run();
      }

      @Override
      protected @Nullable VirtualFile getInitialFile() {
        final VirtualFile file = super.getInitialFile();
        if (file != null) return file;
        final VirtualFile gvmCurrent = VfsUtil.findRelativeFile(VfsUtil.getUserHomeDir(), ".gvm", "grails", "current");
        if (gvmCurrent != null) return gvmCurrent;
        return VfsUtil.findRelativeFile(VfsUtil.getUserHomeDir(), ".sdkman", "grails", "current");
      }
    });
    myPath.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        validate();
        if (myChangedCallback != null) myChangedCallback.run();
      }
    });
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public JComponent getPathComponent() {
    return myPath;
  }

  public @NotNull GrailsSDK getSelectedSdk() {
    return Objects.requireNonNull(mySelectedSdk);
  }

  public GrailsSDKHomeForm setPath(String path) {
    myPath.setText(path);
    validate();
    return this;
  }

  public GrailsSDKHomeForm setVersionRange(@Nullable Range<Version> versionRange) {
    myVersionRange = versionRange;
    validate();
    return this;
  }

  public GrailsSDKHomeForm setChangedCallback(@Nullable Runnable changedCallback) {
    myChangedCallback = changedCallback;
    return this;
  }

  public GrailsSDKHomeForm setEditable(boolean editable) {
    myPath.setEditable(editable);
    myPath.setButtonEnabled(editable);
    return this;
  }

  public boolean validate() {
    final String path = myPath.getText();
    if (StringUtil.isEmpty(path)) {
      return error(GrailsBundle.message("sdk.home.form.label.text.sdk.not.selected"));
    }
    else {
      final String version = GrailsConfigUtils.getInstance().getSDKVersionOrNull(path);
      if (version == null) {
        return error(GrailsBundle.message("sdk.home.form.label.text.cannot.determine.grails.sdk.version"));
      }
      else {
        if (myVersionRange == null || myVersionRange.contains(new VersionImpl(version))) {
          mySelectedSdk = new GrailsSDK(path, new VersionImpl(version));
          return message(GrailsBundle.message("sdk.home.form.label.text.version", version), false);
        }
        else {
          return error(GrailsBundle.message("sdk.home.form.label.text.version.range.error", prettyPrint(myVersionRange)));
        }
      }
    }
  }

  private boolean error(@NlsContexts.Label String message) {
    mySelectedSdk = null;
    return message(message, true);
  }

  private boolean message(@NlsContexts.Label String message, boolean isError) {
    HtmlBuilder builder = new HtmlBuilder();
    builder.append(HtmlChunk.html().children(HtmlChunk.raw(message)));
    myVersionLabel.setText(builder.toString());
    myVersionLabel.setForeground(isError ? JBColor.RED : JBColor.foreground());
    return !isError;
  }

  private static @NotNull String prettyPrint(@NotNull Range<Version> range) {
    final StringBuilder sb = new StringBuilder();
    if (range.getStart() != null) {
      sb.append(range.isStartInclusive() ? "at least" : "more than").append(" ").append(range.getStart());
    }
    if (range.getStart() != null && range.getEnd() != null) {
      sb.append(" and ");
    }
    if (range.getEnd() != null) {
      sb.append(range.isEndInclusive() ? "not more than" : "less than").append(" ").append(range.getEnd());
    }
    return sb.toString();
  }
}
