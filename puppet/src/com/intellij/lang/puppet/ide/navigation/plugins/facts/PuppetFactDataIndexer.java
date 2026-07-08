package com.intellij.lang.puppet.ide.navigation.plugins.facts;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PuppetFactDataIndexer extends DataIndexer<String, Integer, FileContent> {
  String EXTERNAL_FACTS_DIR = "facts.d";

  boolean acceptsFile(@NotNull String fileName, @NotNull String parentDirName);

  @Nullable
  FileType getSuitableFileType();
}
