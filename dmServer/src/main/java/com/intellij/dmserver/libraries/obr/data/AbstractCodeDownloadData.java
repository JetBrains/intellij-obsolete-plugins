package com.intellij.dmserver.libraries.obr.data;

public abstract class AbstractCodeDownloadData<D extends AbstractCodeData> {

  private final D myCodeData;

  private Boolean myDownload = false;

  public AbstractCodeDownloadData(D codeData) {
    myCodeData = codeData;
  }

  public D getCodeData() {
    return myCodeData;
  }

  public Boolean getDownload() {
    return myDownload;
  }

  public void setDownload(Boolean download) {
    myDownload = download;
  }
}
