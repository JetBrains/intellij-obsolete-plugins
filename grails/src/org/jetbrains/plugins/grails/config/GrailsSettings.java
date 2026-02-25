// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyLexer;
import org.jetbrains.plugins.groovy.lang.lexer.TokenSets;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

public final class GrailsSettings {
  public @Nullable Integer buildConfigCrc;

  public int pluginDependenciesCrc;

  public int pluginsCrc;

  public String fixedGrailsVersion; // Don't offer to upgrade application if application has this version.

  public Map<String, String> properties = new HashMap<>();

  /**
   * Map plugin name to plugin path.
   * Note: plugin name may be in snake format (e.g. 'my-plugin' instead 'myPlugin')
   */
  public Map<String, String> customPluginLocations = new HashMap<>();

  public boolean isBuildConfigOutdated(@Nullable String text) {
    return buildConfigCrc == null || buildConfigCrc != getScriptCrc(text);
  }

  public void updateBuildConfig(@Nullable String text) {
    buildConfigCrc = text == null ? 0 : getScriptCrc(text);
  }

  public boolean hasParsedBuildConfig() {
    return buildConfigCrc != null;
  }

  public static int getScriptCrc(@Nullable String text) {
    if (text == null) return 0;

    Lexer lexer = new GroovyLexer();
    lexer.start(text);

    CRC32 crc = new CRC32();

    while (true) {
      IElementType tokenType = lexer.getTokenType();
      if (tokenType == null) break;

      if (TokenSets.WHITE_SPACES_SET.contains(tokenType)) {
        crc.update(1);
      }
      else if (TokenSets.COMMENT_SET.contains(tokenType)) {
        crc.update(2);
      }
      else {
        for (int start = lexer.getTokenStart(), end = lexer.getTokenEnd(); start < end; start++) {
          char a = text.charAt(start);
          crc.update(a);
          crc.update(a >>> 8);
        }
      }

      lexer.advance();
    }

    return (int)crc.getValue();
  }
}
