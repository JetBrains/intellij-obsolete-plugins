package com.intellij.javaee.heroku.cloud;

import com.intellij.remoteServer.runtime.deployment.debug.JavaDebugConnectionData;
import com.intellij.remoteServer.util.ServerRuntimeException;

/**
 * @author michael.golubev
 */
public interface HerokuDebugConnectionProvider {

  JavaDebugConnectionData getDebugConnectionData() throws ServerRuntimeException;
}
