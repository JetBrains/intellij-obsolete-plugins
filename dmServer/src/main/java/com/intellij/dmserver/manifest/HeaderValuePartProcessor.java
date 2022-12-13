package com.intellij.dmserver.manifest;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;

public interface HeaderValuePartProcessor<C> {

  @NonNls
  String getHeaderName();

  void process(HeaderValuePart headerValue, C context);
}
