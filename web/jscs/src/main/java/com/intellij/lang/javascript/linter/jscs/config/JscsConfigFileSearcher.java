package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author by Irina.Chernushina on 9/24/2014.
 */
public class JscsConfigFileSearcher {
  private final static String PACKAGE_FILE_OPTION_NAME = "jscsConfig";
  private final static String JSCSRC = "jscsrc";
  private final static String JSCS_JSON = "jscs.json";

  @NotNull
  private final Project myProject;
  @NotNull
  private final VirtualFile myFile;

  private File myConfig;
  private File myPackage;
  private String myError;

  public JscsConfigFileSearcher(@NotNull Project project, @NotNull VirtualFile file) {
    myProject = project;
    myFile = file;
  }

  public void lookup() {
    if (! myFile.isValid() || myFile.getParent() == null) return;
    browseDirectoriesUp(file -> lookForPackage(file) || lookForConfig(file) || myError != null);
  }

  public File getConfig() {
    return myConfig;
  }

  public File getPackage() {
    return myPackage;
  }

  public String getError() {
    return myError;
  }

  private boolean lookForConfig(@NotNull final VirtualFile file) {
    if (myError != null) return false;  // skip check if a problem occurred earlier
    final VirtualFile[] children = file.getChildren();
    for (VirtualFile child : children) {
      if (! child.isDirectory() && (child.getName().endsWith(JSCSRC) || child.getName().endsWith(JSCS_JSON))) {
        myConfig = new File(child.getPath());
        return true;
      }
    }
    return false;
  }

  private boolean lookForPackage(@NotNull final VirtualFile file) {
    final VirtualFile packageFile = PackageJsonUtil.findChildPackageJsonFile(file);
    if (packageFile != null) {
      if (PackageJsonUtil.isPackageJsonWithTopLevelProperty(packageFile, PACKAGE_FILE_OPTION_NAME)) {
        myPackage = new File(packageFile.getPath());
        return true;
      }
    }
    return false;
  }

  private void browseDirectoriesUp(@NotNull final Processor<VirtualFile> processor) {
    ApplicationManager.getApplication().runReadAction(() -> {
      if (myProject.isDisposed()) return;
      VirtualFile dir = myFile.getParent();
      while (dir != null) {
        if (processor.process(dir)) return;
        dir = dir.getParent();
      }
    });
  }
}
