package com.intellij.gwt.module.index;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.xml.NanoXmlBuilder;
import com.intellij.util.xml.NanoXmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class GwtModuleRenameToIndex extends ScalarIndexExtension<String> {
  private static final ID<String,Void> NAME = ID.create("GwtXmlFile");
  public static final class Holder {
    public static final FileBasedIndex.InputFilter GWT_XML_FILE_INPUT_FILTER = new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE) {
      @Override
      public boolean acceptInput(@NotNull VirtualFile file) {
        return file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX);
      }
    };
  }

  @Override
  public @NotNull ID<String, Void> getName() {
    return NAME;
  }

  @Override
  public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
    return new DataIndexer<>() {
      @Override
      public @NotNull Map<String, Void> map(@NotNull FileContent inputData) {
        final Map<String, Void> data = new HashMap<>();
        NanoXmlUtil.parse(CharArrayUtil.readerFromCharSequence(inputData.getContentAsText()), new NanoXmlBuilder() {
          @Override
          public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
            if (!"module".equals(name)) {
              throw NanoXmlUtil.ParserStoppedXmlException.INSTANCE;
            }
          }

          @Override
          public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception {
            if ("rename-to".equals(key)) {
              if (value != null) {
                data.put(value, null);
              }
              throw NanoXmlUtil.ParserStoppedXmlException.INSTANCE;
            }
          }

          @Override
          public void elementAttributesProcessed(String name, String nsPrefix, String nsURI) throws Exception {
            throw NanoXmlUtil.ParserStoppedXmlException.INSTANCE;
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
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    return Holder.GWT_XML_FILE_INPUT_FILTER;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  public static Collection<VirtualFile> getGwtXmlFiles(@NotNull String renameToAttribute, @NotNull GlobalSearchScope scope) {
    return FileBasedIndex.getInstance().getContainingFiles(NAME, renameToAttribute, scope);
  }
}
