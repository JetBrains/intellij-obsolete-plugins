package com.intellij.dmserver.artifacts;

import com.intellij.openapi.module.Module;

import java.util.EventListener;

public interface ManifestManagerListener extends EventListener {

  void manifestCreated(Module module, ManifestManager.FileWrapper manifest);
}
