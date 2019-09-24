package com.intellij.javaee.heroku.agent.cloud;

import com.intellij.remoteServer.agent.util.CloudAgentConfigBase;

/**
 * @author michael.golubev
 */
public interface HerokuCloudAgentConfig extends CloudAgentConfigBase {
  String getApiKeySafe();
}
