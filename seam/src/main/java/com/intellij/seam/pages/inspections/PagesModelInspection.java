package com.intellij.seam.pages.inspections;

import com.intellij.seam.pages.xml.pages.Pages;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;

public class PagesModelInspection extends BasicDomElementsInspection<Pages> {

  public PagesModelInspection() {
    super(Pages.class);
  }
}

