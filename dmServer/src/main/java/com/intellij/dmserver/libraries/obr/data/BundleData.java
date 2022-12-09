package com.intellij.dmserver.libraries.obr.data;

import com.intellij.dmserver.libraries.BundleDefinition;
import com.intellij.dmserver.libraries.ProgressListener;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public class BundleData extends AbstractCodeData<BundleDetails> {

  public BundleData(BundleDefinition bundleDef) {
    this(bundleDef.getSymbolicName(), bundleDef.getVersion(), null);
  }

  public BundleData(String name, String version, String link) {
    super(name, version, link);
  }

  @Override
  protected BundleDetails doLoadDetails(ProgressListener progressListener) throws IOException, XPathExpressionException {
    return new BundleDetails(getLink(), progressListener);
  }
}
