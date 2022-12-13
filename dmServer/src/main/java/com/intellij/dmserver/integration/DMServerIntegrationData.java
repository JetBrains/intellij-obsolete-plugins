package com.intellij.dmserver.integration;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.DMServerInstallationManager;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DMServerIntegrationData implements ApplicationServerPersistentData {

  @NonNls
  private static final String STATE_ELEMENT = "integrationState";

  private DMServerIntegrationState myState = new DMServerIntegrationState();

  public DMServerIntegrationData(@NotNull String installationHome) {
    setInstallationHome(installationHome);
  }

  @NotNull
  public String getInstallationHome() {
    return myState.getInstallationHome();
  }

  @Nullable
  public DMServerInstallation getInstallation() {
    return DMServerInstallationManager.getInstance().findInstallation(myState.getInstallationHome());
  }

  public void setInstallationHome(@NotNull String installationHome) {
    myState.setInstallationHome(installationHome);
  }

  public int getShellPort() {
    return myState.getShellPort();
  }

  public void setShellPort(int shellPort) {
    myState.setShellPort(shellPort);
  }

  public boolean isShellEnabled() {
    return myState.isShellEnabled();
  }

  public void setShellEnabled(boolean shellEnabled) {
    myState.setShellEnabled(shellEnabled);
  }

  public int getDeploymentTimeoutSecs() {
    return myState.getDeploymentTimeoutSecs();
  }

  public void setDeploymentTimeoutSecs(int deploymentTimeoutSecs) {
    myState.setDeploymentTimeoutSecs(deploymentTimeoutSecs);
  }

  public String getPickupFolder() {
    return myState.getPickupFolder();
  }

  public void setPickupFolder(String pickupFolder) {
    myState.setPickupFolder(pickupFolder);
  }

  public String getDumpsFolder() {
    return myState.getDumpsFolder();
  }

  public void setDumpsFolder(String dumpsFolder) {
    myState.setDumpsFolder(dumpsFolder);
  }

  public boolean isWrapSystemErr() {
    return myState.isWrapSystemErr();
  }

  public void setWrapSystemErr(boolean wrapSystemErr) {
    myState.setWrapSystemErr(wrapSystemErr);
  }

  public boolean isWrapSystemOut() {
    return myState.isWrapSystemOut();
  }

  public void setWrapSystemOut(boolean wrapSystemOut) {
    myState.setWrapSystemOut(wrapSystemOut);
  }

  public List<DMServerRepositoryItem> getRepositoryItems() {
    return myState.getRepositoryItems();
  }

  public void setRepositoryItems(List<DMServerRepositoryItem> repositoryItems) {
    myState.setRepositoryItems(new ArrayList<>(repositoryItems));
  }

  public boolean isReloadRequired() {
    return myState.isReloadRequired();
  }

  public void setReloadRequired(boolean reloadRequired) {
    myState.setReloadRequired(reloadRequired);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    Element content = element.getChild(STATE_ELEMENT);
    if (content == null) {
      DMServerIntegrationDataNoRepository noRepositoryData = new DMServerIntegrationDataNoRepository();
      noRepositoryData.readExternal(element);

      myState = new DMServerIntegrationState();

      myState.setInstallationHome(noRepositoryData.getInstallationHome());

      myState.setShellPort(noRepositoryData.getShellPort());
      myState.setShellEnabled(noRepositoryData.isShellEnabled());
      myState.setDeploymentTimeoutSecs(noRepositoryData.getDeploymentTimeoutSecs());
      myState.setDumpsFolder(noRepositoryData.getDumpsFolder());
      myState.setPickupFolder(noRepositoryData.getPickupFolder());
      myState.setWrapSystemErr(noRepositoryData.isWrapSystemErr());
      myState.setWrapSystemOut(noRepositoryData.isWrapSystemOut());

      myState.setReloadRequired(true);
    }
    else {
      myState = XmlSerializer.deserialize(content, DMServerIntegrationState.class);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    element.addContent(XmlSerializer.serialize(myState, new SkipDefaultValuesSerializationFilters()));
  }

  @Tag(STATE_ELEMENT)
  public static class DMServerIntegrationState extends DMServerIntegrationStateBase {

    private boolean myReloadRequired = false;

    private List<DMServerRepositoryItem> myRepositoryItems = new ArrayList<>();

    @Property(surroundWithTag = false)
    @XCollection(elementTypes = {DMServerRepositoryExternalItem.class,
      DMServerRepositoryWatchedItem.class,
      DMServerRepositoryItem10.class})
    public List<DMServerRepositoryItem> getRepositoryItems() {
      return myRepositoryItems;
    }

    public void setRepositoryItems(List<DMServerRepositoryItem> repositoryItems) {
      myRepositoryItems = repositoryItems;
    }

    public boolean isReloadRequired() {
      return myReloadRequired;
    }

    public void setReloadRequired(boolean reloadRequired) {
      myReloadRequired = reloadRequired;
    }
  }
}
