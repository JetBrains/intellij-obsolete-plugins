package com.intellij.lang.javascript.linter.jscs;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Irina.Chernushina on 10/6/2014.
 */
public enum JscsPreset {
  airbnb("airbnb", "Airbnb", "https://github.com/airbnb/javascript"),
  crockford("crockford", "Crockford", "http://javascript.crockford.com/code.html"),
  google("google", "Google", "https://google.github.io/styleguide/jsguide"),
  grunt("grunt", "Grunt", "http://gruntjs.com/contributing#syntax"),
  idiomatic("idiomatic", "Idiomatic", "https://github.com/rwaldron/idiomatic.js#idiomatic-style-manifesto"),
  jquery("jquery", "jQuery", "https://contribute.jquery.org/style-guide/js/"),
  mdcs("mdcs", "MDCS", "https://github.com/mrdoob/three.js/wiki/Mr.doob's-Code-Style%E2%84%A2"),
  node_style_guide("node-style-guide", "node-style-guide", "https://github.com/felixge/node-style-guide"),
  wikimedia("wikimedia", "Wikimedia", "https://www.mediawiki.org/wiki/Manual:Coding_conventions/JavaScript"),
  wordpress("Wordpress", "wordpress.json", "https://make.wordpress.org/core/handbook/coding-standards/javascript/"),
  yandex("yandex", "Yandex", "https://github.com/ymaps/codestyle/blob/master/js.md");

  private static final Logger LOG = Logger.getInstance(JscsConfiguration.LOG_CATEGORY);
  public static final String COMMON_DESCRIPTION = "https://www.npmjs.org/package/jscs#presets";

  private final String myCode;
  private final String myDisplayName;
  private final String myDescribeUrl;

  JscsPreset(String code, String displayName, String describeUrl) {
    myCode = code;
    myDisplayName = displayName;
    myDescribeUrl = describeUrl;
  }

  public String getCode() {
    return myCode;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  public String getDescribeUrl() {
    return myDescribeUrl;
  }

  public static String[] stringValues() {
    final JscsPreset[] values = values();
    final String[] s = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      s[i] = values[i].name();
    }
    return s;
  }

  public static JscsPreset safeValueOf(@NotNull final String name) {
    try {
      return valueOf(name);
    } catch (IllegalArgumentException e) {
      LOG.debug("JSCS: wrong value of preset: " + name);
    }
    return null;
  }
}
