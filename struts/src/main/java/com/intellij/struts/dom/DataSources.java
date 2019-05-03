/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Presentation(icon = "StrutsApiIcons.DataSource")
public interface DataSources extends StrutsRootElement {

  @NotNull
  List<DataSource> getDataSources();

  DataSource addDataSource();
}
