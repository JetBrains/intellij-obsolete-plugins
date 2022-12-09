package com.intellij.dmserver.libraries.obr.data;

public class LibraryDownloadData extends AbstractCodeDownloadData<LibraryData> {

  public LibraryDownloadData(String name, String version, String link) {
    super(new LibraryData(name, version, link));
  }
}
