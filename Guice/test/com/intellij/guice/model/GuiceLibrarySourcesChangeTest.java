package com.intellij.guice.model;

import com.intellij.guice.GuiceTestBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.workspace.WorkspaceModel;
import com.intellij.platform.workspace.jps.entities.LibraryRoot;
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId;
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * A library whose source roots change (sources attached or detached, also lazily per file after a navigation)
 * keeps its classes: the index re-extracts the files of that library and does not rebuild from scratch.
 */
public class GuiceLibrarySourcesChangeTest extends GuiceTestBase {

  private LibraryRoot root(String url, LibraryRootTypeId type) {
    VirtualFileUrlManager manager = WorkspaceModel.getInstance(getProject()).getVirtualFileUrlManager();
    return new LibraryRoot(manager.getOrCreateFromUrl(url), type, LibraryRoot.InclusionOptions.ROOT_ITSELF);
  }

  public void testOnlySourceRootsDifferRecognisesSourceAttachmentsAndDetachments() {
    LibraryRootTypeId compiled = LibraryRootTypeId.Companion.getCOMPILED();
    LibraryRootTypeId sources = LibraryRootTypeId.Companion.getSOURCES();
    LibraryRoot classes = root("jar:///lib/a.jar!/", compiled);
    LibraryRoot otherClasses = root("jar:///lib/b.jar!/", compiled);
    LibraryRoot singleSource = root("file:///lib/src/com/example/Foo.java", sources);
    LibraryRoot sourceJar = root("jar:///lib/a-src.jar!/", sources);

    // Lazy attachment: a single file, then an archive next to it; a sync that clears the sources again.
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes), List.of(classes, singleSource))).isTrue();
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes, singleSource), List.of(classes, singleSource, sourceJar))).isTrue();
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes, sourceJar), List.of(classes))).isTrue();

    // Class roots change, or nothing changes: a structure change as before.
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes), List.of(otherClasses))).isFalse();
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes), List.of(classes, otherClasses, singleSource))).isFalse();
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes), List.of(classes))).isFalse();
    assertThat(GuiceBackgroundIndexUpdater.onlySourceRootsDiffer(List.of(classes, singleSource), List.of(classes, singleSource))).isFalse();
  }

  public void testSourceChangeOfALibraryReExtractsOnlyItsClasses() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    getNavigationIndex();
    GuiceProjectModel model = model();
    assertThat(model.isPopulationNeeded()).isFalse();
    assertThat(model.hasDirtyFiles()).isFalse();

    List<String> guiceClassRoots = new ArrayList<>();
    OrderEnumerator.orderEntries(getModule()).librariesOnly().forEachLibrary(library -> {
      for (String url : library.getUrls(OrderRootType.CLASSES)) {
        if (url.contains("guice")) guiceClassRoots.add(url);
      }
      return true;
    });
    assertThat(guiceClassRoots).isNotEmpty();
    List<String> jarPaths = ContainerUtil.map(guiceClassRoots, VfsUtilCore::urlToPath);

    model.markLibraryClassesDirty(guiceClassRoots);

    // No full rebuild, only the indexed classes of that library are queued.
    assertThat(model.isPopulationNeeded()).isFalse();
    List<VirtualFile> dirty = model.collectDirtyFiles();
    assertThat(dirty).isNotEmpty();
    for (VirtualFile file : dirty) {
      assertThat(ContainerUtil.exists(jarPaths, file.getPath()::startsWith)).isTrue();
    }

    // The drain ends, and the library classes keep their entries.
    int filesBefore = getNavigationIndex().getIndexedFiles().size();
    ReadAction.run(() -> model.getBackgroundUpdater().processDirtyFilesNow());
    assertThat(model.hasDirtyFiles()).isFalse();
    assertThat(getNavigationIndex().getIndexedFiles()).hasSize(filesBefore);
  }

  private GuiceProjectModel model() {
    return GuiceProjectModel.getInstance(getProject());
  }
}
