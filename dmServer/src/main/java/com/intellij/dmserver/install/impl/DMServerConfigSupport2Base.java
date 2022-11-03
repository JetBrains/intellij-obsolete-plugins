package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.integration.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.*;

public class DMServerConfigSupport2Base extends DMServerConfigSupportBase {

  private static final Logger LOG = Logger.getInstance(DMServerConfigSupport20.class);

  private static final ConfigElement<?>[] ourKernelConfigElements = new ConfigElement<?>[]{ //
    new BooleanConfigElement("shell.enabled", ourShellEnabledWrapper), //
    new IntegerConfigElement("shell.port", ourShellPortWrapper), //
    new IntegerConfigElement("deployer.timeout", ourDeploymentTimeoutSecsWrapper), //
    new StringConfigElement("deployer.pickupDirectory", ourPickupFolderWrapper) //
  };

  private static final ConfigElement<?>[] ourMedicConfigElements = new ConfigElement<?>[]{ //
    new StringConfigElement("dump.root.directory", ourDumpsFolderWrapper), //
    new BooleanConfigElement("log.wrapSysOut", new ConfigElementWrapper<>() {
      @Override
      public Boolean getValue(DMServerIntegrationData data) {
        return data.isWrapSystemOut();
      }

      @Override
      public void setValue(DMServerIntegrationData data, Boolean value) {
        data.setWrapSystemOut(value);
      }
    }), //
    new BooleanConfigElement("log.wrapSysErr", new ConfigElementWrapper<>() {
      @Override
      public Boolean getValue(DMServerIntegrationData data) {
        return data.isWrapSystemErr();
      }

      @Override
      public void setValue(DMServerIntegrationData data, Boolean value) {
        data.setWrapSystemErr(value);
      }
    }), //
  };

  private static final Map<String, RepositoryItemCreator> ourRepositoryItemType2Creator;

  static {
    ourRepositoryItemType2Creator = new HashMap<>();
    ourRepositoryItemType2Creator.put(DMServerRepositoryWatchedItem.TYPE_PROPERTY_VALUE, new RepositoryItemCreator() {

      @Override
      public DMServerRepositoryItem20Base createRepositoryItem() {
        return new DMServerRepositoryWatchedItem();
      }
    });
    ourRepositoryItemType2Creator.put(DMServerRepositoryExternalItem.TYPE_PROPERTY_VALUE, new RepositoryItemCreator() {

      @Override
      public DMServerRepositoryItem20Base createRepositoryItem() {
        return new DMServerRepositoryExternalItem();
      }
    });
  }

  @NonNls
  private static final String ORDER_PROPERTY_NAME = "chain";

  private static final String ORDER_PROPERTY_VALUE_SPLITTER = ",";

  private final VirtualFile myKernelPropertiesFile;
  private final VirtualFile myMedicPropertiesFile;
  private final VirtualFile myRepositoryPropertiesFile;

  public DMServerConfigSupport2Base(@NonNls String kernelPropertiesPath,
                                    @NonNls String medicPropertiesPath,
                                    @NonNls String repositoryPropertiesPath,
                                    VirtualFile home) {
    myKernelPropertiesFile = home.findFileByRelativePath(kernelPropertiesPath);
    myMedicPropertiesFile = home.findFileByRelativePath(medicPropertiesPath);
    myRepositoryPropertiesFile = home.findFileByRelativePath(repositoryPropertiesPath);
  }

  @Override
  protected List<VirtualFile> getFiles() {
    return Arrays.asList(myKernelPropertiesFile, myMedicPropertiesFile, myRepositoryPropertiesFile);
  }

