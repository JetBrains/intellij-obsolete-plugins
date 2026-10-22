package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.guice.GuiceTestBase;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks the update pipeline of {@link GuiceProjectModel}: change detection of the index,
 * the activation of the listeners, and the background processing of dirty files.
 */
public class GuiceModelUpdateTest extends GuiceTestBase {

  private static final String CLIENT_WITH_ONE_INJECT = """
    import com.google.inject.Inject;
    public class Client {
      @Inject
      MyService service;
    }
    """;

  private GuiceProjectModel model() {
    return GuiceProjectModel.getInstance(getProject());
  }

  private @NotNull PsiFile configureClientAndModule() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    return myFixture.configureByText("Client.java", CLIENT_WITH_ONE_INJECT);
  }

  private void replaceClientText(@NotNull String newText) {
    Document document = myFixture.getEditor().getDocument();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      document.setText(newText);
      PsiDocumentManager.getInstance(getProject()).commitDocument(document);
    });
  }

  private boolean processClientNow(@NotNull VirtualFile clientFile) {
    return ReadAction.compute(() -> model().processSingleDirtyFile(clientFile));
  }

  public void testIndexUpdateReportsNoChangeForEqualExtractionResult() {
    PsiFile client = configureClientAndModule();
    GuiceNavigationIndex index = getNavigationIndex();
    String path = client.getVirtualFile().getPath();

    GuiceEntryProducer.FileExtractionResult sameResult =
      ReadAction.compute(() -> GuiceEntryProducer.extractFileData(client));
    assertThat(sameResult.entries()).isNotEmpty();
    assertThat(index.updateFile(path, sameResult).changed()).isFalse();

    GuiceNavigationIndex.FileUpdate removal = index.updateFile(path, GuiceEntryProducer.FileExtractionResult.EMPTY);
    assertThat(removal.changed()).isTrue();
    assertThat(removal.affectedTypeFqns()).containsExactly("MyService");
    assertThat(index.updateFile(path, GuiceEntryProducer.FileExtractionResult.EMPTY).changed()).isFalse();
    assertThat(index.removeFile(path).changed()).isFalse();

    GuiceNavigationIndex.FileUpdate addition = index.updateFile(path, sameResult);
    assertThat(addition.changed()).isTrue();
    assertThat(addition.affectedTypeFqns()).containsExactly("MyService");
    assertThat(index.removeFile(path).affectedTypeFqns()).containsExactly("MyService");
  }

  /**
   * A8: a file that was indexed before a re-population, is edited while the pass runs, and is missed by discovery,
   * is processed by the dirty drain and must survive the prune at the end of the pass.
   */
  public void testPopulationKeepsFileProcessedDuringThePass() {
    PsiFile other = myFixture.addFileToProject("Other.java", CLIENT_WITH_ONE_INJECT.replace("class Client", "class Other"));
    PsiFile client = configureClientAndModule();
    GuiceNavigationIndex index = getNavigationIndex();
    String clientPath = client.getVirtualFile().getPath();
    String otherPath = other.getVirtualFile().getPath();
    assertThat(index.getIndexedFiles()).containsAtLeast(clientPath, otherPath);

    GuiceProjectModel model = model();
    // A re-population starts: new generation, snapshot of the indexed paths.
    model.markStructureChanged();
    int generation = model.getStructureGeneration();
    java.util.Set<String> pathsBefore = model.getIndexedFilesSnapshot();
    assertThat(pathsBefore).contains(clientPath);

    // The client is edited during the pass. The drain processes it; discovery did not list it.
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    assertThat(processClientNow(client.getVirtualFile())).isTrue();

    VirtualFile moduleFile = myFixture.findFileInTempDir("MyModule.java");
    GuiceProjectModel.PopulationOutcome outcome =
      model.completePopulationIfCurrent(generation, pathsBefore, java.util.Set.of(moduleFile));
    assertThat(outcome.completed()).isTrue();

    assertThat(index.getIndexedFiles()).contains(clientPath);
    assertThat(index.getFilesWithEntriesOfTypes(List.of("MyKey"))).containsExactly(client.getVirtualFile());
    // A file that nobody processed in this pass and that discovery did not list is still pruned.
    assertThat(index.getIndexedFiles()).doesNotContain(otherPath);
  }

  public void testAffectedTypesPointToFilesWithCounterparts() {
    PsiFile client = configureClientAndModule();
    GuiceNavigationIndex index = getNavigationIndex();
    VirtualFile moduleFile = myFixture.findFileInTempDir("MyModule.java");

    // The module file binds MyService; the client file injects it.
    assertThat(index.getFilesWithEntriesOfTypes(List.of("MyService")))
      .containsExactly(client.getVirtualFile(), moduleFile);
    assertThat(index.getFilesWithEntriesOfTypes(List.of("MyKey"))).isEmpty();

    // Adding an injection point of MyKey affects only the MyKey type.
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    GuiceEntryProducer.FileExtractionResult edited = ReadAction.compute(() -> GuiceEntryProducer.extractFileData(client));
    GuiceNavigationIndex.FileUpdate update = index.updateFile(client.getVirtualFile().getPath(), edited);
    assertThat(update.affectedTypeFqns()).containsExactly("MyKey");
    assertThat(index.getFilesWithEntriesOfTypes(List.of("MyKey"))).containsExactly(client.getVirtualFile());
  }

  public void testFirstHighlightingPassAfterEditShowsNewGutter() {
    configureClientAndModule();
    GuiceProjectModel model = model();
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");

    // In background mode nothing processes the dirty file synchronously, except the line marker pass itself.
    model.getBackgroundUpdater().enableBackgroundModeForTests(getTestRootDisposable());
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyService service2;"));
    assertThat(model.hasDirtyFiles()).isTrue();

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service", "service2");
    assertThat(model.hasDirtyFiles()).isFalse();
  }

  /**
   * N1: the index mutation must finish even when the surrounding progress is cancelled. Entry equality goes
   * through smart pointers, whose {@code equals} calls {@code checkCanceled()}; two injection points of the
   * same key in one file share a hash bucket, so the removal compares them.
   *
   * <p>Note: the test framework runs in a non-cancelable section, so {@code checkCanceled()} does not throw here
   * even without the non-cancelable section in {@link GuiceNavigationIndex}. The test guards the atomicity
   * contract; the real protection is the non-cancelable section plus "read action before the index lock".
   */
  public void testIndexMutationCompletesUnderCancelledIndicator() throws Exception {
    // No other entry of the type: the removal then looks the entries up in the file's own set, which compares
    // the pointers of different fields that share a hash bucket.
    PsiFile client = myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject MyService first;
        @Inject MyService second;
        @Inject MyService third;
      }
      """);
    GuiceNavigationIndex index = getNavigationIndex();
    String path = client.getVirtualFile().getPath();
    GuiceBindingKey key = ReadAction.compute(() -> GuiceBindingKey.forClass(myFixture.findClass("MyService")));
    assertThat(index.findByKey(key, EntryRole.INJECTION_POINT)).hasSize(3);

    // The test thread runs in a non-cancelable section, so the mutation runs on a pooled thread under a
    // cancelled indicator, as a background drain that a write action interrupts.
    var cancelled = new com.intellij.openapi.progress.EmptyProgressIndicator();
    cancelled.cancel();
    var mutation = CompletableFuture.runAsync(
      () -> ProgressManager.getInstance().runProcess(
        () -> ReadAction.run(() -> index.updateFile(path, GuiceEntryProducer.FileExtractionResult.EMPTY)),
        cancelled),
      AppExecutorUtil.getAppExecutorService());
    mutation.get(30, TimeUnit.SECONDS);

    assertThat(index.getIndexedFiles()).doesNotContain(path);
    assertThat(index.findByKey(key, EntryRole.INJECTION_POINT)).isEmpty();
    assertThat(index.getFilesWithEntriesOfTypes(List.of("MyService"))).doesNotContain(client.getVirtualFile());
  }

  /**
   * N2: a file whose extraction throws must leave the dirty set, or the drain loops on it forever.
   */
  public void testDirtyFileWhoseExtractionThrowsDoesNotBlockTheDrain() throws Exception {
    PsiFile client = configureClientAndModule();
    getNavigationIndex();
    GuiceProjectModel model = model();
    VirtualFile clientFile = client.getVirtualFile();

    GuiceProjectModel.setExtractionFailureForTests(vf -> vf.equals(clientFile), getTestRootDisposable());
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    assertThat(model.hasDirtyFiles()).isTrue();

    // The drain runs on another thread so that a regression shows up as a timeout, not as a hung test.
    var drained = CompletableFuture.runAsync(
      () -> ReadAction.run(() -> model.getBackgroundUpdater().processDirtyFilesNow()),
      AppExecutorUtil.getAppExecutorService());
    drained.get(30, TimeUnit.SECONDS);

    assertThat(model.hasDirtyFiles()).isFalse();
    // The old entries stay until an extraction succeeds; the next edit retries.
    assertThat(getNavigationIndex().getFilesWithEntriesOfTypes(List.of("MyService"))).contains(clientFile);
  }

  /**
   * N4: the targeted restart must reach an open file that declares an affected type but holds no entry of
   * its own, such as an interface whose class gutter shows the bindings of other files.
   */
  public void testRestartReachesOpenFilesThatDeclareAffectedTypes() {
    PsiFile api = myFixture.addFileToProject("Api.java", "public interface Api {}\n");
    myFixture.addClass("public class ApiImpl implements Api {}");
    PsiFile module = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
      """);
    getNavigationIndex();
    GuiceProjectModel model = model();
    myFixture.openFileInEditor(api.getVirtualFile());
    myFixture.openFileInEditor(module.getVirtualFile());
    model.clearPendingRestartsForTests();

    replaceClientText(module.getText().replace("protected void configure() {", "protected void configure() {\n    bind(Api.class).to(ApiImpl.class);"));
    assertThat(processClientNow(module.getVirtualFile())).isTrue();
    model.requestRestartOfAffectedEditors(GuiceProjectModel.RestartMode.WHEN_DAEMON_FINISHES, module.getVirtualFile());

    assertThat(model.hasDeferredRestart(api.getVirtualFile())).isTrue();
    assertThat(model.hasDeferredRestart(module.getVirtualFile())).isFalse();
  }

  /**
   * N3: only a real coroutine cancellation may end a collector; a {@code ProcessCanceledException} from one pass
   * (for example from a disposed module) is logged and the collector stays alive.
   */
  public void testOnlyCoroutineCancellationIsFatalForACollector() {
    // ProcessCanceledException extends CancellationException, so only the state of the collector decides.
    var pce = new ProcessCanceledException();
    var disposed = new AlreadyDisposedException("module");
    assertThat(GuiceBackgroundIndexUpdater.isFatalForCollector(pce, true)).isFalse();
    assertThat(GuiceBackgroundIndexUpdater.isFatalForCollector(disposed, true)).isFalse();
    assertThat(GuiceBackgroundIndexUpdater.isFatalForCollector(new RuntimeException(), true)).isFalse();
    assertThat(GuiceBackgroundIndexUpdater.isFatalForCollector(new CancellationException(), false)).isTrue();
    assertThat(GuiceBackgroundIndexUpdater.isFatalForCollector(pce, false)).isTrue();
  }


  public void testEditInsideHighlightingDefersRestartOfOtherOpenFiles() {
    PsiFile client = configureClientAndModule();
    getNavigationIndex();
    GuiceProjectModel model = model();
    VirtualFile moduleFile = myFixture.findFileInTempDir("MyModule.java");

    // Edit the module while it is highlighted: the client shows MyService counterparts, so it must be re-highlighted,
    // but only after the running daemon cycle. (Unit-test mode records the deferred files and issues no restart.)
    myFixture.openFileInEditor(moduleFile);
    // Background mode without the dirty collector: the highlighting pass, not a debounced drain, must be the
    // first to process the module, whatever the timing of the daemon.
    model.getBackgroundUpdater().enableBackgroundModeForTests(getTestRootDisposable(), false);
    model.clearPendingRestartsForTests();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      Document document = myFixture.getEditor().getDocument();
      document.setText(document.getText().replace("bind(MyService.class).to(MyServiceImpl.class);", ""));
      PsiDocumentManager.getInstance(getProject()).commitDocument(document);
    });
    assertThat(model.hasDirtyFiles()).isTrue();
    assertThat(model.hasDeferredRestart(client.getVirtualFile())).isFalse();

    myFixture.doHighlighting();

    assertThat(model.hasDirtyFiles()).isFalse();
    assertThat(model.hasDeferredRestart(client.getVirtualFile())).isTrue();
    assertThat(model.hasDeferredRestart(moduleFile)).isFalse();
  }

  public void testEditThatKeepsEntriesDoesNotChangeIndex() {
    PsiFile client = configureClientAndModule();
    getNavigationIndex();
    VirtualFile clientFile = client.getVirtualFile();

    // A comment changes the stamps but not the entries: no re-highlighting is necessary.
    replaceClientText("// a comment\n" + CLIENT_WITH_ONE_INJECT);
    assertThat(model().hasDirtyFiles()).isTrue();
    assertThat(processClientNow(clientFile)).isFalse();
    assertThat(model().hasDirtyFiles()).isFalse();

    // The anchors moved by the comment: the anchor index must still find the entry at its new position.
    GuiceNavigationIndex index = getNavigationIndex();
    assertThat(ReadAction.compute(() -> {
      PsiClass clientClass = myFixture.findClass("Client");
      return index.findEntriesByAnchor(clientClass.findFieldByName("service", false));
    })).hasSize(1);

    // A second injection point changes the entries.
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    assertThat(processClientNow(clientFile)).isTrue();

    // Processing the same stamp again is a no-op.
    assertThat(processClientNow(clientFile)).isFalse();
  }

  public void testListenersStayActiveAfterStructureChange() {
    configureClientAndModule();
    GuiceProjectModel model = model();
    getNavigationIndex();
    assertThat(model.isIndexActive()).isTrue();

    // A structure change during a population must not deactivate the listeners,
    // or an edit of an already processed file is lost.
    model.markStructureChanged();
    assertThat(model.isIndexActive()).isTrue();
    assertThat(model.isPopulationNeeded()).isTrue();

    getNavigationIndex();
    assertThat(model.isPopulationNeeded()).isFalse();
    assertThat(model.isIndexActive()).isTrue();
  }

  /**
   * M1: whether a population is needed follows the structure generation, not a flag that a concurrent
   * {@code markStructureChanged()} can lose. A completion for an older generation leaves the need in place.
   */
  public void testPopulationNeededFollowsTheStructureGeneration() {
    configureClientAndModule();
    getNavigationIndex();
    GuiceProjectModel model = model();
    assertThat(model.isPopulationNeeded()).isFalse();

    model.markStructureChanged();
    int staleGeneration = model.getStructureGeneration();
    assertThat(model.isPopulationNeeded()).isTrue();

    // A second structure change arrives while the pass for staleGeneration runs.
    model.markStructureChanged();
    assertThat(model.completePopulationIfCurrent(staleGeneration, java.util.Set.of(), java.util.Set.of()).completed()).isFalse();
    assertThat(model.isPopulationNeeded()).isTrue();

    assertThat(model.completePopulationIfCurrent(model.getStructureGeneration(), java.util.Set.of(), java.util.Set.of()).completed()).isTrue();
    assertThat(model.isPopulationNeeded()).isFalse();
  }

  /**
   * M3: a directory event (rename, move, copy) marks only the directory, and the drain expands it to its
   * files in the background. Here the client's data is removed as the VFS listener does before a move, then
   * only its directory is marked dirty: the drain must bring the client back.
   */
  public void testDirtyDirectoryIsExpandedToItsFilesInTheDrain() {
    myFixture.addFileToProject("pkg/Svc.java", "package pkg;\npublic interface Svc {}\n");
    myFixture.addFileToProject("pkg/SvcImpl.java", "package pkg;\npublic class SvcImpl implements Svc {}\n");
    PsiFile client = myFixture.addFileToProject("pkg/Client.java", """
      package pkg;
      import com.google.inject.Inject;
      public class Client {
        @Inject
        Svc service;
      }
      """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(pkg.Svc.class).to(pkg.SvcImpl.class);
        }
      }
      """);
    VirtualFile clientFile = client.getVirtualFile();
    GuiceNavigationIndex index = getNavigationIndex();
    assertThat(index.getFilesWithEntriesOfTypes(List.of("pkg.Svc"))).contains(clientFile);

    GuiceProjectModel model = model();
    ReadAction.run(() -> model.removeFile(clientFile));
    assertThat(index.getIndexedFiles()).doesNotContain(clientFile.getPath());
    assertThat(model.hasDirtyFiles()).isFalse();

    model.markDirectoryDirty(clientFile.getParent());
    assertThat(model.hasDirtyFiles()).isTrue();

    getNavigationIndex();
    assertThat(model.hasDirtyFiles()).isFalse();
    assertThat(index.getIndexedFiles()).contains(clientFile.getPath());
    assertThat(index.getFilesWithEntriesOfTypes(List.of("pkg.Svc"))).contains(clientFile);
  }

  /**
   * M4: saving a document changes the VFS stamp but not the PSI; the file must not be extracted again.
   */
  public void testSaveWithoutPsiChangeDoesNotReExtract() {
    PsiFile client = configureClientAndModule();
    getNavigationIndex();
    GuiceProjectModel model = model();
    VirtualFile clientFile = client.getVirtualFile();

    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    assertThat(processClientNow(clientFile)).isTrue();
    assertThat(model.hasDirtyFiles()).isFalse();

    Document document = myFixture.getEditor().getDocument();
    WriteAction.runAndWait(() -> FileDocumentManager.getInstance().saveDocument(document));

    // From here on, an extraction of the client is a failure of the test.
    GuiceProjectModel.setExtractionFailureForTests(vf -> vf.equals(clientFile), getTestRootDisposable());
    assertThat(processClientNow(clientFile)).isFalse();
    assertThat(model.hasDirtyFiles()).isFalse();
  }

  /**
   * M15: the collectors that a test starts stop with the test, so a pending debounce cannot run into the
   * next test of the shared project.
   */
  public void testBackgroundModeCollectorsStopWhenDisposed() {
    configureClientAndModule();
    getNavigationIndex();
    GuiceBackgroundIndexUpdater updater = model().getBackgroundUpdater();
    assertThat(updater.collectorsRunningForTests()).isFalse();

    Disposable scope = Disposer.newDisposable();
    updater.enableBackgroundModeForTests(scope);
    assertThat(updater.collectorsRunningForTests()).isTrue();
    assertThat(updater.isSynchronous()).isFalse();

    Disposer.dispose(scope);
    assertThat(updater.collectorsRunningForTests()).isFalse();
    assertThat(updater.isSynchronous()).isTrue();

    // A later test can start them again.
    updater.enableBackgroundModeForTests(getTestRootDisposable());
    assertThat(updater.collectorsRunningForTests()).isTrue();
  }

  public void testBackgroundModeDrainsDirtyFilesAfterDebounce() {
    PsiFile client = configureClientAndModule();
    GuiceNavigationIndex index = getNavigationIndex();
    GuiceProjectModel model = model();
    PsiClass myKey = myFixture.findClass("MyKey");
    assertThat(index.findByKey(GuiceBindingKey.forClass(myKey), EntryRole.INJECTION_POINT)).isEmpty();

    model.getBackgroundUpdater().enableBackgroundModeForTests(getTestRootDisposable());
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    assertThat(model.hasDirtyFiles()).isTrue();

    PlatformTestUtil.waitWithEventsDispatching("The background updater did not drain the dirty files",
                                               () -> !model.hasDirtyFiles(), 30);

    assertThat(ReadAction.compute(() -> index.findByKey(GuiceBindingKey.forClass(myKey), EntryRole.INJECTION_POINT)))
      .hasSize(1);
    assertThat(client.isValid()).isTrue();
  }

  public void testModulePassDoesNotLoadAstOfInjectingFiles() {
    int n = 20;
    PsiFile module = myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class MyModule extends AbstractModule {
        @Override protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
        @Provides MyKey provideKey() { return null; }
      }
      """);
    List<PsiFile> clients = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      clients.add(myFixture.addFileToProject("Client" + i + ".java", """
        import com.google.inject.Inject;
        public class Client%d {
          @Inject MyService service;
          @Inject Client%d(MyKey key) {}
        }
        """.formatted(i, i)));
    }
    myFixture.configureFromExistingVirtualFile(module.getVirtualFile());
    getNavigationIndex();
    myFixture.doHighlighting();

    // Drop the ASTs of the injecting files, as the IDE does under memory pressure or for files never opened.
    WriteAction.run(() -> {
      for (PsiFile client : clients) ((PsiFileImpl)client).onContentReload();
    });
    assertThat(countLoaded(clients)).isEqualTo(0);

    // A whole-file pass of the module must find and sort the 2 * n counterparts through stubs only.
    DaemonCodeAnalyzer.getInstance(getProject()).restart(module, "test");
    myFixture.doHighlighting();
    assertThat(myFixture.findAllGutters()).isNotEmpty();
    assertThat(countLoaded(clients)).isEqualTo(0);

    // Reading the targets, as the popup does, may load them.
    assertThat(gutterTargets(TO_INJECTION_POINTS_TOOLTIP)).hasSize(2 * n);
  }

  private static int countLoaded(List<PsiFile> files) {
    return ReadAction.compute(() -> (int)files.stream().filter(f -> ((PsiFileImpl)f).isContentsLoaded()).count());
  }

  /**
   * The entries hold objects of the contributors. A contributor added or removed by a dynamic plugin
   * must drop them at once, not at the next population, or an unloaded plugin's class loader stays alive.
   */
  public void testContributorChangeClearsIndexAtOnce() {
    configureClientAndModule();
    GuiceNavigationIndex index = getNavigationIndex();
    GuiceProjectModel model = model();
    assertThat(index.getIndexedFiles()).isNotEmpty();

    Disposable contributorLifetime = Disposer.newDisposable();
    try {
      GuiceBindingContributor.EP_NAME.getPoint().registerExtension(registrar -> {}, contributorLifetime);

      // Cleared synchronously by the extension point listener, before any repopulation.
      assertThat(index.getIndexedFiles()).isEmpty();
      assertThat(model.isPopulationNeeded()).isTrue();

      getNavigationIndex();
      assertThat(index.getIndexedFiles()).isNotEmpty();
      assertThat(model.isPopulationNeeded()).isFalse();
    }
    finally {
      Disposer.dispose(contributorLifetime);
    }

    // The removal clears the index as well.
    assertThat(index.getIndexedFiles()).isEmpty();
    assertThat(model.isPopulationNeeded()).isTrue();
    getNavigationIndex();
    assertThat(index.getIndexedFiles()).isNotEmpty();
  }
}
