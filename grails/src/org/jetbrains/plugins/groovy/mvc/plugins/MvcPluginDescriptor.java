// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MvcPluginDescriptor {

  public static final MvcPluginDescriptor[] EMPTY_ARRAY = new MvcPluginDescriptor[0];

  private final String myName;

  private Release myLastRelease;

  private final List<Release> releases = new ArrayList<>();

  public MvcPluginDescriptor(@NotNull String name) {
    this.myName = name;
  }

  public @NlsSafe String getName() {
    return myName;
  }

  public Release getLastRelease() {
    return myLastRelease;
  }

  public void setLastRelease(Release lastRelease) {
    myLastRelease = lastRelease;
  }

  public List<Release> getReleases() {
    return releases;
  }

  public @Nullable @NlsSafe String getLatestVersion() {
    return myLastRelease == null ? null : myLastRelease.getVersion();
  }

  public @Nullable @NlsSafe String getTitle() {
    return myLastRelease == null ? null : myLastRelease.getTitle();
  }

  @Override
  public String toString() {
    return myName;
  }

  public static class Release {
    private final MvcPluginDescriptor myPlugin;
    private final String myVersion;
    private final String myType;
    private final String myTitle;
    private final String myAuthor;
    private final String myDescription;
    private final String myEmail;
    private final String myZipRelease;
    private final String myDocumentation;

    public Release(MvcPluginDescriptor plugin, String version, String type, String title, String author, String description, String email, String zipRelease, String documentation) {
      myPlugin = plugin;
      myTitle = title;
      myType = type;
      myVersion = version;
      myAuthor = author;
      myDescription = description;
      myEmail = email;
      myZipRelease = zipRelease;
      myDocumentation = documentation;
    }

    public @NlsSafe String getTitle() {
      return myTitle;
    }

    public @NlsSafe String getAuthor() {
      return myAuthor;
    }

    public @NlsSafe String getDescription() {
      return myDescription;
    }

    public @NlsSafe String getEmail() {
      return myEmail;
    }

    public @NlsSafe String getZipRelease() {
      return myZipRelease;
    }

    public @NlsSafe String getDocumentation() {
      return myDocumentation;
    }

    public @NlsSafe String getVersion() {
      return myVersion;
    }

    public String getType() {
      return myType;
    }

    public MvcPluginDescriptor getPlugin() {
      return myPlugin;
    }
  }
}
