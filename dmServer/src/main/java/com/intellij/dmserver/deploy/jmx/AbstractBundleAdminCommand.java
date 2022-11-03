package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.DMConstants;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.springsource.server.management.remote.BundleAdmin;
import org.jetbrains.annotations.Nullable;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractBundleAdminCommand<T> extends AbstractDMConnectorCommand<T> {
  private static boolean ourUnrecoverableError = false;

  public AbstractBundleAdminCommand(DMServerInstance dmServer) {
    super(dmServer);
  }

  protected void checkBundleAdminAndInstall(MBeanServerConnection connection) throws IOException {
    if (ourUnrecoverableError) {
      throw new IOException("We were unable to install connector bundle before. This is unrecoverable condition");
    }
    try {
      connection.getObjectInstance(DMConstants.MBEAN_BUNDLE_ADMIN);
    }
    catch (InstanceNotFoundException e) {
      VirtualFile connectorBundle = getConnectorBundle();
      if (connectorBundle == null) {
        ourUnrecoverableError = true;
        throw new IOException("Can't find connector bundle");
      }

      try {
        new ConnectorFileDeployCommand(getServerInstance(), connectorBundle).execute();
      }
      catch (TimeoutException | ExecutionException ex) {
        throw new IOException(ex);
      }
    }
  }

  @Nullable
  private static VirtualFile getConnectorBundle() {
    return LocalFileSystem.getInstance().findFileByPath(PathUtil.getJarPathForClass(BundleAdmin.class));
  }
}
