package com.intellij.dmserver.libraries.obr.data;

import com.intellij.dmserver.libraries.ProgressListener;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public class LibraryData extends AbstractCodeData<LibraryDetails> {

  public LibraryData(String name, String version, String link) {
    super(name, version, link);
  }

  @Override
  protected LibraryDetails doLoadDetails(ProgressListener progressListener) throws IOException, XPathExpressionException {
    return new LibraryDetails(getLink(), progressListener);
  }
}
