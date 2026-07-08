package com.intellij.lang.puppet.ide.navigation.plugins.facts;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.lang.puppet.psi.resolve.PuppetNamedPsiElementProcessor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PuppetFactsIndex extends FileBasedIndexExtension<String, Integer> {
  private static final ExtensionPointName<PuppetFactDataIndexer> EP_NAME = new ExtensionPointName<>("com.intellij.puppet.factIndexer");

  public static final ID<String, Integer> KEY = ID.create("PuppetFactsIndex");

  private static final int VERSION = 3;

  @Override
  public @NotNull ID<String, Integer> getName() {
    return KEY;
  }

  @Override
  public @NotNull DataIndexer<String, Integer, FileContent> getIndexer() {
    return new DataIndexer<>() {
      @Override
      public @NotNull Map<String, Integer> map(@NotNull FileContent inputData) {
        for (PuppetFactDataIndexer indexer : EP_NAME.getExtensionList()) {
          if (indexer.acceptsFile(inputData.getFileName(), inputData.getFile().getParent().getName())) {
            return indexer.map(inputData);
          }
        }
        return Collections.emptyMap();
      }
    };
  }

  @Override
  public @NotNull KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public @NotNull DataExternalizer<Integer> getValueExternalizer() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    for (PuppetFactDataIndexer indexer : EP_NAME.getExtensionList()) {
      FileType ft = indexer.getSuitableFileType();
      if (ft == null) return PuppetFactsIndex::_acceptFile;
    }
    return new DefaultFileTypeSpecificInputFilter(
      EP_NAME.getExtensionList().stream().map(indexer -> indexer.getSuitableFileType()).toArray(FileType[]::new)) {
      @Override
      public boolean acceptInput(@NotNull VirtualFile file) {
        return _acceptFile(file);
      }
    };
  }

  private static boolean _acceptFile(VirtualFile file) {
    final VirtualFile parentDir = file.getParent();

    if (!parentDir.isDirectory()) {
      return false;
    }

    String fileName = file.getName();
    String parentDirName = parentDir.getName();

    for (PuppetFactDataIndexer indexer : EP_NAME.getExtensionList()) {
      if (indexer.acceptsFile(fileName, parentDirName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  public static List<PsiElement> findElementsByKey(@NotNull String key,
                                                   final @NotNull Project project,
                                                   @NotNull GlobalSearchScope searchScope,
                                                   final int maxReturnSize) {
    final List<PsiElement> result = new ArrayList<>();
    FileBasedIndex.getInstance().processValues(KEY, key, null, (virtualFile, puppetExtSymbolInfo) -> {
      result.add(new PuppetLazyProxyLightElement(
        project,
        key,
        virtualFile,
        puppetExtSymbolInfo,
        PuppetBundle.message("puppet.fact"))
      );
      return result.size() < maxReturnSize;
    }, searchScope);

    return result;
  }

  public static Collection<String> getAllKeys(@NotNull Project project) {
    return FileBasedIndex.getInstance().getAllKeys(KEY, project);
  }

  public static void processAllElements(@NotNull Project project,
                                        @NotNull GlobalSearchScope searchScope,
                                        @NotNull PuppetNamedPsiElementProcessor processor) {
    for (String key : getAllKeys(project)) {
      for (PsiElement variable : findElementsByKey(key, project, searchScope, Integer.MAX_VALUE)) {
        processor.executeWithName(key, variable);
      }
    }
  }
}
