package com.intellij.javaee.heroku.cloud.module;

import com.intellij.javaee.heroku.cloud.HerokuCloudConfiguration;
import com.intellij.javaee.heroku.cloud.HerokuCloudType;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.impl.module.CloudModuleBuilder;
import com.intellij.remoteServer.impl.module.CloudModuleBuilderContribution;
import com.intellij.remoteServer.impl.module.CloudModuleBuilderContributionFactory;

/**
 * @author michael.golubev
 */
public class HerokuModuleBuilderContributionFactory extends CloudModuleBuilderContributionFactory {

  @Override
  public ServerType<HerokuCloudConfiguration> getCloudType() {
    return HerokuCloudType.getInstance();
  }

  @Override
  public CloudModuleBuilderContribution createContribution(CloudModuleBuilder moduleBuilder) {
    return new HerokuModuleBuilderContribution(moduleBuilder, getCloudType());
  }
}
