package com.intellij.play.language.support;

import com.intellij.lang.Commenter;

public class PlayCommenter implements Commenter {
  @Override
  public String getLineCommentPrefix() {
    return null;
  }

  @Override
  public String getBlockCommentPrefix() {
    return "*{";
  }

  @Override
  public String getBlockCommentSuffix() {
    return "}*";
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


