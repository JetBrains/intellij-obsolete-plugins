package com.intellij.dmserver.install;

import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DMServerInstallation {

  VirtualFile getHome();

  VirtualFile[] getSharedLibraries();

  VirtualFile getTempFolder();

  @Nullable
  VirtualFile getSystemLibraryFolder();

  boolean isValid();

  ServerVersionHandler getServerVersion();

  ApplicationServer getOrCreateApplicationServer();

  DMServerConfigSupport getConfigSupport();

  List<RepositoryPattern> collectRepositoryPatterns();

  String getVersionName();
}
