package org.jetbrains.plugins.ruby.chef.sdk;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.NlsContexts.NotificationContent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefBundle;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.chef.sourceRoot.ChefTopics;
import org.jetbrains.plugins.ruby.chef.sourceRoot.CookbookUrlsCache;
import org.jetbrains.plugins.ruby.gem.util.BundlerUtil;
import org.jetbrains.plugins.ruby.remote.RubyRemoteInterpreterManager;
import org.jetbrains.plugins.ruby.ruby.RModuleUtil;
import org.jetbrains.plugins.ruby.ruby.RubyFileSystemUtil;
import org.jetbrains.plugins.ruby.ruby.sdk.RubySdkType;
import org.jetbrains.plugins.ruby.ruby.sdk.RubySdkUtil;
import org.jetbrains.plugins.ruby.support.UIUtil;
import org.jetbrains.plugins.ruby.utils.RubyPluginUtil;
import org.jetbrains.plugins.ruby.utils.VirtualFileUtil;
import org.jetbrains.plugins.ruby.version.management.AbstractSdkRefresher;
import org.jetbrains.plugins.ruby.version.management.RubyVersionManagerSdkData;
import org.jetbrains.plugins.ruby.version.management.system.RubySystemVersionManagerHandler;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

final class ChefSdkRefresher extends AbstractSdkRefresher {
  private static final Logger LOG = Logger.getInstance(ChefSdkRefresher.class);

  private static final String OPT_CHEF_DK = "/opt/chefdk";
  private static final String OPT_CHEF = "/opt/chef";

  private static final String WIN_OPSCODE = "c:\\opscode";
  private static final String WIN_CHEF = WIN_OPSCODE + "\\chef";
  private static final String WIN_CHEF_DK = WIN_OPSCODE + "\\chefdk";

  private static final String EMBEDDED = "embedded";
  private static final String GEMS_PATH = EMBEDDED + "/lib/ruby/gems/";
  private static final String FILE_EXTENSION = SystemInfo.isWindows ? ".exe" : "";
  private static final String EMBEDDED_BIN_RUBY = EMBEDDED + "/bin/ruby" + FILE_EXTENSION;

  private static final String CHEF_DK_NOTIFICATION_GROUP = "Chef DK";
  private static final String GEMS_DIRECTORY = BundlerUtil.GEMS;

