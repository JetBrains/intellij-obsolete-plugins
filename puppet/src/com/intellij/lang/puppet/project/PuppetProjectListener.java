package com.intellij.lang.puppet.project;

import java.util.EventListener;

public interface PuppetProjectListener extends EventListener {
  default void projectUpdated() {}
}
