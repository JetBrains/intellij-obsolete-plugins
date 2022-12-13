package com.intellij.dmserver.libraries.obr.data;

import java.util.List;

public interface CodeDataDetails<D extends BundleData> {

  List<D> getDependencies();
}
