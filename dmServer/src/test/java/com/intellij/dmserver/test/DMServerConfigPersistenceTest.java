package com.intellij.dmserver.test;

import com.intellij.dmserver.integration.*;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class DMServerConfigPersistenceTest extends DMTestBase {

  public void testRepository20Persistence() {
    String WATCHED_NAME = "usr";
    String WATCHED_PATH = "watched/path";
    String WATCHED_INTERVAL = "10";
    String EXTERNAL_NAME = "ext";
    String EXTERNAL_PATH = "external/{path}/*.jar";

    DMServerIntegrationData sourceData = new DMServerIntegrationData("c:\\home");

    List<DMServerRepositoryItem> sourceRepositoryItems = new ArrayList<>();

    DMServerRepositoryWatchedItem watchedItem = new DMServerRepositoryWatchedItem();
    watchedItem.setName(WATCHED_NAME);
    watchedItem.setPath(WATCHED_PATH);
    watchedItem.setWatchedInterval(WATCHED_INTERVAL);
    sourceRepositoryItems.add(watchedItem);

    DMServerRepositoryExternalItem externalItem = new DMServerRepositoryExternalItem();
    externalItem.setName(EXTERNAL_NAME);
    externalItem.setPath(EXTERNAL_PATH);
    sourceRepositoryItems.add(externalItem);

    sourceData.setRepositoryItems(sourceRepositoryItems);

    Element element = new Element("mock");
    sourceData.writeExternal(element);

    DMServerIntegrationData resultData = new DMServerIntegrationData("c:\\home");
    resultData.readExternal(element);

    List<DMServerRepositoryItem> resultRepositoryItems = resultData.getRepositoryItems();
    assertEquals(2, resultRepositoryItems.size());

    DMServerRepositoryWatchedItem resultWatchedItem = null;
    DMServerRepositoryExternalItem resultExternalItem = null;
    for (DMServerRepositoryItem repositoryItem : resultRepositoryItems) {
      if (repositoryItem instanceof DMServerRepositoryWatchedItem) {
        resultWatchedItem = (DMServerRepositoryWatchedItem)repositoryItem;
      }
      else if (repositoryItem instanceof DMServerRepositoryExternalItem) {
        resultExternalItem = (DMServerRepositoryExternalItem)repositoryItem;
      }
    }
    assertNotNull(resultWatchedItem);
    assertNotNull(resultExternalItem);
    assertEquals(WATCHED_NAME, resultWatchedItem.getName());
    assertEquals(WATCHED_PATH, resultWatchedItem.getPath());
    assertEquals(WATCHED_INTERVAL, resultWatchedItem.getWatchedInterval());
    assertEquals(EXTERNAL_NAME, resultExternalItem.getName());
    assertEquals(EXTERNAL_PATH, resultExternalItem.getPath());
  }

  public void testRepository10Persistence() {
    String ITEM_PATH = "watched/path";

    DMServerIntegrationData sourceData = new DMServerIntegrationData("c:\\home");

    List<DMServerRepositoryItem> sourceRepositoryItems = new ArrayList<>();

    DMServerRepositoryItem10 sourceItem = new DMServerRepositoryItem10();
    sourceItem.setPath(ITEM_PATH);
    sourceRepositoryItems.add(sourceItem);

    sourceData.setRepositoryItems(sourceRepositoryItems);

    Element element = new Element("mock");
    sourceData.writeExternal(element);

    DMServerIntegrationData resultData = new DMServerIntegrationData("c:\\home");
    resultData.readExternal(element);

    List<DMServerRepositoryItem> resultRepositoryItems = resultData.getRepositoryItems();
    assertEquals(1, resultRepositoryItems.size());

    DMServerRepositoryItem10 resultItem = assertInstanceOf(resultRepositoryItems.get(0), DMServerRepositoryItem10.class);
    assertEquals(ITEM_PATH, resultItem.getPath());
  }
}
