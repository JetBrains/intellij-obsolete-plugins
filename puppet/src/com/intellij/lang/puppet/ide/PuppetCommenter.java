package com.intellij.lang.puppet.ide;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Anna Bulenkova
 */
public class PuppetCommenter implements Commenter {
  @Override
  public @Nullable String getLineCommentPrefix() {
    return "# ";
  }

  @Override
  public @Nullable String getBlockCommentPrefix() {
    return "/*";
  }

  @Override
  public @Nullable String getBlockCommentSuffix() {
    return "*/";
  }

  @Override
  public @Nullable String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public @Nullable String getCommentedBlockCommentSuffix() {
    return null;
  }
}
