package com.intellij.lang.puppet.project.meta;

import org.jetbrains.annotations.Nullable;

/**
 * Metadata for implicit modules without metadata file
 */
public class PuppetHeadlessModuleMetadata extends PuppetModuleMetadata {

  /**
   * @param name arbitrary name to be shown in project tree. e.g. folder name
   */
  public PuppetHeadlessModuleMetadata(@Nullable String name) {
    setName(name);
  }
}