  @Override
  public void readFromServer(DMServerIntegrationData data) {

    try {
      myKernelPropertiesFile.refresh(false, false);
      Properties kernelProperties = PropertiesUtil.loadProperties(myKernelPropertiesFile);
      for (ConfigElement<?> element : ourKernelConfigElements) {
        element.load(kernelProperties, data);
      }

      myMedicPropertiesFile.refresh(false, false);
      Properties medicProperties = PropertiesUtil.loadProperties(myMedicPropertiesFile);
      for (ConfigElement<?> element : ourMedicConfigElements) {
        element.load(medicProperties, data);
      }

      myRepositoryPropertiesFile.refresh(false, false);
      Properties repositoryProperties = PropertiesUtil.loadProperties(myRepositoryPropertiesFile);
      readRepositoryProperties(repositoryProperties, data);
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void readRepositoryProperties(Properties repositoryProperties, DMServerIntegrationData data) {
    Map<String, DMServerRepositoryItem> name2repositoryItem = new HashMap<>();

    for (String propertyName : repositoryProperties.stringPropertyNames()) {
      List<String> propertyNameParts = StringUtil.split(propertyName, DMServerRepositoryItem20Base.PROPERTY_NAME_SPLITTER);
      if (propertyNameParts.size() != 2 || !DMServerRepositoryItem20Base.TYPE_PROPERTY_NAME.equals(propertyNameParts.get(1))) {
        continue;
      }
      String itemName = propertyNameParts.get(0);
      if ("".equals(itemName)) {
        continue;
      }

      String propertyValue = repositoryProperties.getProperty(propertyName);
      RepositoryItemCreator itemCreator = ourRepositoryItemType2Creator.get(propertyValue);
      if (itemCreator == null) {
        continue;
      }

      DMServerRepositoryItem20Base repositoryItem = itemCreator.createRepositoryItem();
      repositoryItem.setName(itemName);
      repositoryItem.load(repositoryProperties);
      name2repositoryItem.put(itemName, repositoryItem);
    }

    String orderProperty = repositoryProperties.getProperty(ORDER_PROPERTY_NAME);
    List<String> itemsOrder = orderProperty == null
                              ? Collections.emptyList()
                              : StringUtil.split(orderProperty, ORDER_PROPERTY_VALUE_SPLITTER);

    List<DMServerRepositoryItem> repositoryItems = new ArrayList<>();
    for (String itemName : itemsOrder) {
      DMServerRepositoryItem repositoryItem = name2repositoryItem.remove(itemName);
      if (repositoryItem == null) {
        continue;
      }
      repositoryItems.add(repositoryItem);
    }
    repositoryItems.addAll(name2repositoryItem.values());


    data.setRepositoryItems(repositoryItems);
  }

  @Override
  public void writeToServer(DMServerIntegrationData data) {
    try {
      Properties kernelProperties = PropertiesUtil.loadProperties(myKernelPropertiesFile);
      for (ConfigElement<?> element : ourKernelConfigElements) {
        element.save(kernelProperties, data);
      }
      PropertiesUtil.saveProperties(myKernelPropertiesFile, kernelProperties);

      Properties medicProperties = PropertiesUtil.loadProperties(myMedicPropertiesFile);
      for (ConfigElement<?> element : ourMedicConfigElements) {
        element.save(medicProperties, data);
      }
      PropertiesUtil.saveProperties(myMedicPropertiesFile, medicProperties);

      Properties repositoryProperties = new Properties();
      writeRepositoryProperties(repositoryProperties, data);
      PropertiesUtil.saveProperties(myRepositoryPropertiesFile, repositoryProperties);
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void writeRepositoryProperties(Properties repositoryProperties, DMServerIntegrationData data) {
    List<String> itemsOrder = new ArrayList<>();
    for (DMServerRepositoryItem repositoryItem : data.getRepositoryItems()) {
      if (!(repositoryItem instanceof DMServerRepositoryItem20Base)) {
        continue;
      }

      DMServerRepositoryItem20Base repositoryItem20 = (DMServerRepositoryItem20Base)repositoryItem;
      repositoryItem20.save(repositoryProperties);
      itemsOrder.add(repositoryItem20.getName());
    }

    repositoryProperties.setProperty(ORDER_PROPERTY_NAME, StringUtil.join(itemsOrder, ORDER_PROPERTY_VALUE_SPLITTER));
  }

  // TODO: ? may make data empty if load error

  private static abstract class ConfigElement<T> {
    private final ConfigElementWrapper<T> myElementWrapper;

    private final String myProperyName;

    ConfigElement(@NonNls String propertyName, ConfigElementWrapper<T> elementWrapper) {
      myProperyName = propertyName;
      myElementWrapper = elementWrapper;
    }

    private String getPropertyName() {
      return myProperyName;
    }

    public void load(Properties properties, DMServerIntegrationData data) {
      String propertyText = properties.getProperty(getPropertyName());
      if (propertyText != null) {
        setValue(data, toValue(propertyText));
      }
    }

    public void save(Properties properties, DMServerIntegrationData data) {
      properties.setProperty(getPropertyName(), toText(getValue(data)));
    }

    private String toText(T value) {
      return value.toString();
    }

    private void setValue(DMServerIntegrationData data, T value) {
      myElementWrapper.setValue(data, value);
    }

    private T getValue(DMServerIntegrationData data) {
      return myElementWrapper.getValue(data);
    }

    protected abstract T toValue(String propertyText);
  }

  private static class BooleanConfigElement extends ConfigElement<Boolean> {

    BooleanConfigElement(@NonNls String propertyName, ConfigElementWrapper<Boolean> booleanConfigElementWrapper) {
      super(propertyName, booleanConfigElementWrapper);
    }

    @Override
    protected Boolean toValue(String propertyText) {
      return Boolean.valueOf(propertyText);
    }
  }

  private static class IntegerConfigElement extends ConfigElement<Integer> {
    IntegerConfigElement(@NonNls String propertyName, ConfigElementWrapper<Integer> integerConfigElementWrapper) {
      super(propertyName, integerConfigElementWrapper);
    }

    @Override
    protected Integer toValue(String propertyText) {
      return Integer.valueOf(propertyText);
    }
  }

  private static class StringConfigElement extends ConfigElement<String> {
    StringConfigElement(@NonNls String propertyName, ConfigElementWrapper<String> stringConfigElementWrapper) {
      super(propertyName, stringConfigElementWrapper);
    }

    @Override
    protected String toValue(String propertyText) {
      return propertyText;
    }
  }

  private interface RepositoryItemCreator {

    DMServerRepositoryItem20Base createRepositoryItem();
  }
}
