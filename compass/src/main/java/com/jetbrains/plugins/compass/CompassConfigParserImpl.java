package com.jetbrains.plugins.compass;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.*;
import org.jrubyparser.lexer.SyntaxException;
import org.jrubyparser.parser.ParserConfiguration;
import org.jrubyparser.rewriter.ReWriteVisitor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.vfs.VfsUtilCore.loadText;

public class CompassConfigParserImpl extends CompassConfigParser {
  private static final String DUMMY_CONFIG_PATH_RB = "dummy_config_path.rb";

  @NotNull
  @Override
  public CompassConfig parse(@NotNull final VirtualFile file, @NotNull final String importPathsRoot, @Nullable final PsiManager psiManager) {
    Parser parser = new Parser();
    try {
      final List<String> importPaths = new ArrayList<>();
      final List<String> additionalImportPaths = new ArrayList<>();
      String fileContent = ReadAction.compute(() -> {
        if (psiManager != null) {
          final PsiFile psiFile = psiManager.findFile(file);
          if (psiFile != null) {
            return psiFile.getText();
          }
        }
        return null;
      });
      if (fileContent == null) {
        fileContent = loadText(file);
      }
      Node node = parser.parse(DUMMY_CONFIG_PATH_RB, new StringReader(fileContent), new ParserConfiguration(0, CompatVersion.RUBY2_0));
      if (node == null) {
        return CompassConfig.EMPTY_COMPASS_CONFIG;
      }
      node.accept(new ReWriteVisitor(new StringWriter(), fileContent) {
        @Override
        public Object visitModuleNode(ModuleNode iVisited) {
          return null; //skip modules
        }

        @Override
        public Object visitClassNode(ClassNode iVisited) {
          return null; //skip classes
        }

        @Override
        public Object visitLocalAsgnNode(LocalAsgnNode iVisited) {
          if (ADDITIONAL_IMPORT_PATHS_ASSIGNMENT.equals(iVisited.getName())) {
            additionalImportPaths.clear();
            final Node value = iVisited.getValue();
            if (value instanceof ArrayNode) {
              for (Node node : value.childNodes()) {
                if (node instanceof StrNode) {
                  additionalImportPaths.add(normalizePath(((StrNode)node).getValue(), importPathsRoot));
                }
              }
            }
          }
          return super.visitLocalAsgnNode(iVisited);
        }

        @Override
        public Object visitFCallNode(FCallNode iVisited) {
          if (ADD_IMPORT_PATH_CALL.equals(iVisited.getName())) {
            if (iVisited.getArgs() instanceof ArrayNode) {
              List<Node> nodes = iVisited.getArgs().childNodes();
              if (nodes.size() > 0) {
                for (Node arg : nodes) {
                  if (arg instanceof StrNode) {
                    importPaths.add(normalizePath(((StrNode)arg).getValue(), importPathsRoot));
                  }
                }
              }
            }
          }
          else {
            super.visitFCallNode(iVisited);
          }
          return null;
        }

        @Override
        public void visitNode(Node iVisited) {
          try {
            super.visitNode(iVisited);
          }
          catch (Exception ignore) {
            //ignore parsing errors
          }
        }
      });

      importPaths.addAll(additionalImportPaths);
      return new CompassConfig(importPaths);
    }
    catch (SyntaxException ignore) {
      return CompassConfig.EMPTY_COMPASS_CONFIG;
    }
    catch (IOException e) {
      return CompassConfig.EMPTY_COMPASS_CONFIG;
    }
  }
}
