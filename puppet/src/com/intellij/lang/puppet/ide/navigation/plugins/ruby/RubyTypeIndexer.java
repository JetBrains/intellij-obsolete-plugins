package com.intellij.lang.puppet.ide.navigation.plugins.ruby;

import com.intellij.lang.puppet.ide.navigation.plugins.PuppetExtFunctionInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RubyTypeIndexer implements DataIndexer<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo, FileContent> {
  private static final Pattern RUBY_ADD_TYPE_PATTERN
    = Pattern.compile("newtype\\s*\\((?:\"|'|:)([^ ,)\"]+)(?:\"|')?(?:\\)|,)");
  private static final Pattern RUBY_TYPE_PARAM_PATTERN
    = Pattern.compile("new(?:param|property)\\s*\\((?:\"|'|:)([^ ,)\"]+)(?:\"|')?(?:\\)|,)");
  private static final Pattern RUBY_ENSURABLE_PATTERN
    = Pattern.compile("ensurable");
  private static final String ENSURE_PARAM_NAME = "ensure";

  @Override
  public @NotNull Map<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo> map(@NotNull FileContent inputData) {
    final Map<PuppetRubyPluginsIndex.EntryKey, PuppetExtFunctionInfo> result =
      new HashMap<>();

    final CharSequence fileContents = inputData.getContentAsText();
    final Matcher typeMatcher = RUBY_ADD_TYPE_PATTERN.matcher(fileContents);

    while (typeMatcher.find()) {
      final @NonNls String typeName = typeMatcher.group(1);
      // Recommended by {@link https://docs.puppetlabs.com/guides/custom_types.html#deploying-and-using-types-and-providers}
      if (!inputData.getFileName().equals(typeName + ".rb")) {
        continue;
      }

      final List<PuppetExtFunctionInfo.PuppetExtParamInfo> paramInfos = new ArrayList<>();
      final Matcher paramsMatcher = RUBY_TYPE_PARAM_PATTERN.matcher(fileContents.subSequence(typeMatcher.start(), fileContents.length()));

      while (paramsMatcher.find()) {
        final String paramName = paramsMatcher.group(1);
        final int paramOffset = paramsMatcher.start(1);
        paramInfos.add(new PuppetExtFunctionInfo.PuppetExtParamInfo(typeMatcher.start() + paramOffset, paramName, null));
      }

      final Matcher ensurableMatcher = RUBY_ENSURABLE_PATTERN.matcher(fileContents.subSequence(typeMatcher.start(), fileContents.length()));
      if (ensurableMatcher.find()) {
        final int offset = ensurableMatcher.start();
        paramInfos.add(new PuppetExtFunctionInfo.PuppetExtParamInfo(typeMatcher.start() + offset, ENSURE_PARAM_NAME, null));
      }

      final int typeNameOffset = typeMatcher.start(1);
      result.put(new PuppetRubyPluginsIndex.EntryKey(PuppetRubyPluginsIndex.SymbolType.TYPE, StringUtil.toLowerCase(typeName)),
                 new PuppetExtFunctionInfo(typeNameOffset, paramInfos));
    }

    return result;
  }
}
