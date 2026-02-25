// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.execution.filters.DefaultConsoleFiltersProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.ide.browsers.OpenUrlHyperlinkInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.debug.GspPositionManager;
import org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GrailsConsoleFilterProvider extends DefaultConsoleFiltersProvider {

  @Override
  public Filter @NotNull [] getDefaultFilters(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    return ReadAction.compute(() -> {
      if (!GrailsApplicationManager.getInstance(project).hasApplications()) {
        return Filter.EMPTY_ARRAY;
      }

      return new Filter[]{new GrailsConsoleFilter(project)};
    });
  }

  private static class GrailsConsoleFilter implements Filter {

    private static final Pattern LINK_PATTERN = Pattern.compile("https?:\\/\\/\\S+");

    // Compiler error line has prefix "  [groovyc] " for Grails older than 2.0.0.M1
    private static final Pattern GROOVYC_PATTERN = Pattern.compile("(?:  \\[groovyc\\] )?(((?:/|[a-zA-Z]:[\\\\/])(?:[^\\\\/:\\*\\?\\|]+[\\\\/])*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.(?:groovy|java)): ?(\\d{1,8}): .*?(?:@ line \\2, column (\\d{1,8})\\.)?)\\s*");

    private static final Pattern GSP_PATTERN = Pattern.compile("\tat (\\w+_gsp)(?:\\$\\w+)?\\.\\w+\\(((?:\\1)|(\\w+\\.gsp))(?::(-?\\d+))?\\)\\s*");

    private static final Pattern GSP_2_0_PATTERN = Pattern.compile("(?:->>|\\|) *(-?\\d+) \\| [\\w_\\$]+[ \\.]*in (([\\w\\.]+)(?:\\$[\\w\\.\\$]+)?)\\s*");

    private static final Pattern GSP_COMPILATION_ERROR = Pattern.compile("(\\w+_gsp): ?(\\d{1,8}): .*?@ line \\2, column (\\d{1,8})\\.\\s*");

    private final Project myProject;

    GrailsConsoleFilter(Project project) {
      myProject = project;
    }

    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
      int start = GrailsExecutionUtils.getGrailsConsolePrefixLength(line);

      if (line.startsWith(GrailsExecutionUtils.SERVER_RUNNING_BROWSE_TO, start)) {
        final String url = line.substring(start + GrailsExecutionUtils.SERVER_RUNNING_BROWSE_TO.length()).trim();
        if (LINK_PATTERN.matcher(url).matches()) {
          int urlStart = entireLength - line.length() + start + GrailsExecutionUtils.SERVER_RUNNING_BROWSE_TO.length();
          return new Result(urlStart, urlStart + url.length(), new OpenUrlHyperlinkInfo(url));
        }
        return null;
      }

      Matcher matcher = GROOVYC_PATTERN.matcher(line);
      if (matcher.matches()) {
        String path = matcher.group(2);
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file != null) {
          int lineNumber = Integer.parseInt(matcher.group(3)) - 1;
          String column = matcher.group(4);
          int columnNumber = column == null ? 0 : Integer.parseInt(column) - 1;

          TextAttributes attr = createCompilationErrorAttr();

          return new Result(entireLength - line.length() + matcher.start(1), entireLength - 1,
                            new OpenFileHyperlinkInfo(myProject, file, lineNumber, columnNumber), attr);
        }

        return null;
      }

      matcher = GSP_PATTERN.matcher(line);
      if (matcher.matches()) {
        String page = matcher.group(1);

        VirtualFile gspFile = getGspFile(page);

        if (gspFile != null) {
          String fileName = matcher.group(3);

          int lineNumber = -1;
          if (fileName != null) {
            lineNumber = parseLineNumber(matcher.group(4));
          }

          TextAttributes attr = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);

          return new Result(entireLength - line.length() + matcher.start(2), entireLength - line.length() + matcher.end(2),
                            new OpenFileHyperlinkInfo(myProject, gspFile, lineNumber), attr);
        }

        return null;
      }

      matcher = GSP_2_0_PATTERN.matcher(line);
      if (matcher.matches()) {
        final String className = matcher.group(3);

        final VirtualFile file = getVirtualFileByClassName(className);

        if (file != null) {
          final int lineNumber = parseLineNumber(matcher.group(1));

          TextAttributes attr = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);

          return new Result(entireLength - line.length() + matcher.start(2), entireLength - line.length() + matcher.end(2), new HyperlinkInfo() {
            @Override
            public void navigate(@NotNull Project project) {
              VirtualFile currentFile = getVirtualFileByClassName(className); // Reobtain virtualFile by class name,
                                                                              // because something might be changed (for example sources downloaded)
                                                                              // don't use variable 'file'!
              if (currentFile != null) {
                new OpenFileHyperlinkInfo(myProject, currentFile, lineNumber).navigate(project);
              }
            }
          }, attr);
        }

        return null;
      }

      matcher = GSP_COMPILATION_ERROR.matcher(line);
      if (matcher.matches()) {
        String gspName = matcher.group(1);
        VirtualFile file = getGspFile(gspName);
        if (file != null) {
          int lineNumber = Integer.parseInt(matcher.group(2));
          int columnNumber = Integer.parseInt(matcher.group(3));

          TextAttributes attr = createCompilationErrorAttr();

          return new Result(entireLength - line.length() + matcher.start(1), entireLength - line.length() + matcher.end(1),
                            new OpenFileHyperlinkInfo(myProject, file, lineNumber, columnNumber), attr);
        }

        return null;
      }
      
      return null;
    }

    private @Nullable VirtualFile getVirtualFileByClassName(String className) {
      VirtualFile file = null;

      if (className.indexOf('.') == -1 && className.indexOf('_') != -1) {
        // It's GSP
        file = getGspFile(className);
      }

      if (file == null) {
        PsiClass aClass = JavaPsiFacade.getInstance(myProject).findClass(className, GlobalSearchScope.allScope(myProject));
        if (aClass != null) {
          file = aClass.getContainingFile().getNavigationElement().getContainingFile().getVirtualFile();
        }
      }

      return file;
    }

    private static TextAttributes createCompilationErrorAttr() {
      TextAttributes attr =
        EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES).clone();
      attr.setForegroundColor(JBColor.RED);
      attr.setEffectColor(JBColor.RED);
      attr.setEffectType(EffectType.LINE_UNDERSCORE);
      attr.setFontType(Font.PLAIN);
      return attr;
    }

    private static int parseLineNumber(@Nullable String sLineNumber) {
      if (sLineNumber != null) {
        try {
          int res = Integer.parseInt(sLineNumber);
          if (res >= 0) return res - 1;
        }
        catch (NumberFormatException ignored) {

        }
      }

      return -1;
    }

    private @Nullable VirtualFile getGspFile(String page) {
      for (VirtualFile root : ProjectRootManager.getInstance(myProject).getContentRootsFromAllModules()) {
        String escapedRootName = GspPositionManager.ESCAPED_CHAR.matcher(root.getName()).replaceAll("_") + '_';
        int index = page.indexOf(escapedRootName);
        if (index != -1) {
          VirtualFile gspFile = findFile(page, index + escapedRootName.length(), root);
          if (gspFile != null) {
            return gspFile;
          }
        }
      }

      return null;
    }

    private static @Nullable VirtualFile findFile(String escapedPath, int startIndex, VirtualFile root) {
      for (VirtualFile child : root.getChildren()) {
        String name = child.getName();
        if (startWithIgnoreEscapedLetters(escapedPath, name, startIndex)) {
          int newStartIndex = startIndex + name.length();
          if (newStartIndex == escapedPath.length()) return child;

          if (escapedPath.charAt(newStartIndex) == '_') {
            newStartIndex++;

            VirtualFile res = findFile(escapedPath, newStartIndex, child);
            if (res != null) return res;
          }
        }
      }

      return null;
    }

    private static boolean startWithIgnoreEscapedLetters(String escapedPath, String s, int startIndex) {
      if (s.length() > escapedPath.length() - startIndex) return false;

      for (int i = 0; i < s.length(); i++) {
        char a = s.charAt(i);

        if (!Character.isLetter(a) && !Character.isDigit(a)) {
          a = '_';
        }

        if (escapedPath.charAt(startIndex + i) != a) return false;
      }

      return true;
    }
  }
}
