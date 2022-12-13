package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.deploy.IDMCommand;
import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.oss.util.AbstractConnectorCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractDMConnectorCommand<T> implements IDMCommand<T> {

  private final DMServerInstance myDmServer;

  private final AbstractConnectorCommand<T> myCommonCommand;

  public AbstractDMConnectorCommand(@NotNull DMServerInstance dmServer) {
    myDmServer = dmServer;
    myCommonCommand = new AbstractConnectorCommand<>() {

      @Override
      protected T doExecute(MBeanServerConnection connection) throws JMException, IOException {
        return AbstractDMConnectorCommand.this.doExecute(connection);
      }

      @Override
      protected String getHost() {
        return getServerInstance().getCommonModel().getHost();
      }

      @Override
      protected int getJmxPort() {
        return getServerInstance().getServerModel().getMBeanServerPort();
      }
    };
  }

  protected final DMServerInstance getServerInstance() {
    return myDmServer;
  }

  protected final ServerVersionHandler getServerVersion() {
    return getServerInstance().getVersionHandler();
  }

  @Override
  @Nullable
  public final T execute() throws TimeoutException, ExecutionException {
    if (!prepareExecution()) {
      return null;
    }
    return myCommonCommand.execute();
  }

  protected static <R> R invokeOperation(MBeanServerConnection connection,
                                         ObjectName objectName,
                                         String operationName,
                                         Object... operationArguments) throws JMException, IOException {
    return AbstractConnectorCommand.invokeOperation(connection, objectName, operationName, operationArguments);
  }

  protected boolean prepareExecution() {
    return true;
  }

  @Nullable
  protected abstract T doExecute(MBeanServerConnection connection) throws JMException, IOException;
}
