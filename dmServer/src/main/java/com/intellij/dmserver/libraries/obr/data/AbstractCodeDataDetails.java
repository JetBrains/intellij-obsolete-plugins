package com.intellij.dmserver.libraries.obr.data;

import com.intellij.dmserver.libraries.ProgressListener;
import com.intellij.dmserver.libraries.obr.DownloadBundlesEditor;
import com.intellij.dmserver.libraries.obr.HttpRetriever;
import com.intellij.dmserver.libraries.obr.XPathUtils;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCodeDataDetails<D extends BundleData> implements CodeDataDetails<D> {
  private final ArrayList<D> myDependencies;

  public AbstractCodeDataDetails(String link, ProgressListener progressListener) throws IOException, XPathExpressionException {
    Node root = XPathUtils.getInstance().createHtmlRoot(HttpRetriever.retrievePage(link, progressListener));

    myDependencies = new ArrayList<>();
    NodeList bundleNodeList = scrapTableRows(root, "dependencies");
    for (int iBundle = 0; iBundle < bundleNodeList.getLength(); iBundle++) {
      Node bundleNode = bundleNodeList.item(iBundle);
      myDependencies.add(createDependency(evaluateXpath("td[2]/a/text()", bundleNode).trim(), //
                                      evaluateXpath("td[3]/text()", bundleNode).trim(), //
                                      resolveUrl(evaluateXpath("td[2]/a/@href", bundleNode).trim())));
    }

    retrieveCustomDetails(root);
  }

  protected String evaluateXpath(@NonNls String xPath, Object item) throws XPathExpressionException {
    return getXPath().evaluate(xPath, item);
  }

  protected static Object evaluateXpath(@NonNls String xPath, Object item, QName returnType) throws XPathExpressionException {
    return getXPath().evaluate(xPath, item);
  }

  protected static NodeList scrapTableRows(Node root, @NonNls String tableDivId) throws XPathExpressionException {
    @NonNls String xPath = "//div[@id='" + tableDivId + "']//table//tr[not(th)]";
    return (NodeList)getXPath().evaluate(xPath, root, XPathConstants.NODESET);
  }

  protected static XPath getXPath() {
    return XPathUtils.getInstance().getXPath();
  }

  @Override
  public List<D> getDependencies() {
    return myDependencies;
  }

  private static String resolveUrl(String suffix) {
    return StringUtil.isEmpty(suffix) ? null : DownloadBundlesEditor.URL_BASE + suffix;
  }

  protected static String scrapNamedLink(Node root, @NonNls String linkName) throws XPathExpressionException {
    return resolveUrl((String)evaluateXpath("//a[text()='" + linkName + "']/@href", root, XPathConstants.STRING));
  }

  protected abstract D createDependency(String name, String version, String link);

  protected abstract void retrieveCustomDetails(Node root) throws XPathExpressionException;
}
