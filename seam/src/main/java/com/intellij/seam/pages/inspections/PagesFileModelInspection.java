package com.intellij.seam.pages.inspections;

import com.intellij.seam.pages.xml.pages.Page;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;

public class PagesFileModelInspection extends BasicDomElementsInspection<Page> {
  public PagesFileModelInspection() {
    super(Page.class);
  }
}
