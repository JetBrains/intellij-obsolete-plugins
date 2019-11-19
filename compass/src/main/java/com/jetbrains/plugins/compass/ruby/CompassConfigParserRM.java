package com.jetbrains.plugins.compass.ruby;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.interpret.RubyPsiInterpreter;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;
import org.jetbrains.plugins.ruby.ruby.lang.psi.basicTypes.stringLiterals.RStringLiteral;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RArray;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RAssignmentExpression;
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.expressions.RAssignmentExpressionNavigator;
import com.jetbrains.plugins.compass.CompassConfig;
import com.jetbrains.plugins.compass.CompassConfigParser;

import java.util.ArrayList;
import java.util.List;

public class CompassConfigParserRM extends CompassConfigParser {
  @NotNull
  @Override
  public CompassConfig parse(@NotNull final VirtualFile file,
                             @NotNull final String importPathsRoot,
                             @Nullable final PsiManager psiManager) {
    return ReadAction.compute(() -> {
      PsiFile psiFile = psiManager != null ? psiManager.findFile(file) : null;
      if (psiFile != null) {
        final List<String> importPaths = new ArrayList<>();
        final List<String> additionalImportPaths = new ArrayList<>();
        final RubyPsiInterpreter interpreter = new RubyPsiInterpreter(true);
        interpreter.interpret(psiFile, arguments -> {

          final String cmdName = arguments.getCommand();

          if (ADD_IMPORT_PATH_CALL.equals(cmdName)) {
            final RPsiElement firstArgument = ContainerUtil.getFirstItem(arguments.getArguments());
            if (firstArgument instanceof RStringLiteral) {
              importPaths.add(normalizePath(((RStringLiteral)firstArgument).getContent(), importPathsRoot));
            }
          }
          else if (ADDITIONAL_IMPORT_PATHS_ASSIGNMENT.equalsIgnoreCase(cmdName)) {
            final RAssignmentExpression assignment = RAssignmentExpressionNavigator.getAssignmentByLeftPart(arguments.getCallElement());
            if (assignment != null) {
              additionalImportPaths.clear();
              final RPsiElement value = assignment.getValue();
              if (value instanceof RArray) {
                for (RPsiElement element : ((RArray)value).getElements()) {
                  if (element instanceof RStringLiteral) {
                    additionalImportPaths.add(normalizePath(((RStringLiteral)element).getContent(), importPathsRoot));
                  }
                }
              }
            }
          }
        });
        importPaths.addAll(additionalImportPaths);
        return new CompassConfig(importPaths);
      }
      return CompassConfig.EMPTY_COMPASS_CONFIG;
    });
  }
}
