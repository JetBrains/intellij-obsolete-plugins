package com.intellij.lang.puppet.ide.navigation.plugins.ruby;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.ide.navigation.plugins.PuppetExtFunctionInfo;
import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.lang.puppet.psi.resolve.PuppetNamedPsiElementProcessor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PuppetRubyPluginsIndex extends FileBasedIndexExtension<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo> {

  public static final ID<EntryKey, PuppetExtFunctionInfo> KEY = ID.create("PuppetRubyPluginsIndex");

  private static final MyKeyDescriptor KEY_DESCRIPTOR = new MyKeyDescriptor();

  private static final int VERSION = 3;

  private static final String EXT_RUBY = "rb";

  @Override
  public @NotNull ID<EntryKey, PuppetExtFunctionInfo> getName() {
    return KEY;
  }

  @Override
  public @NotNull DataIndexer<EntryKey, PuppetExtFunctionInfo, FileContent> getIndexer() {

    return new DataIndexer<>() {
      @Override
      public @NotNull Map<EntryKey, PuppetExtFunctionInfo> map(@NotNull FileContent inputData) {
        final VirtualFile file = inputData.getFile();
        final SymbolType symbolType = getSymbolTypeByFileLocation(file);

        assert symbolType != null : "Input filter should have filtered this!";

        return symbolType.myIndexer.map(inputData);
      }
    };
  }

  @Override
  public @NotNull KeyDescriptor<EntryKey> getKeyDescriptor() {
    return KEY_DESCRIPTOR;
  }

  @Override
  public @NotNull DataExternalizer<PuppetExtFunctionInfo> getValueExternalizer() {
    return PuppetExtFunctionInfo.EXTERNALIZER;
  }

  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    FileBasedIndex.InputFilter filter = file -> {
      if (!EXT_RUBY.equals(file.getExtension())) {
        return false;
      }

      return getSymbolTypeByFileLocation(file) != null;
    };

    FileType rubyFileType = FileTypeManager.getInstance().getFileTypeByExtension("rb");
    if (rubyFileType == UnknownFileType.INSTANCE) {
      return filter;
    }
    return new DefaultFileTypeSpecificInputFilter(rubyFileType) {
      @Override
      public boolean acceptInput(@NotNull VirtualFile file) {
        return filter.acceptInput(file);
      }
    };
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  public static List<PsiElement> findElementsByKey(@NotNull SymbolType type,
                                                   @NotNull String key,
                                                   final @NotNull Project project,
                                                   @NotNull GlobalSearchScope searchScope) {
    final @NotNull List<Pair<PuppetExtFunctionInfo, VirtualFile>> values = getSymbolsByKey(type, key, searchScope);
    return ContainerUtil
      .mapNotNull(values, info -> getLightProxyElement(project, key, info.getSecond(), info.getFirst().getOffsetInFile(), type));
  }

  public static @NotNull List<Pair<PuppetExtFunctionInfo, VirtualFile>> getSymbolsByKey(@NotNull SymbolType type,
                                                                                        @NotNull String key,
                                                                                        @NotNull GlobalSearchScope searchScope) {
    List<Pair<PuppetExtFunctionInfo, VirtualFile>> result = new SmartList<>();
    FileBasedIndex.getInstance().processValues(KEY, new EntryKey(type, key), null,
                                               (file, value) -> {
                                                 result.add(Pair.create(value, file));
                                                 return true;
                                               }, searchScope);
    return result;
  }

  public static void processElementParameters(@NotNull SymbolType type,
                                              @NotNull String key,
                                              @NotNull Project project,
                                              @NotNull GlobalSearchScope searchScope,
                                              @NotNull PuppetNamedPsiElementProcessor processor) {
    for (Pair<PuppetExtFunctionInfo, VirtualFile> typeInfo : getSymbolsByKey(type, key, searchScope)) {
      for (PuppetExtFunctionInfo.PuppetExtParamInfo info : typeInfo.getFirst().getParamInfos()) {
        String paramName = info.getParamName();
        processor
          .executeWithName(paramName, getLightProxyElement(project, paramName, typeInfo.getSecond(), info.getOffsetInFile(), type));
      }
    }
  }

  private static @NotNull PsiElement getLightProxyElement(@NotNull Project project,
                                                          @NotNull String name,
                                                          @NotNull VirtualFile file,
                                                          int offset,
                                                          SymbolType type) {
    return new PuppetLazyProxyLightElement(project, name, file, offset, type.getTypeName());
  }

  public static void processAllElements(final @NotNull SymbolType symbolType,
                                        final @NotNull Project project,
                                        final @NotNull GlobalSearchScope searchScope,
                                        @NotNull PuppetNamedPsiElementProcessor processor) {
    FileBasedIndex.getInstance().processAllKeys(KEY, s -> {
      if (s.mySymbolType != symbolType) {
        return true;
      }

      for (PsiElement targetElement : findElementsByKey(symbolType, s.mySymbolName, project, searchScope)) {
        // fixme make boolean
        processor.executeWithName(s.mySymbolName, targetElement);
      }

      return true;
    }, project);
  }

  private static @Nullable SymbolType getSymbolTypeByFileLocation(@NotNull VirtualFile file) {
    assert !file.isDirectory();

    for (SymbolType type : SymbolType.values()) {
      if (isInLocationForType(file, type)) {
        return type;
      }
    }
    return null;
  }

  private static boolean isInLocationForType(@NotNull VirtualFile file, @NotNull SymbolType type) {
    final Collection<List<String>> locations = type.myLocations;

    VirtualFile currentDir = file;

    outer:
    for (List<String> location : locations) {

      for (int i = location.size() - 1; i >= 0; --i) {
        final VirtualFile nextDir = currentDir.getParent();
        if (nextDir == null) {
          continue outer;
        }
        if (!nextDir.getName().equals(location.get(i))) {
          continue outer;
        }

        currentDir = nextDir;
      }

      return true;
    }

    return false;
  }

  public enum SymbolType {
    FUNCTION(
      Arrays.asList(
        Arrays.asList("lib", "puppet", "parser", "functions"),
        Arrays.asList("lib", "stubs")),
      new RubyFunctionIndexer(),
      "puppet.type.names.function_definition"
    ),

    TYPE(
      Collections.singletonList(Arrays.asList("lib", "puppet", "type")),
      new RubyTypeIndexer(),
      "puppet.type.names.resource_definition"
    );

    private final @NotNull Collection<List<String>> myLocations;
    private final @NotNull DataIndexer<EntryKey, PuppetExtFunctionInfo, FileContent> myIndexer;
    private final @NotNull String myKey;

    SymbolType(@NotNull Collection<List<String>> locations,
               @NotNull DataIndexer<EntryKey, PuppetExtFunctionInfo, FileContent> indexer,
               @NotNull @PropertyKey(resourceBundle = PuppetBundle.BUNDLE) String key) {
      myLocations = locations;
      myIndexer = indexer;
      myKey = key;
    }

    public @NotNull @Nls String getTypeName() {
      return PuppetBundle.message(myKey);
    }
  }

  private static class MyKeyDescriptor implements KeyDescriptor<EntryKey> {

    @Override
    public void save(@NotNull DataOutput out, EntryKey value) throws IOException {
      out.writeUTF(value.mySymbolType.name());
      out.writeUTF(value.mySymbolName);
    }

    @Override
    public EntryKey read(@NotNull DataInput in) throws IOException {
      final String typeName = in.readUTF();
      final String symbolName = in.readUTF();

      return new EntryKey(SymbolType.valueOf(typeName), symbolName);
    }

    @Override
    public int getHashCode(EntryKey value) {
      return value.hashCode();
    }

    @Override
    public boolean isEqual(EntryKey val1, EntryKey val2) {
      return val1.equals(val2);
    }
  }

  static final class EntryKey {

    private final @NotNull SymbolType mySymbolType;
    private final @NotNull String mySymbolName;

    EntryKey(@NotNull SymbolType symbolType, @NotNull String symbolName) {
      mySymbolType = symbolType;
      mySymbolName = symbolName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      EntryKey key = (EntryKey)o;

      if (mySymbolType != key.mySymbolType) return false;
      if (!mySymbolName.equals(key.mySymbolName)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = mySymbolType.name().hashCode();
      result = 31 * result + mySymbolName.hashCode();
      return result;
    }
  }
}
