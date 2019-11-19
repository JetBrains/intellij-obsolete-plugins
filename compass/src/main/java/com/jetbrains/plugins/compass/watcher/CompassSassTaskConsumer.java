package com.jetbrains.plugins.compass.watcher;

import org.jetbrains.plugins.sass.SASSFileType;
import org.jetbrains.plugins.scss.watcher.CompassSassScssTaskConsumerBase;

public class CompassSassTaskConsumer extends CompassSassScssTaskConsumerBase {
  public CompassSassTaskConsumer() {
    super(SASSFileType.SASS);
  }

  @Override
  public String getConsumeMessage() {
    return "Enable File Watcher to compile SASS to CSS using Compass?";
  }
}
