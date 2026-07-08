package com.intellij.lang.puppet;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anna Bulenkova
 */
public final class PuppetLanguage extends Language {
  private static final String LANGUAGE_ID = "Puppet";

  public static final PuppetLanguage INSTANCE = new PuppetLanguage();

  private PuppetLanguage() {
    super(LANGUAGE_ID);
  }

  @Override
  public @NotNull String getDisplayName() {
    return "Puppet";
  }

  private static @NotNull String getIdWithVersion(@Nullable String version) {
    if (version == null) {
      return LANGUAGE_ID;
    }
    else {
      return LANGUAGE_ID + " " + version;
    }
  }

  public enum Version {
    PUPPET_3 {
      @Override
      public @NotNull String getVersionString() {
        return "3.X";
      }
    },
    PUPPET_4 {
      @Override
      public @NotNull String getVersionString() {
        return "4";
      }
    };

    public abstract @NotNull String getVersionString();

    @Override
    public @NotNull String toString() {
      return getIdWithVersion(getVersionString());
    }
  }
}
