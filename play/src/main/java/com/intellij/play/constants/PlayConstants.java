package com.intellij.play.constants;

import org.jetbrains.annotations.NonNls;

public interface PlayConstants {
  @NonNls String CONTROLLER_CLASS = "play.mvc.Controller";
  @NonNls String ROUTER_CLASS = "play.mvc.Router";
  @NonNls String RENDER_ARG_SCOPE_CLASS = "play.mvc.Scope.RenderArgs";
  @NonNls String FLASH_SCOPE_CLASS = "play.mvc.Scope.Flash";

  @NonNls String SCOPE_FLASH = "play.mvc.Scope.Flash";
  @NonNls String FAST_TAGS = "play.templates.FastTags";
  @NonNls String FAST_TAGS_NAMESPACE = "play.templates.FastTags.Namespace";
}
