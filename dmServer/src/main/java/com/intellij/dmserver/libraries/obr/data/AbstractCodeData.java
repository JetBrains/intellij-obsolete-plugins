package com.intellij.dmserver.libraries.obr.data;

import com.intellij.dmserver.libraries.ProgressListener;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public abstract class AbstractCodeData<T extends CodeDataDetails> {
  private final String myName;

  private final String myVersion;

  private final String myLink;

  private T myDetails;

  public AbstractCodeData(String name, String version, String link) {
    myName = name;
    myVersion = version;
    myLink = link;
  }

  public String getName() {
    return myName;
  }

  public String getVersion() {
    return myVersion;
  }

  public String getLink() {
    return myLink;
  }

  public void loadDetails(ProgressListener progressListener) throws IOException, XPathExpressionException {
    if (myDetails != null) {
      return;
    }
    myDetails = doLoadDetails(progressListener);
  }

  public T getDetails() {
    return myDetails;
  }

  public String getID() {
    return getName() + "###" + getVersion();
  }

  protected abstract T doLoadDetails(ProgressListener progressListener) throws IOException, XPathExpressionException;

}
