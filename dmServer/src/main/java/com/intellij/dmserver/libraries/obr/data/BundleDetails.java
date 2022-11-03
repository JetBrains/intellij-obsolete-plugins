package com.intellij.dmserver.libraries.obr.data;

import com.intellij.dmserver.libraries.ProgressListener;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public class BundleDetails extends AbstractCodeDataDetails<BundleData> {

  private String myBinaryJarLink;

  private String mySourceJarLink;

  public BundleDetails(String link, ProgressListener progressListener) throws IOException, XPathExpressionException {
    super(link, progressListener);
  }

  @Override
  protected BundleData createDependency(String name, String version, String link) {
    return new BundleData(name, version, link);
  }

  @Override
  protected void retrieveCustomDetails(Node root) throws XPathExpressionException {
    myBinaryJarLink = scrapNamedLink(root, "Binary Jar");
    mySourceJarLink = scrapNamedLink(root, "Source Jar");
  }

  public String getBinaryJarLink() {
    return myBinaryJarLink;
  }

  public String getSourceJarLink() {
    return mySourceJarLink;
  }
}
