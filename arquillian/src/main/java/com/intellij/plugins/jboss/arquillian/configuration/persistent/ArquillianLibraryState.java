package com.intellij.plugins.jboss.arquillian.configuration.persistent;

public abstract class ArquillianLibraryState implements ArquillianState {

  public abstract <R> R accept(Visitor<R> visitor);

  public interface Visitor<R> {
    R visitMavenLibrary(ArquillianMavenLibraryState state);

    R visitExistLibrary(ArquillianExistLibraryState state);
  }
}
