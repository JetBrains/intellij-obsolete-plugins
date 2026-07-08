package com.intellij.lang.puppet.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */
public final class PuppetRunConfiguration extends LocatableConfigurationBase {
  private final PuppetRunnerParameters myRunnerParameters = new PuppetRunnerParameters();

  PuppetRunConfiguration(final Project project, final ConfigurationFactory factory, final String name) {
    super(project, factory, name);
  }

  public PuppetRunnerParameters getRunnerParameters() {
    return myRunnerParameters;
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return null;
    //return new PuppetConfigurationEditorForm(getProject());
  }

  @Override
  public RunProfileState getState(final @NotNull Executor executor, final @NotNull ExecutionEnvironment env) throws ExecutionException {
    final Project project = env.getProject();
    final @NlsSafe String path = myRunnerParameters.getFilePath();
    if (path == null || VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(path)) == null) {
      throw new ExecutionException(PuppetBundle.message("dialog.message.can.t.find.file", path));
    }
    final VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(path));
    assert virtualFile != null;
    final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
    if (module == null) {
      throw new ExecutionException(PuppetBundle.message("dialog.message.can.t.find.module.for.file"));
    }
    return new PuppetRunningState(env, myRunnerParameters);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    final String path = myRunnerParameters.getFilePath();
    if (path == null || VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(path)) == null) {
      throw new RuntimeConfigurationException(PuppetBundle.message("run.configuration.exception.no.file", path));
    }
    if (!FileUtilRt.extensionEquals(path, PuppetFileType.INSTANCE.getDefaultExtension())) {
      throw new RuntimeConfigurationException(PuppetBundle.message("run.configuration.exception.non.puppet.file"));
    }
  }

  @Override
  public void writeExternal(final @NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    XmlSerializer.serializeInto(myRunnerParameters, element);
  }

  @Override
  public void readExternal(final @NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    XmlSerializer.deserializeInto(myRunnerParameters, element);
  }
}
