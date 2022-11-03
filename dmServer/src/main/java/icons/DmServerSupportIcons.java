package icons;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class DmServerSupportIcons {
  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, DmServerSupportIcons.class);
  }
  /** 16x16 */ public static final @NotNull Icon Bundle = load("icons/bundle.gif");
  /** 16x16 */ public static final @NotNull Icon DM = load("icons/dm.png");
  /** 13x13 */ public static final @NotNull Icon DmToolWindow = load("icons/dmToolWindow.png");
  /** 16x16 */ public static final @NotNull Icon ParBundle = load("icons/par-bundle.gif");
}
