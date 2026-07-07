package org.jetbrains.plugins.ruby.chef.sourceRoot;

import com.intellij.util.messages.Topic;
import org.jetbrains.plugins.ruby.chef.sdk.CookbooksListener;

public final class ChefTopics {
  public static final Topic<CookbooksListener> COOKBOOK =
    new Topic<>("Cookbooks changed", CookbooksListener.class);

  private ChefTopics() {
  }
}
