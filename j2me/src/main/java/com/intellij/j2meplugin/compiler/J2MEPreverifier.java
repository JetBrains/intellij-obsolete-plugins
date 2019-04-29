/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.compiler;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.j2meplugin.emulator.MobileSdk;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class J2MEPreverifier implements ClassPostProcessingCompiler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");
  private final HashMap<Module, File> myModulePreverifiedClasses = new HashMap<>();

  @Override
  @NotNull
  public ProcessingItem[] getProcessingItems(final CompileContext context) {
    myModulePreverifiedClasses.clear();
    return ReadAction.compute(() -> {
      final Module[] affectedModules = context.getCompileScope().getAffectedModules();
      if (affectedModules.length == 0) {
        return ProcessingItem.EMPTY_ARRAY;
      }
      ArrayList<ProcessingItem> result = new ArrayList<>();
      for (final Module module : affectedModules) {
        if (module.isDisposed()) continue;
        if (!ModuleType.get(module).equals(J2MEModuleType.getInstance())) continue;
        final Sdk jdk = ModuleRootManager.getInstance(module).getSdk();
        //check mobile jdk
        if (!MobileSdk.checkCorrectness(jdk, module)) {
          context.addMessage(CompilerMessageCategory.ERROR,
                             J2MEBundle.message("compiler.jdk.is.invalid.common", jdk != null ? jdk.getName() : " "), null, -1, -1);
          continue;
        }
        if (((Emulator)jdk.getSdkAdditionalData()).getEmulatorType().getPreverifyPath(jdk.getHomePath()) == null) {
          //not necessary to preverify
          continue;
        }
        try {
          final File temp = FileUtil.createTempDirectory("temp", "temp");
          temp.deleteOnExit();
          myModulePreverifiedClasses.put(module, temp);
          HashSet<Module> modulesToPreverify = new HashSet<>();
          MobileMakeUtil.getDependencies(module, modulesToPreverify);
          ArrayList<VirtualFile> dependantClasspath = new ArrayList<>();
          for (Module toPreverify : modulesToPreverify) {
            if (module.equals(toPreverify)) continue;
            final VirtualFile moduleOutputDirectory = context.getModuleOutputDirectory(toPreverify);
            dependantClasspath.add(moduleOutputDirectory);
            if (moduleOutputDirectory != null) {
              result.add(new MyProcessingItem(moduleOutputDirectory, toPreverify, temp, jdk, null));
            }
          }
          final VirtualFile moduleOutputDirectory = context.getModuleOutputDirectory(module);
          if (moduleOutputDirectory != null) {
            result.add(new MyProcessingItem(moduleOutputDirectory, module, temp, jdk, dependantClasspath));
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
      return result.toArray(ProcessingItem.EMPTY_ARRAY);
    });
  }

  @Override
  public ProcessingItem[] process(final CompileContext context, final ProcessingItem[] items) {
    ProgressIndicator progressIndicator = context.getProgressIndicator();
    progressIndicator.pushState();
    try {
      progressIndicator.setText(J2MEBundle.message("compiler.preverifying.progress.title"));
      LocalFileSystem.getInstance().refresh(false);
      return ReadAction.compute(() -> {
        List<ProcessingItem> processed = new ArrayList<>();
        for (int i = 0; items != null && i < items.length; i++) {
          final MyProcessingItem item = (MyProcessingItem)items[i];
          final ArrayList<VirtualFile> dependantClasspath = item.getDependantClasspath();
          try {
            FileUtil.copyDir(new File(item.myFromClasses.getPath().replace('/', File.separatorChar)), item.getToClasses());
          }
          catch (IOException e) {
            LOG.error(e);
          }
          final Sdk jdk = item.getJdkToPreverify();
          try {
            GeneralCommandLine generalCommandLine = new GeneralCommandLine();
            final Emulator emulator = (Emulator)jdk.getSdkAdditionalData();
            final EmulatorType emulatorType = emulator.getEmulatorType();
            generalCommandLine.setExePath(emulatorType.getPreverifyPath(jdk.getHomePath()));
            final String[] preverifyOptions = emulator.getPreverifyOptions();
            if (preverifyOptions != null) {
              generalCommandLine.addParameters(preverifyOptions);
            }
            generalCommandLine.addParameter("-d");
            generalCommandLine.addParameter(item.getToClasses().getPath().replace(File.separatorChar, '/'));
            generalCommandLine.addParameter("-classpath");
            PathsList classpath = new PathsList();
            classpath.addVirtualFiles(jdk.getRootProvider().getFiles(OrderRootType.CLASSES));
            OrderEnumerator.orderEntries(item.getModule()).librariesOnly().classes().collectPaths(classpath);
            for (int k = 0; dependantClasspath != null && k < dependantClasspath.size(); k++) {
              classpath.add(PathUtil.getLocalPath(dependantClasspath.get(k)));
            }

            generalCommandLine.addParameter(classpath.getPathsString());
            generalCommandLine.addParameter(item.getFile().getPath());

            generalCommandLine.setWorkDirectory(jdk.getHomePath());

            OSProcessHandler osProcessHandler = new OSProcessHandler(generalCommandLine);
            final ArrayList<String> errors = new ArrayList<>();
            osProcessHandler.addProcessListener(new ProcessAdapter() {
              @Override
              public void onTextAvailable(@NotNull final ProcessEvent event, @NotNull final Key outputType) {
                errors.add(event.getText());
              }
            });
            osProcessHandler.startNotify();
            osProcessHandler.waitFor();
            printPreverifyErrors(errors, context);
          }
          catch (ExecutionException e) {
            context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
          }
          processed.add(items[i]);
        }
        return processed.toArray(ProcessingItem.EMPTY_ARRAY);
      });
    }
    finally {
      progressIndicator.popState();
    }
  }

  private void printPreverifyErrors(final ArrayList<String> errors, final CompileContext context) {
    int textIndex = 0;
    int noErrors = -1;
    while (textIndex < errors.size() && noErrors == -1) {
      String text = errors.get(textIndex);
      if (StringUtil.containsIgnoreCase(text, J2MEBundle.message("compiler.preverify.error"))) {
        noErrors = textIndex;
      }
      textIndex++;
    }
    if (noErrors > -1) {
      for (int errorIdx = noErrors; errorIdx < errors.size(); errorIdx++) {
        context.addMessage(CompilerMessageCategory.WARNING, getDescription() + errors.get(errorIdx), null, -1, -1);
      }
    }
  }

  public File getModulePreverifiedClasses(Module module) {
    return myModulePreverifiedClasses.get(module);
  }

  private static class MyProcessingItem implements ProcessingItem {
    private EmptyValidityState myEmptyValidityState;
    private final VirtualFile myFromClasses;
    private final File myToClasses;
    private final Sdk myProjectJdk;
    private final Module myModule;
    private final ArrayList<VirtualFile> myDependantClasspath;

    MyProcessingItem(VirtualFile fromClasses,
                            Module module,
                            File toClasses,
                            Sdk projectJdk,
                            ArrayList<VirtualFile> dependantClasspath) {
      myFromClasses = fromClasses;
      myToClasses = toClasses;
      myProjectJdk = projectJdk;
      myModule = module;
      myDependantClasspath = dependantClasspath;
      setValidityState();
    }

    public ArrayList<VirtualFile> getDependantClasspath() {
      return myDependantClasspath;
    }

    public File getToClasses() {
      return myToClasses;
    }

    public Sdk getJdkToPreverify() {
      return myProjectJdk;
    }

    @Override
    @NotNull
    public VirtualFile getFile() {
      return myFromClasses;
    }

    public Module getModule() {
      return myModule;
    }


    @Override
    public EmptyValidityState getValidityState() {
      return myEmptyValidityState;
    }

    public void setValidityState() {
      myEmptyValidityState = new EmptyValidityState();
    }


  }


  @Override
  @NotNull
  public String getDescription() {
    return J2MEBundle.message("compiler.preverifier");
  }

  @Override
  public boolean validateConfiguration(final CompileScope scope) {
    return ReadAction.compute(() -> {
      final Module[] affectedModules = scope.getAffectedModules();
      if (affectedModules.length == 0) {
        return Boolean.TRUE;
      }
      for (final Module module : affectedModules) {
        if (!ModuleType.get(module).equals(J2MEModuleType.getInstance())) continue;
        Sdk projectJdk = ModuleRootManager.getInstance(module).getSdk();
        if (!MobileSdk.checkCorrectness(projectJdk, module)) {
          Messages.showErrorDialog(
            J2MEBundle.message("compiler.jdk.is.invalid", projectJdk != null ? projectJdk.getName() : "", module.getName()),
            J2MEBundle.message("compiler.unable.to.compile", module.getName()));
          return Boolean.FALSE;
        }
        final MobileApplicationType mobileApplicationType = J2MEModuleProperties.getInstance(module).getMobileApplicationType();
        final MobileModuleSettings settings = MobileModuleSettings.getInstance(module);
        LOG.assertTrue(settings != null);
        if (settings.getSettings().get(mobileApplicationType.getJarUrlSettingName()) == null) {
          Messages.showErrorDialog(J2MEBundle.message("compiler.jar.file.not.specified"),
                                   J2MEBundle.message("compiler.unable.to.compile", module.getName()));
          return Boolean.FALSE;
        }
        else if (settings.getMobileDescriptionPath() == null) {
          Messages.showErrorDialog(
            J2MEBundle.message("compiler.descriptor.file.not.specified", StringUtil.capitalize(mobileApplicationType.getExtension())),
            J2MEBundle.message("compiler.unable.to.compile", module.getName()));
          return Boolean.FALSE;
        }
      }
      return Boolean.TRUE;
    }).booleanValue();
  }

  @Override
  public ValidityState createValidityState(DataInput in) throws IOException {
    return new EmptyValidityState();
  }

}
