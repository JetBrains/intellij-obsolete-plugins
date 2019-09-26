package org.jetbrains.plugins.ruby.motion.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.facet.Facet;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtilRt;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;

/**
 * @author Dennis.Ushakov
 */
public class RubyMotionUtil {
  public static final String RUBY_MOTION_IDENTIFIER_REGEX = "[a-z][A-Za-z\\d]*";
  public static final String RUBY_MOTION_CONSTANT_REGEX = "[A-Z][A-Za-z\\d]*";
  private static final NotNullLazyValue<RubyMotionUtil> NONE = new NotNullLazyValue<RubyMotionUtil>() {
    @NotNull
    @Override
    protected RubyMotionUtil compute() {
      return new RubyMotionUtil();
    }
  };

  protected static final Key<String> SDK_VERSION = Key.create("ruby.motion.sdk.version");
  protected static final Key<String[]> REQUIRED_FRAMEWORKS = Key.create("ruby.motion.required.frameworks");

  protected static final String[] DEFAULT_IOS_FRAMEWORKS = new String[]{"UIKit", "Foundation", "CoreGraphics"};
  protected static final String[] DEFAULT_OSX_FRAMEWORKS = new String[]{"AppKit", "Foundation", "CoreGraphics"};
  protected static final String[] DEFAULT_ANDROID_FRAMEWORKS = new String[]{"android"};
  protected static final String IOS_SDK_PREFIX = "iPhoneOS";
  protected static final String OSX_SDK_PREFIX = "MacOSX";
  protected static final String SDK_SUFFIX = ".sdk";
  protected static String DEFAULT_OSX_SDK_VERSION = null;
  protected static String DEFAULT_IOS_SDK_VERSION = null;

  protected static final String RUBY_MOTION_PATH = "/Library/RubyMotion";
  public static final String RUBY_MOTION_LIBRARY = "RubyMotion Library";

  public static RubyMotionUtil getInstance() {
    RubyMotionUtil service = ServiceManager.getService(RubyMotionUtil.class);
    return service != null ? service : NONE.getValue();
  }

  public XDebugSession createMotionDebugSession(final RunProfileState state,
                                                final ExecutionEnvironment env,
                                                final ProcessHandler serverProcessHandler) throws ExecutionException {
    throw new ExecutionException("No RubyMotion support found");
  }

  @Contract("null -> false")
  public boolean isRubyMotionModule(@Nullable final Module module) {
    return false;
  }

  public boolean hasMacRubySupport(@Nullable PsiElement element) {
    return false;
  }

  public boolean hasRubyMotionSupport(@Nullable Module module) {
    return false;
  }

  public String getSdkVersion(final Module module) {
    return "unknown";
  }

  public boolean isOSX(@NotNull final Module module) {
    return false;
  }

  public boolean isAndroid(@NotNull final Module module) {
    return false;
  }

  public String[] getRequiredFrameworks(final Module module) {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  public void resetSdkAndFrameworks(Module module) {
  }

  public boolean isIgnoredFrameworkName(String name) {
    return false;
  }

  @TestOnly
  protected Pair<String, String[]> calculateSdkAndFrameworks(PsiFile file) {
    return Pair.empty();
  }

  public String getMainRakeTask(@NotNull final Module module) {
    return "";
  }

  public String getRubyMotionPath() {
    return "";
  }

  public boolean rubyMotionPresent() {
    return false;
  }

  @Deprecated
  @Nullable
  public Module getModuleWithMotionSupport(final @NotNull Project project) {
    return null;
  }

  @Nullable
  public Facet getRubyMotionFacet(Module module) {
    return null;
  }

  public Symbol getMotionSuperclass(Symbol symbol, PsiElement invocationPoint) {
    return null;
  }

  public boolean isMotionSymbol(Symbol symbol) {
    return false;
  }

  public String getMotionDoc(PsiElement element, Symbol symbol) {
    return null;
  }
}