  private static final ExecutorService ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
    "ChefDK Refresher Executor", 1
  );

  @Override
  public boolean isManagedSdk(@Nullable Sdk sdk) {
    return false;
  }

  @Override
  protected void doRefreshSDKs() {
    refreshChefSdks();
  }

  @Override
  public @Nullable String suggestSdkName(@NotNull String executablePath,
                                         @NotNull String distributionId,
                                         @NotNull RubyVersionManagerSdkData versionManagerSdkAdditionalData) {
    String chefSdkHomePath = getChefSdkHomePath(executablePath);
    return chefSdkHomePath == null ? null : ChefBundle.message("chef.sdk.name", chefSdkHomePath);
  }

  @Override
  protected void doInitialize() {
    final VirtualFile chefSdkHome = getPreferableChefSdkRoot();
    if (chefSdkHome == null) return;

    VirtualFile gemsFolder = getGemsFolder(chefSdkHome);
    if (gemsFolder == null) return;

    RubyFileSystemUtil.addRootToWatch(gemsFolder.getPath(), false, RubyPluginUtil.getUnloadAwareDisposable());
  }

  private static @Nullable VirtualFile getGemsFolder(final @NotNull VirtualFile chefDK) {
    final VirtualFile pathToGems = chefDK.findFileByRelativePath(GEMS_PATH);
    if (pathToGems == null) {
      LOG.warn("Gems folder not found.");
      return null;
    }

    final VirtualFile[] children = pathToGems.getChildren();
    return children.length > 0 ? children[0].findFileByRelativePath(GEMS_DIRECTORY) : null;
  }

  @Override
  public void onProjectOpened(@NotNull Project project) {
    suggestChefDK(project);
    project.getMessageBus().simpleConnect().subscribe(ChefTopics.COOKBOOK, new MyCookbooksListener(project));
  }

  public static void suggestChefDK(final @NotNull Project project) {
    final Module[] modulesWithRuby = RModuleUtil.getInstance().getAllModulesWithRubySupport(project);

    for (Module module : modulesWithRuby) {
      ReadAction.nonBlocking(
        () -> buildSuggestChefDKNotificationIfNeeded(project, module)
      ).expireWhen(
        module::isDisposed
      ).finishOnUiThread(ModalityState.defaultModalityState(), notification -> {
        if (notification == null) {
          return;
        }
        notification.notify(project);
      }).coalesceBy(module, ChefSdkRefresher.class).submit(ourExecutor);
    }
  }

  @RequiresReadLock
  private static @Nullable Notification buildSuggestChefDKNotificationIfNeeded(final @NotNull Project project,
                                                                               final @NotNull Module module) {
    final Sdk foundRubySdkForModule = RModuleUtil.getInstance().findRubySdkForModule(module);
    final Sdk preferableChefDK = getPreferableChefSdk();

    if (foundRubySdkForModule == null || preferableChefDK == null || preferableChefDK == foundRubySdkForModule) {
      return null;
    }

    final List<String> cookbooksUrls = CookbookUrlsCache.Companion.getInstance(project).getCachedURLs(module);
    if (cookbooksUrls.isEmpty()) {
      return null;
    }

    return buildShowNotification(module, preferableChefDK, cookbooksUrls);
  }

  private static @NotNull Notification buildShowNotification(final @NotNull Module module,
                                                             final @NotNull Sdk sdk,
                                                             final @NotNull List<String> cookbookUrls) {
    final Notification notification = new Notification(
      CHEF_DK_NOTIFICATION_GROUP,
      ChefBundle.message("chef.dk.project.notifications.title"),
      createNotificationContent(sdk.getName(), cookbookUrls),
      NotificationType.INFORMATION
    );

    notification.setSuggestionType(true);

    final AnAction changeModuleSdkAction = createChangeModuleSdkAction(module, sdk);
    notification.addAction(changeModuleSdkAction);

    final AnAction openRubyMineSettingsAction = createOpenRubyModuleSettingsAction(module);
    notification.addAction(openRubyMineSettingsAction);

    return notification;
  }

  private static @NotNull AnAction createChangeModuleSdkAction(final @NotNull Module module, final @NotNull Sdk sdk) {
    return new DumbAwareAction(ChefBundle.message("action.sdk.switch.text")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (!module.isDisposed()) {
          RModuleUtil.getInstance().changeModuleSdk(sdk, module);
        }
      }
    };
  }

  private static @NotNull AnAction createOpenRubyModuleSettingsAction(final @NotNull Module module) {
    return new DumbAwareAction(ChefBundle.message("action.sdk.configure.text")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> UIUtil.openRubyModuleSettings(module));
      }
    };
  }

  private static @NotNull CharSequence createCookbooksText(@NotNull List<String> cookbookUrls) {
    StringBuilder cookbooksText = new StringBuilder();
    for (String url : cookbookUrls) {
      if (!cookbooksText.isEmpty()) {
        cookbooksText.append(", ");
      }
      cookbooksText.append(ChefUtil.getCookbookNameByUrl(url));
    }
    return cookbooksText;
  }

  private static @NotNull @NotificationContent String createNotificationContent(final @NotNull String sdkName,
                                                                                final @NotNull List<String> cookbookUrls) {
    return ChefBundle.message("notification.content.sdk.with.cookbooks", sdkName, createCookbooksText(cookbookUrls));
  }

  public void refreshChefSdks() {
    final Sdk chefSdk = getPreferableChefSdk();
    if (chefSdk != null) return;

    final VirtualFile chefSdkDirectory = getChefDKRubyInterpreter();
    if (chefSdkDirectory == null) {
      dropChefSdks();
      return;
    }

    RubySdkType.getInstance().createAndAddLocalSdk(chefSdkDirectory, RubySystemVersionManagerHandler.getInstance().createAdditionalData());
  }

  /**
   * Deletes all chef sdks from jdkTable
   *
   * @implSpec current implementation relies on SDK home path
   */
  private static void dropChefSdks() {
    ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
    List<Path> possibleRoots = Arrays.asList(
      Paths.get(OPT_CHEF),
      Paths.get(OPT_CHEF_DK),
      Paths.get(WIN_OPSCODE));

    Stream.of(jdkTable.getAllJdks())
      .filter(it -> {
        if (!RubySdkUtil.isRubySDK(it) || RubyRemoteInterpreterManager.isRemoteSdk(it)) {
          return false;
        }
        String homePathName = it.getHomePath();
        if (homePathName == null) {
          return false;
        }
        Path homePath = Paths.get(homePathName);
        return ContainerUtil.exists(possibleRoots, root -> homePath.startsWith(root));
      })
      .forEach(it -> WriteAction.runAndWait(() -> jdkTable.removeJdk(it)));
  }

  private static @Nullable VirtualFile getChefDKRubyInterpreter() {
    final VirtualFile chefHome = getPreferableChefSdkRoot();
    if (chefHome == null) return null;
    return chefHome.findFileByRelativePath(EMBEDDED_BIN_RUBY);
  }

  private static @Nullable Sdk getPreferableChefSdk() {
    final VirtualFile chefSdkRoot = getPreferableChefSdkRoot();
    if (chefSdkRoot == null) return null;

    return findChefSdk(getChefDKRubyInterpreter());
  }

  private static @Nullable Sdk findChefSdk(VirtualFile sdkRoot) {
    final List<Sdk> sdks = getChefSdks();

    for (Sdk sdk : sdks) {
      final VirtualFile homeDirectory = sdk.getHomeDirectory();
      if (homeDirectory == null) continue;

      if (VirtualFileUtil.compareVirtualFiles(homeDirectory, sdkRoot) == 0) return sdk;
    }
    return null;
  }

  private static @NotNull List<Sdk> getChefSdks() {
    final Sdk[] sdks = ProjectJdkTable.getInstance().getAllJdks();

    return ContainerUtil.filter(sdks, sdk -> isChefSdk(sdk) && !RubyRemoteInterpreterManager.isRemoteSdk(sdk));
  }

  private static @Nullable VirtualFile getPreferableChefSdkRoot() {
    String chefDKUrl = null;
    if (SystemInfo.isWindows) {
      if ((new File(WIN_CHEF_DK)).exists()) {
        chefDKUrl = WIN_CHEF_DK;
      }
      if ((new File(WIN_CHEF)).exists()) {
        chefDKUrl = WIN_CHEF;
      }
    }
    else {
      if ((new File(OPT_CHEF_DK)).exists()) {
        chefDKUrl = OPT_CHEF_DK;
      }
      if ((new File(OPT_CHEF)).exists()) {
        chefDKUrl = OPT_CHEF;
      }
    }

    return chefDKUrl == null ? null : VirtualFileManager.getInstance().findFileByUrl("file:///" + chefDKUrl);
  }

  private static boolean isChefSdk(final @NotNull Sdk sdk) {
    String sdkHomePath = sdk.getHomePath();
    return getChefSdkHomePath(sdkHomePath) != null && new File(sdkHomePath).exists();
  }

  @Contract("null->null")
  private static @Nullable String getChefSdkHomePath(final @Nullable String executablePath) {
    if (executablePath == null) {
      return null;
    }
    int embeddedIndex = executablePath.indexOf(EMBEDDED_BIN_RUBY);
    return embeddedIndex < 1 ? null : executablePath.substring(0, embeddedIndex - 1);
  }

  private static class MyCookbooksListener implements CookbooksListener {
    private final Project myProject;

    MyCookbooksListener(Project project) {
      myProject = project;
    }

    @Override
    public void cookbookAdded() {
      suggestChefDK(myProject);
    }
  }

}
