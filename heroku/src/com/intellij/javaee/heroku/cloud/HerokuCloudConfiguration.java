package com.intellij.javaee.heroku.cloud;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.javaee.heroku.agent.cloud.HerokuCloudAgentConfig;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.util.CloudConfigurationBase;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.Nullable;

/**
 * @author michael.golubev
 */
public class HerokuCloudConfiguration extends CloudConfigurationBase<HerokuCloudConfiguration> implements HerokuCloudAgentConfig {

  private String myApiKey;

  @Attribute("apiKey")
  public String getApiKey() {
    return myApiKey;
  }

  public void setApiKey(String apiKey) {
    myApiKey = apiKey;
  }

  @Transient
  public void setApiKeySafe(String apiKey) {
    CredentialAttributes credentialAttributes = createCredentialAttributes(true);
    doSetSafeValue(credentialAttributes, getCredentialUser(), apiKey, this::setApiKey);
  }

  @Transient
  @Override
  public String getApiKeySafe() {
    CredentialAttributes apiKeyAttributes = createCredentialAttributes(true);
    return doGetSafeValue(apiKeyAttributes, this::getApiKey);
  }

  @Override
  @Transient
  public void setPasswordSafe(String password) {
    CredentialAttributes passwordAttributes = createCredentialAttributes(false);
    doSetSafeValue(passwordAttributes, getCredentialUser(), password, this::setPassword);
  }

  @Override
  public String getPasswordSafe() {
    CredentialAttributes passwordAttributes = createCredentialAttributes(false);
    return doGetSafeValue(passwordAttributes, this::getPassword);
  }

  @Override
  public boolean isPasswordSafe() {
    return hasSafeCredentials(createCredentialAttributes(false));
  }

  @Transient
  public boolean isApiKeySafe() {
    return hasSafeCredentials(createCredentialAttributes(true));
  }

  @Override
  @Nullable
  protected String getServiceName() {
    // should not be called, we have two separate credentials for password and api key
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  protected CredentialAttributes createCredentialAttributes() {
    throw new UnsupportedOperationException();
  }

  private CredentialAttributes createCredentialAttributes(boolean apiKeyNotPassword) {
    String credentialUser = getCredentialUser();
    String serviceName = createServiceName(apiKeyNotPassword);
    return createCredentialAttributes(serviceName, credentialUser);
  }

  private String createServiceName(boolean apiKeyNotPassword) {
    String email = getEmail();
    String kind = apiKeyNotPassword ? "api key" : "password";
    return StringUtil.isEmpty(email) ? null : CredentialAttributesKt.generateServiceName("Heroku", email + " - " + kind);
  }

  @Override
  public boolean shouldMigrateToPasswordSafe() {
    return !StringUtil.isEmpty(myApiKey) || super.shouldMigrateToPasswordSafe();
  }

  @Override
  public void migrateToPasswordSafe() {
    super.migrateToPasswordSafe();

    final String unsafeApiKey = myApiKey;
    if (!StringUtil.isEmpty(unsafeApiKey)) {
      setApiKeySafe(unsafeApiKey);
    }
  }
}
