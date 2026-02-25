// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.lang.Commenter;

public final class GspCommenter implements Commenter {

  @Override
  public String getLineCommentPrefix() {
    return null;
  }

  @Override
  public String getBlockCommentPrefix() {
    return "%{--";
  }

  @Override
  public String getBlockCommentSuffix() {
    return "--}%";
  }

  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }
}
