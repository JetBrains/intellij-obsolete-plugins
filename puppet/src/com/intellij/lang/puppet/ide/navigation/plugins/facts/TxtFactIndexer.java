package com.intellij.lang.puppet.ide.navigation.plugins.facts;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TxtFactIndexer implements PuppetFactDataIndexer {

  private static final Pattern TXT_FACT_PATTERN = Pattern.compile("^(\\S+)=.*$", Pattern.MULTILINE);

  @Override
  public @NotNull Map<String, Integer> map(@NotNull FileContent inputData) {
    final Map<String, Integer> result = new HashMap<>();
    final Matcher matcher = TXT_FACT_PATTERN.matcher(inputData.getContentAsText());

    while (matcher.find()) {
      final String factName = matcher.group(1);
      result.put(factName, matcher.start());
    }

    return result;
  }

  @Override
  public boolean acceptsFile(@NotNull String fileName, @NotNull String parentDirName) {
    return parentDirName.equals(EXTERNAL_FACTS_DIR) && StringUtil.endsWithIgnoreCase(fileName, ".txt");
  }

  @Override
  public @NotNull FileType getSuitableFileType() {
    return PlainTextFileType.INSTANCE;
  }
}
