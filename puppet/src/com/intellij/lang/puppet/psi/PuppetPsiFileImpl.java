package com.intellij.lang.puppet.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.Language;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.lang.puppet.psi.resolve.PuppetVariableScopeProcessor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil.PUPPET_STUBS_ROOT_PATH;

/**
 * @author Anna Bulenkova
 */
public class PuppetPsiFileImpl extends PsiFileBase implements PuppetNamedScopeHolder, PupppetTopScopeHolder {

  private static final @NonNls String PUPPET_BUILTIN_STUBS_FILE_NAME = "builtin.pp";

  private static final String PUPPET_BUILTIN_STUBS_FILE_PATH = PUPPET_STUBS_ROOT_PATH + PUPPET_BUILTIN_STUBS_FILE_NAME;

  private static final Key<Boolean> PUPPET_BUILTIN_STUBS_FILE_KEY = Key.create("it's a puppet builtins stubs file");

  private static final @NonNls String PUPPET_BUILTIN_VARIABLES_STUBS_FILE_NAME = "builtin_variables.pp";

  private static final String PUPPET_BUILTIN_VARIABLES_STUBS_FILE_PATH = PUPPET_STUBS_ROOT_PATH + PUPPET_BUILTIN_VARIABLES_STUBS_FILE_NAME;

  private static final Key<Boolean> PUPPET_BUILTIN_VARIABLES_STUBS_FILE_KEY = Key.create("it's a puppet builtin variables stubs file");

  public PuppetPsiFileImpl(FileViewProvider provider) {
    this(provider, PuppetLanguage.INSTANCE);
  }

  protected PuppetPsiFileImpl(@NotNull FileViewProvider viewProvider, @NotNull Language language) {
    super(viewProvider, language);
    VirtualFile virtualFile = getVirtualFile();

    if (virtualFile != null) {
      String fileName = virtualFile.getName();
      if (FileUtil.namesEqual(fileName, PUPPET_BUILTIN_STUBS_FILE_NAME) &&
          FileUtil.pathsEqual(virtualFile.getPath(), PUPPET_BUILTIN_STUBS_FILE_PATH)) {
        putUserData(PUPPET_BUILTIN_STUBS_FILE_KEY, true);
      }
      if (FileUtil.namesEqual(fileName, PUPPET_BUILTIN_VARIABLES_STUBS_FILE_NAME) &&
          FileUtil.pathsEqual(virtualFile.getPath(), PUPPET_BUILTIN_VARIABLES_STUBS_FILE_PATH)) {
        putUserData(PUPPET_BUILTIN_VARIABLES_STUBS_FILE_KEY, true);
      }
    }
  }

  @Override
  public @NotNull FileType getFileType() {
    return PuppetFileType.INSTANCE;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState resolveState,
                                     @Nullable PsiElement lastChildElement,
                                     @NotNull PsiElement originElement) {
    if (lastChildElement instanceof PuppetNodeDefinition) {
      lastChildElement = null;
    }

    boolean childrenProcessingResult = PuppetResolveUtil.processChildren(this, processor, resolveState, lastChildElement, originElement);

    if (childrenProcessingResult && processor instanceof PuppetVariableScopeProcessor) {
      ((PuppetVariableScopeProcessor)processor).processFile(this);
    }

    return childrenProcessingResult;
  }

  public static boolean isInBuiltInStubsFile(@NotNull PsiElement element) {
    return PUPPET_BUILTIN_STUBS_FILE_KEY.get(element.getContainingFile()) != null;
  }

  public static boolean isInBuiltinVariablesStubsFile(@NotNull PsiElement element) {
    return PUPPET_BUILTIN_VARIABLES_STUBS_FILE_KEY.get(element.getContainingFile()) != null;
  }

}
