package com.intellij.play.console;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayConsoleCompletionContributor extends CompletionContributor {
  private static final Map<String, String> myCommands = new HashMap<>();
  private static final Map<String, CompleteParametersFunction> myParameters = new HashMap<>();

  static {
    myCommands.put("antify", "Create a build.xml file for this project");
    myCommands.put("auto-test", "Automatically run all application tests");
    myCommands.put("build-module", "Build and package a module");
    myCommands.put("check", "Check for a release newer than the current one");
    myCommands.put("classpath", "Display the computed classpath");
    myCommands.put("clean", "Delete temporary files (including the bytecode cache)");
    myCommands.put("dependencies", "Resolve and retrieve project dependencies");
    myCommands.put("eclipsify", "Create all Eclipse configuration files");
    myCommands.put("evolutions", "Run the evolution check");
    myCommands.put("evolutions:apply", "Automatically apply pending evolutions");
    myCommands.put("evolutions:markA", "ppliedMark pending evolutions as manually applied");
    myCommands.put("evolutions:resolve", " Resolve partially applied evolution");
    myCommands.put("help", "Display help on a specific command");
    myCommands.put("id", "Define the framework ID");
    myCommands.put("idealize", "Create all IntelliJ Idea configuration files");
    myCommands.put("install", "Install a module");
    myCommands.put("javadoc", "Generate your application Javadoc");
    myCommands.put("list-modules", "List modules available from the central modules repository");
    myCommands.put("modules", "Display the computed modules list");
    myCommands.put("netbeansify", "Create all NetBeans configuration files");
    myCommands.put("new", "Create a new application");
    myCommands.put("new-module", "Create a module");
    myCommands.put("out", "Follow logs/system.out file");
    myCommands.put("pid", "Show the PID of the running application");
    myCommands.put("precompile", "Precompile all Java sources and templates to speed up application start-up");
    myCommands.put("restart", "Restart the running application");
    myCommands.put("run", "Run the application in the current shell");
    myCommands.put("secret", "   Generate a new secret key");
    myCommands.put("start", "Start the application in the background");
    myCommands.put("status", "Display the running application's status");
    myCommands.put("stop", "Stop the running application");
    myCommands.put("test", "Run the application in test mode in the current shell");
    myCommands.put("version", "Print the framework version");
    myCommands.put("war", "Export the application as a standalone WAR archive");

    myParameters.put("run", new ModulesAsParametersFunction());
    myParameters.put("start", new ModulesAsParametersFunction());
    myParameters.put("stop", new ModulesAsParametersFunction());
  }


  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    if (parameters.getInvocationCount() != 0) {
      final PsiFile originalFile = parameters.getOriginalFile();
      final Document document = PsiDocumentManager.getInstance(originalFile.getProject()).getDocument(originalFile);
      if (document != null) {
        final Object data = document.getUserData(PlayConsoleRunner.PLAY_CONSOLE_KEY);
        if (data != null) {
          final String text = parameters.getOriginalFile().getText();
          final List<String> strings = StringUtil.split(text, " ");

          if (strings.size() > 0) {
            final String commandName = strings.get(0);
            final CompleteParametersFunction function = myParameters.get(commandName.trim());

            if (function != null) {
              function.complete(result, parameters, text.substring(commandName.length()));
            }
          }
          else {
            completeCommands(result);
          }
        }
      }
    }
  }

  public static boolean completeModuleNames(CompletionResultSet result, CompletionParameters parameters, String parametersString) {
    final List<String> params = StringUtil.split(parametersString, " ");
    final String prefix = result.getPrefixMatcher().getPrefix();
    if (params.size() == 0 || prefix.trim().length() > 0) {
      final Project project = parameters.getPosition().getProject();
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        result.addElement(
          LookupElementBuilder.create(StringUtil.toLowerCase(module.getName())).withCaseSensitivity(false)
            .withIcon(ModuleType.get(module).getIcon()).withTailText(getWorkingDir(module), true));
      }
    }
    return true;
  }

  private static void completeCommands(CompletionResultSet result) {
    for (Map.Entry<String, String> entry : myCommands.entrySet()) {
      result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(entry.getKey()).
        withCaseSensitivity(false).
        withTailText(" (" + entry.getValue() + ")", true).

        bold(), TailType.SPACE));
    }

    result.stopHere();
  }

  @Nullable
  private static String getWorkingDir(Module module) {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    if (contentRoots.length == 0) {
      return "";
    }
    return contentRoots[0].getPath();
  }

  private static class ModulesAsParametersFunction extends CompleteParametersFunction {
    @Override
    boolean complete(CompletionResultSet result, CompletionParameters parameters, String parametersString) {
      return completeModuleNames(result, parameters, parametersString);
    }
  }

  public abstract static class CompleteParametersFunction {
    abstract boolean complete(CompletionResultSet result, CompletionParameters parameters, String parametersString);
  }
}
