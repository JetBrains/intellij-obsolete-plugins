package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.guice.GuiceTestBase;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.testFramework.PlatformTestUtil;
import java.util.ArrayList;
import java.util.List;
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
    assertThat(index.getFilesWithEntriesOfTypes(java.util.List.of("MyKey"))).containsExactly(client.getVirtualFile());
    // A file that nobody processed in this pass and that discovery did not list is still pruned.
    assertThat(index.getIndexedFiles()).doesNotContain(otherPath);
  }

  public void testAffectedTypesPointToFilesWithCounterparts() {
    PsiFile client = configureClientAndModule();
    GuiceNavigationIndex index = getNavigationIndex();
    VirtualFile moduleFile = myFixture.findFileInTempDir("MyModule.java");

    // The module file binds MyService; the client file injects it.
    assertThat(index.getFilesWithEntriesOfTypes(java.util.List.of("MyService")))
      .containsExactly(client.getVirtualFile(), moduleFile);
    assertThat(index.getFilesWithEntriesOfTypes(java.util.List.of("MyKey"))).isEmpty();

    // Adding an injection point of MyKey affects only the MyKey type.
    replaceClientText(CLIENT_WITH_ONE_INJECT.replace("MyService service;", "MyService service;\n  @Inject MyKey key;"));
    GuiceEntryProducer.FileExtractionResult edited = ReadAction.compute(() -> GuiceEntryProducer.extractFileData(client));
    GuiceNavigationIndex.FileUpdate update = index.updateFile(client.getVirtualFile().getPath(), edited);
    assertThat(update.affectedTypeFqns()).containsExactly("MyKey");
    assertThat(index.getFilesWithEntriesOfTypes(java.util.List.of("MyKey"))).containsExactly(client.getVirtualFile());
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

  public void testEditInsideHighlightingDefersRestartOfOtherOpenFiles() {
    PsiFile client = configureClientAndModule();
    getNavigationIndex();
    GuiceProjectModel model = model();
    VirtualFile moduleFile = myFixture.findFileInTempDir("MyModule.java");

    // Edit the module while it is highlighted: the client shows MyService counterparts, so it must be re-highlighted,
    // but only after the running daemon cycle. (Unit-test mode records the deferred files and issues no restart.)
    myFixture.openFileInEditor(moduleFile);
    model.getBackgroundUpdater().enableBackgroundModeForTests(getTestRootDisposable());
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
