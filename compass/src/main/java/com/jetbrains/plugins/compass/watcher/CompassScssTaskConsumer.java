package com.jetbrains.plugins.compass.watcher;

import org.jetbrains.plugins.scss.SCSSFileType;

public class CompassScssTaskConsumer extends CompassSassScssTaskConsumerBase {
  public CompassScssTaskConsumer() {
    super(SCSSFileType.SCSS);
  }

  @Override
  public String getConsumeMessage() {
    return "Enable File Watcher to compile SCSS to CSS using Compass?";
  }
}
