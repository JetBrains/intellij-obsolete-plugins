package com.intellij.lang.puppet.ide.navigation.plugins.facts;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RubyFactIndexer implements PuppetFactDataIndexer {

  private static final Pattern RUBY_ADD_FACT_PATTERN
    = Pattern.compile("Facter\\s*\\.\\s*add\\s*\\((?:\"|'|:)([^ )\"']+)(?:\"|')?\\s*(?:\\)|,)");

  @Override
  public @NotNull Map<String, Integer> map(@NotNull FileContent inputData) {
    final Map<String, Integer> result = new HashMap<>();
    final Matcher matcher = RUBY_ADD_FACT_PATTERN.matcher(inputData.getContentAsText());

    while (matcher.find()) {
      final String factName = matcher.group(1);
      result.put(factName, matcher.start(1));
    }

    return result;
  }

  @Override
  public boolean acceptsFile(@NotNull String fileName, @NotNull String parentDirName) {
    return parentDirName.equals("facter") && StringUtil.endsWithIgnoreCase(fileName, ".rb");
  }

  @Override
  public @Nullable FileType getSuitableFileType() {
    return FileTypeRegistry.getInstance().findFileTypeByName("Ruby");
  }
}