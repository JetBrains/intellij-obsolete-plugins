package com.intellij.gwt.run.remoteUi;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.gwt.run.GwtClasspathUtil;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.psi.search.GlobalSearchScope.moduleWithDependenciesScope;

public class GwtErrorFilter implements Filter {

  private static final Pattern FILE_NAME_EXTRACTOR = Pattern.compile("^\\[ERROR] Errors in '(.*)'$");
  private static final Pattern LINE_NUMBER_EXTRACTOR = Pattern.compile("^\\[ERROR] (Line (\\d+):.+)$");

  private static final Pattern BINDER_NAME_EXTRACTOR = Pattern.compile("^Computing all possible rebind results for '(.*)'$");
  private static final Pattern BINDER_LINE_EXTRACTOR = Pattern.compile("^\\[ERROR] (.*[(]:(\\d+)[)])$");

  private final Module myModule;
  private final Project myProject;
  private int myContextLeadingSpacesCount;
  private VirtualFile myCurrentVirtualFile;

  public GwtErrorFilter(Module module) {
    myModule = module;
    myProject = module.getProject();
    myContextLeadingSpacesCount = -1;
  }

  @Override
  public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
    String trimmedLine = line.trim();
    int leadingSpacesCount = spacesCount(line);
    if (leadingSpacesCount <= myContextLeadingSpacesCount || myContextLeadingSpacesCount == -1) {
      String currentFilePath = null;
      if (myContextLeadingSpacesCount != -1) {
        myContextLeadingSpacesCount = -1;
        myCurrentVirtualFile = null;
      }
      Matcher matcher = FILE_NAME_EXTRACTOR.matcher(trimmedLine);
      if (matcher.matches()) {
        currentFilePath = matcher.group(1);
      }
      else {
        matcher = BINDER_NAME_EXTRACTOR.matcher(trimmedLine);
        if (matcher.matches()) {
          JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myProject);
          PsiClass binderClass = psiFacade.findClass(matcher.group(1), moduleWithDependenciesScope(myModule));
          currentFilePath = binderClass == null ? null : UiBinderMappingService.deduceTemplateFileUrl(binderClass);
        }
      }

      if (currentFilePath != null) {
        myCurrentVirtualFile = findVirtualFile(currentFilePath);
        if (myCurrentVirtualFile != null) {
          myContextLeadingSpacesCount = leadingSpacesCount;
        }
      }

      return null;
    }
    else {
      if (myCurrentVirtualFile == null || !myCurrentVirtualFile.isValid()) {
        return null;
      }

      Matcher matcher = LINE_NUMBER_EXTRACTOR.matcher(trimmedLine);
      if (!matcher.find()) {
        matcher = BINDER_LINE_EXTRACTOR.matcher(trimmedLine);
        if (!matcher.find()) {
          return null;
        }
      }

      int startIndex = entireLength + leadingSpacesCount - line.length() + matcher.start(1);
      int endIndex   = entireLength + leadingSpacesCount - line.length() + matcher.end(1);
      int lineNumber = Integer.parseInt(matcher.group(2)) - 1;

      HyperlinkInfo hyperlinkInfo = new OpenFileHyperlinkInfo(myProject, myCurrentVirtualFile, lineNumber, 0);
      return new Result(startIndex, endIndex, hyperlinkInfo);
    }
  }

  private VirtualFile findVirtualFile(@NonNls String currentFilePath) {
    if (currentFilePath.startsWith("file:")) {
      return VirtualFileManager.getInstance().findFileByUrl(currentFilePath);
    }
    else {
      for (VirtualFile root : GwtClasspathUtil.getSourceRootsOfGwtModules(myModule, false).getRootDirs()) {
        VirtualFile file = root.findFileByRelativePath(currentFilePath);
        if (file != null) {
          return file;
        }
      }
    }
    return null;
  }

  private static int spacesCount(String line) {
    int length = line.length();
    int count = 0;
    while (count < length && line.charAt(count) <= ' ') {
      count++;
    }
    return count;
  }
}
