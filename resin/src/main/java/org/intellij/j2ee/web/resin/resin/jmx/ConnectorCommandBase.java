package org.intellij.j2ee.web.resin.resin.jmx;

import com.intellij.javaee.oss.util.AbstractConnectorCommand;
import com.intellij.openapi.diagnostic.Logger;
import org.intellij.j2ee.web.resin.ResinModelBase;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class ConnectorCommandBase<T> extends AbstractConnectorCommand<T> {

  private static final Logger LOG = Logger.getInstance(ConnectorCommandBase.class);

  private final ResinModelBase myResinModel;

  private T myResult;

  public ConnectorCommandBase(ResinModelBase resinModel) {
    myResinModel = resinModel;
  }

  @Override
  protected String getHost() {
    return myResinModel.getCommonModel().getHost();
  }

  @Override
  protected int getJmxPort() {
    return myResinModel.getJmxPort();
  }

  public boolean safeExecute() {
    try {
      myResult = execute();
    }
    catch (TimeoutException | ExecutionException e) {
      LOG.debug(e);
      return false;
    }
    return true;
  }

  public T getResult() {
    return myResult;
  }

  @Nullable
  @Override
  protected String getJmxUsername() {
    return myResinModel.getJmxUsername();
  }

  @Nullable
  @Override
  protected String getJmxPassword() {
    return myResinModel.getJmxPassword();
  }
}
