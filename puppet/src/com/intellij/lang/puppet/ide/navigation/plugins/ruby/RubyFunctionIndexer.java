package com.intellij.lang.puppet.ide.navigation.plugins.ruby;

import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.ide.navigation.plugins.PuppetExtFunctionInfo;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RubyFunctionIndexer implements DataIndexer<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo, FileContent> {

  private static final Pattern RUBY_ADD_FUNCTION_PATTERN
    = Pattern.compile("newfunction\\s*\\((?:\"|'|:)([^ ,)\"]+)(?:\"|')?(?:\\)|,)");

  @Override
  public @NotNull Map<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo> map(@NotNull FileContent inputData) {
    final Map<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo> result =
      new HashMap<>();

    final Matcher matcher = RUBY_ADD_FUNCTION_PATTERN.matcher(inputData.getContentAsText());

    while (matcher.find()) {
      final String functionName = matcher.group(1);
      // Recommended by {@link https://docs.puppetlabs.com/guides/custom_functions.html#where-to-put-your-functions}
      if (!PuppetLibraryUtil.isFunctionsStubsFile(inputData.getFile()) && !inputData.getFileName().equals(functionName + ".rb")) {
        continue;
      }

      result.put(new PuppetRubyPluginsIndex.EntryKey(PuppetRubyPluginsIndex.SymbolType.FUNCTION, functionName),
                 new PuppetExtFunctionInfo(matcher.start(1), Collections.emptyList()));
    }

    return result;
  }
}
