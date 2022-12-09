package com.intellij.dmserver.libraries.obr.data;

import com.intellij.dmserver.libraries.ProgressListener;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LibraryDetails extends AbstractCodeDataDetails<LibraryBundleData> {

  private String myLibraryDefLink;

  public LibraryDetails(String link, ProgressListener progressListener) throws IOException, XPathExpressionException {
    super(link, progressListener);
  }

  @Override
  protected LibraryBundleData createDependency(String name, String version, String link) {
    return new LibraryBundleData(name, version, link);
  }

  @Override
  protected void retrieveCustomDetails(Node root) throws XPathExpressionException {
    myLibraryDefLink = scrapNamedLink(root, "Library Definition");

    Map<String, LibraryBundleData> dependencyName2Bundle = new HashMap<>();
    for (LibraryBundleData dependency : getDependencies()) {
      dependencyName2Bundle.put(dependency.getName(), dependency);
    }

    NodeList includedNodeList = scrapTableRows(root, "bundles");
    for (int iIncluded = 0; iIncluded < includedNodeList.getLength(); iIncluded++) {
      Node includedNode = includedNodeList.item(iIncluded);
      String includedName = evaluateXpath("td[1]/a/text()", includedNode).trim();
      LibraryBundleData includedDependency = dependencyName2Bundle.get(includedName);
      if (includedDependency == null) {
        // TODO: should never happen, indicates error on page
        continue;
      }
      includedDependency.setIncluded(true);
    }
  }

  public String getLibraryDefLink() {
    return myLibraryDefLink;
  }
}
