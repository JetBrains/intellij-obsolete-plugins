package org.jetbrains.plugins.ruby.motion;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class RubyMotionBundle extends AbstractBundle {
    private static final RubyMotionBundle INSTANCE = new RubyMotionBundle();
    public static final String PATH_TO_BUNDLE = "messages.RubyMotionBundle";

    public RubyMotionBundle() {
        super(PATH_TO_BUNDLE);
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
