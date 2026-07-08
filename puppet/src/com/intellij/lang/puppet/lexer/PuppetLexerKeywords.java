package com.intellij.lang.puppet.lexer;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.util.PuppetConfigurationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PuppetLexerKeywords implements PuppetTokenTypes {
  // this can be used with wordscanner, for example, which knows nothing about the project
  private static final Map<String, IElementType> COMMON_KEYWORDS_MAP = new HashMap<>();
  private static final Map<PuppetLanguage.Version, Map<String, IElementType>> VERSIONED_KEYWORDS_MAP = new HashMap<>();

  static {
    // common used keywrods
    COMMON_KEYWORDS_MAP.put("case", CASE);
    COMMON_KEYWORDS_MAP.put("class", CLASS);
    COMMON_KEYWORDS_MAP.put("default", DEFAULT);
    COMMON_KEYWORDS_MAP.put("define", DEFINE);
    COMMON_KEYWORDS_MAP.put("if", IF);
    COMMON_KEYWORDS_MAP.put("elsif", ELSIF);
    COMMON_KEYWORDS_MAP.put("else", ELSE);
    COMMON_KEYWORDS_MAP.put("inherits", INHERITS);
    COMMON_KEYWORDS_MAP.put("node", NODE);
    COMMON_KEYWORDS_MAP.put("and", AND);
    COMMON_KEYWORDS_MAP.put("or", OR);
    COMMON_KEYWORDS_MAP.put("undef", UNDEF);
    COMMON_KEYWORDS_MAP.put("false", FALSE);
    COMMON_KEYWORDS_MAP.put("true", TRUE);
    COMMON_KEYWORDS_MAP.put("in", IN);
    COMMON_KEYWORDS_MAP.put("unless", UNLESS);

    Map<String, IElementType> puppet3KeywordsMap = new HashMap<>();
    VERSIONED_KEYWORDS_MAP.put(PuppetLanguage.Version.PUPPET_3, puppet3KeywordsMap);

    puppet3KeywordsMap.putAll(COMMON_KEYWORDS_MAP);
    puppet3KeywordsMap.put("import", IMPORT);

    Map<String, IElementType> puppet4KeywordsMap = new HashMap<>();
    VERSIONED_KEYWORDS_MAP.put(PuppetLanguage.Version.PUPPET_4, puppet4KeywordsMap);
    puppet4KeywordsMap.putAll(COMMON_KEYWORDS_MAP);
    puppet4KeywordsMap.put("function", FUNCTION);
    // type is reserved for future use according to https://docs.puppet.com/puppet/4.7/reference/lang_reserved.html
    // but there is a function with such name: https://docs.puppet.com/puppet/latest/reference/function.html#type
    // To avoid tricky parsing, disabling for now
    //puppet4KeywordsMap.put("type", TYPE);
    puppet4KeywordsMap.put("attr", ATTR);
    puppet4KeywordsMap.put("private", PRIVATE);

    // The following tokens exist in reserved form. Later they will be made live subject to a feature switch.
    // these exists in v3 future parser and v4 parser
    puppet4KeywordsMap.put("application", APPLICATION);
    puppet4KeywordsMap.put("consumes", CONSUMES);
    puppet4KeywordsMap.put("produces", PRODUCES);

    // this one exists in v4 parser only, but atm we have no distinct future parser
    puppet4KeywordsMap.put("site", SITE);
  }

  public static TokenSet getAllKeywordsTokenset() {
    TokenSet result = TokenSet.EMPTY;
    for (Map<String, IElementType> map : VERSIONED_KEYWORDS_MAP.values()) {
      Collection<IElementType> values = map.values();
      result = TokenSet.orSet(result, TokenSet.create(values.toArray(IElementType.EMPTY_ARRAY)));
    }
    return result;
  }

  public static @NotNull Map<String, IElementType> getKeywordsMap(@Nullable Project project) {
    return project == null ? COMMON_KEYWORDS_MAP : getKeywordsMap(PuppetConfigurationUtil.getPuppetVersion(project));
  }

  public static @NotNull Map<String, IElementType> getKeywordsMap(@Nullable PuppetLanguage.Version version) {
    if (version == null) {
      return COMMON_KEYWORDS_MAP;
    }
    Map<String, IElementType> keywordsMap = VERSIONED_KEYWORDS_MAP.get(version);
    assert keywordsMap != null : "Keywords map is not defined for puppet version: " + version;
    return keywordsMap;
  }
}
