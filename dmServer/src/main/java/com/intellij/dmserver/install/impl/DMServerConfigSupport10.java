package com.intellij.dmserver.install.impl;


import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.integration.DMServerRepositoryItem;
import com.intellij.dmserver.integration.DMServerRepositoryItem10;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DMServerConfigSupport10 extends DMServerConfigSupportBase {

  private static final Logger LOG = Logger.getInstance(DMServerConfigSupport10.class);

  @NonNls
  private static final List<String> DEFAULT_REPOSITORY_PATHS = Arrays.asList(
    "repository/bundles/subsystems/{name}/{bundle}.jar",
    "repository/bundles/ext/{bundle}",
    "repository/bundles/usr/{bundle}",
    "repository/libraries/ext/{library}",
    "repository/libraries/usr/{library}");

  private static final ConfigElement<?>[] ourServerConfigElements = new ConfigElement<?>[]{ //
    new ObjectConfigElement("serviceability", new ConfigElement<?>[]{//
      new ObjectConfigElement("dump", new ConfigElement<?>[]{//
        new StringConfigElement("directory", ourDumpsFolderWrapper)
      })
    }), //
    new ObjectConfigElement("osgiConsole", new ConfigElement<?>[]{//
      new BooleanConfigElement("enabled", ourShellEnabledWrapper), //
      new IntegerConfigElement("port", ourShellPortWrapper) //
    }), //
    new StringConfigElement("pickupDirectory", ourPickupFolderWrapper), //
    new ObjectConfigElement("provisioning", new ConfigElement<?>[]{ //
      new RepositoryConfigElement("searchPaths") //
    }) //
  };

  private static final ConfigElement<?>[] ourDeployerConfigElements = new ConfigElement<?>[]{ //
    new ObjectConfigElement("deployer", new ConfigElement<?>[]{//
      new IntegerConfigElement("deploymentTimeoutSeconds", ourDeploymentTimeoutSecsWrapper) //
    })
  };


  private final VirtualFile myServerConfigFile;
  private final VirtualFile myDeployerConfigFile;

  public DMServerConfigSupport10(VirtualFile home) {
    myServerConfigFile = home.findFileByRelativePath("config/server.config");
    myDeployerConfigFile = home.findFileByRelativePath("config/deployer.config");
  }

  @Override
  protected List<VirtualFile> getFiles() {
    return Arrays.asList(myServerConfigFile, myDeployerConfigFile);
  }

  @Override
  public void readFromServer(DMServerIntegrationData data) {
    try {

      myServerConfigFile.refresh(false, false);
      JSONObject serverConfig = JsonUtil.loadConfig(myServerConfigFile);
      for (ConfigElement<?> element : ourServerConfigElements) {
        element.load(serverConfig, data);
      }

      myDeployerConfigFile.refresh(false, false);
      JSONObject deployerConfig = JsonUtil.loadConfig(myDeployerConfigFile);
      for (ConfigElement<?> element : ourDeployerConfigElements) {
        element.load(deployerConfig, data);
      }
    }
    catch (IOException | JSONException e) {
      LOG.error(e);
    }
  }

  @Override
  public void writeToServer(final DMServerIntegrationData data) {
    try {

      // TODO: may process the situation when there is no config
      JSONObject serverConfig = JsonUtil.loadConfig(myServerConfigFile);
      for (ConfigElement<?> element : ourServerConfigElements) {
        element.save(serverConfig, data);
      }
      JsonUtil.saveConfig(myServerConfigFile, serverConfig);

      // TODO: may process the situation when there is no config
      JSONObject deployerConfig = JsonUtil.loadConfig(myDeployerConfigFile);
      for (ConfigElement<?> element : ourDeployerConfigElements) {
        element.save(deployerConfig, data);
      }
      JsonUtil.saveConfig(myDeployerConfigFile, deployerConfig);
    }
    catch (IOException | JSONException e) {
      LOG.error(e);
    }
  }


  private static abstract class ConfigElement<T> {

    private final String myElementName;

    ConfigElement(@NonNls String elementName) {
      myElementName = elementName;
    }

    protected final String getElementName() {
      return myElementName;
    }

    public void load(JSONObject source, DMServerIntegrationData data) throws JSONException {
      boolean hasValue = source != null && source.has(getElementName());
      setValue(data, hasValue ? loadValue(source) : null);
    }

    public abstract void save(JSONObject target, DMServerIntegrationData data) throws JSONException;//

    protected abstract T loadValue(JSONObject source) throws JSONException;

    protected abstract void setValue(DMServerIntegrationData data, T value) throws JSONException;
  }

  private static class ObjectConfigElement extends ConfigElement<JSONObject> {

    private final ConfigElement<?>[] myChildren;

    ObjectConfigElement(@NonNls String elementName, ConfigElement<?>[] children) {
      super(elementName);
      myChildren = children;
    }

    @Override
    protected JSONObject loadValue(JSONObject source) throws JSONException {
      return source.getJSONObject(getElementName());
    }

    @Override
    public void save(JSONObject target, DMServerIntegrationData data) throws JSONException {
      JSONObject childTarget;
      if (target.has(getElementName())) {
        childTarget = target.getJSONObject(getElementName());
      }
      else {
        childTarget = new JSONObject();
        target.put(getElementName(), childTarget);
      }
      for (ConfigElement<?> child : myChildren) {
        child.save(childTarget, data);
      }
    }

    @Override
    protected void setValue(DMServerIntegrationData data, JSONObject value) throws JSONException {
      for (ConfigElement<?> child : myChildren) {
        child.load(value, data);
      }
    }
  }

  private static class RepositoryConfigElement extends ConfigElement<JSONArray> {

    RepositoryConfigElement(@NonNls String elementName) {
      super(elementName);
    }

    @Override
    public void save(JSONObject target, DMServerIntegrationData data) throws JSONException {
      target.put(getElementName(), getValue(data));
    }

    @Override
    protected JSONArray loadValue(JSONObject source) throws JSONException {
      return source.getJSONArray(getElementName());
    }

    private static JSONArray getValue(DMServerIntegrationData data) {
      List<String> paths = new ArrayList<>();
      for (DMServerRepositoryItem item : data.getRepositoryItems()) {
        if (!(item instanceof DMServerRepositoryItem10)) {
          continue;
        }
        DMServerRepositoryItem10 item10 = (DMServerRepositoryItem10)item;
        paths.add(item10.getPath());
      }
      return new JSONArray(paths);
    }

    @Override
    protected void setValue(DMServerIntegrationData data, JSONArray value) throws JSONException {
      List<DMServerRepositoryItem> items = new ArrayList<>();
      if (value == null) {
        for (String path : DEFAULT_REPOSITORY_PATHS) {
          DMServerRepositoryItem10 item = new DMServerRepositoryItem10();
          item.setPath(path);
          items.add(item);
        }
      }
      else {
        for (int i = 0; i < value.length(); i++) {
          DMServerRepositoryItem10 item = new DMServerRepositoryItem10();
          item.setPath(value.getString(i));
          items.add(item);
        }
      }
      data.setRepositoryItems(items);
    }
  }

  private static abstract class PrimitiveConfigElement<T> extends ConfigElement<T> {

    private final ConfigElementWrapper<T> myElementWrapper;

    PrimitiveConfigElement(@NonNls String elementName, ConfigElementWrapper<T> elementWrapper) {
      super(elementName);
      myElementWrapper = elementWrapper;
    }

    @Override
    public void save(JSONObject target, DMServerIntegrationData data) throws JSONException {
      target.put(getElementName(), getValue(data));
    }

    private T getValue(DMServerIntegrationData data) {
      return myElementWrapper.getValue(data);
    }

    @Override
    protected void setValue(DMServerIntegrationData data, T value) throws JSONException {
      if (value != null) {
        myElementWrapper.setValue(data, value);
      }
    }
  }

  private static class BooleanConfigElement extends PrimitiveConfigElement<Boolean> {

    BooleanConfigElement(@NonNls String elementName, ConfigElementWrapper<Boolean> elementWrapper) {
      super(elementName, elementWrapper);
    }

    @Override
    protected Boolean loadValue(JSONObject source) throws JSONException {
      return source.getBoolean(getElementName());
    }
  }

  private static class IntegerConfigElement extends PrimitiveConfigElement<Integer> {

    IntegerConfigElement(@NonNls String elementName, ConfigElementWrapper<Integer> elementWrapper) {
      super(elementName, elementWrapper);
    }

    @Override
    protected Integer loadValue(JSONObject source) throws JSONException {
      return source.getInt(getElementName());
    }
  }

  private static class StringConfigElement extends PrimitiveConfigElement<String> {

    StringConfigElement(@NonNls String elementName, ConfigElementWrapper<String> elementWrapper) {
      super(elementName, elementWrapper);
    }

    @Override
    protected String loadValue(JSONObject source) throws JSONException {
      return source.getString(getElementName());
    }
  }
}
