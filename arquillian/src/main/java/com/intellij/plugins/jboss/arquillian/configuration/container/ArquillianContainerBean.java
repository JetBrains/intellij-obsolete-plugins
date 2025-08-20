package com.intellij.plugins.jboss.arquillian.configuration.container;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"NotNullFieldNotInitialized"})
public class ArquillianContainerBean {
  @NlsSafe
  @NotNull
  @Attribute("name")
  public String name;

  @NotNull
  @Attribute("id")
  public String id;

  @NotNull
  @Tag("url")
  public String url;

  @NotNull
  @Attribute("kind")
  public ArquillianContainerKind kind;

  @Property(surroundWithTag = false)
  @XCollection
  public List<MavenDependency> dependencies = new ArrayList<>();

  @Tag("mavenDependency")
  public static final class MavenDependency {
    @NotNull
    @Attribute("groupId")
    public String groupId;

    @NotNull
    @Attribute("artifactId")
    public String artifactId;
  }
}
