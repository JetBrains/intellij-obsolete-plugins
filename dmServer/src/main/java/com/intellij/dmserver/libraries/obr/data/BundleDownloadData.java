package com.intellij.dmserver.libraries.obr.data;

public class BundleDownloadData extends AbstractCodeDownloadData<BundleData> {

  public BundleDownloadData(String name, String version, String link) {
    super(new BundleData(name, version, link));
  }
}