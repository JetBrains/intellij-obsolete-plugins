package com.intellij.gwt.superSource;

import com.intellij.gwt.module.index.GwtModuleRenameToIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.externalizer.StringCollectionExternalizer;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.xml.NanoXmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GwtModuleSuperSourceIndex extends FileBasedIndexExtension<String, List<String>> {
  private static final ID<String,List<String>> NAME = ID.create("GwtXmlFileSuperSource");
  private static final String KEY = "";
  private static final @NonNls String SUPER_SOURCE_PATH = ".module.super-source";

  @Override
  public @NotNull ID<String, List<String>> getName() {
    return NAME;
  }

  @Override
  public @NotNull DataIndexer<String, List<String>, FileContent> getIndexer() {
    return new DataIndexer<>() {
      @Override
      public @NotNull Map<String, List<String>> map(final @NotNull FileContent inputData) {
        final Map<String, List<String>> data = new HashMap<>();
        NanoXmlUtil.parse(CharArrayUtil.readerFromCharSequence(inputData.getContentAsText()), new NanoXmlUtil.BaseXmlBuilder() {
          public boolean mySuperSourcePathAdded;

          @Override
          public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
            super.startElement(name, nsPrefix, nsURI, systemID, lineNr);
            if (isInSuperSourceTag()) {
              mySuperSourcePathAdded = false;
            }
          }

          @Override
          public void endElement(String name, String nsPrefix, String nsURI) throws Exception {
            if (isInSuperSourceTag() && !mySuperSourcePathAdded) {
              addUrl("");
            }
            super.endElement(name, nsPrefix, nsURI);
          }

          @Override
          public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception {
            if ("path".equals(key) && isInSuperSourceTag()) {
              if (value != null) {
                mySuperSourcePathAdded = true;
                addUrl(StringUtil.trimStart(value, "/"));
              }
              throw NanoXmlUtil.ParserStoppedXmlException.INSTANCE;
            }
          }

          private boolean isInSuperSourceTag() {
            return SUPER_SOURCE_PATH.equals(getLocation());
          }

          private void addUrl(final String relativePath) {
            data.computeIfAbsent(KEY, _ -> new SmartList<>()).add(relativePath);
          }
        });
        return data;
      }
    };
  }

  @Override
  public @NotNull KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public @NotNull DataExternalizer<List<String>> getValueExternalizer() {
    return StringCollectionExternalizer.STRING_LIST_EXTERNALIZER;
  }

  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    return GwtModuleRenameToIndex.Holder.GWT_XML_FILE_INPUT_FILTER;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 1;
  }

  public static @NotNull List<VirtualFile> getSuperSourceRoots(@NotNull GlobalSearchScope scope) {
    Set<VirtualFile> result = new HashSet<>();
    FileBasedIndex.getInstance().processValues(NAME, KEY, null, (file, relativePaths) -> {
      for (String relativePath : relativePaths) {
        ContainerUtil.addIfNotNull(result, VfsUtilCore.findRelativeFile(relativePath, file.getParent()));
      }
      return true;
    }, scope);
    return new SmartList<>(result);
  }
}
