package com.intellij.gwt.sdk;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.openapi.roots.libraries.DummyLibraryProperties;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;

public final class GwtLibraryPresentationProvider extends LibraryPresentationProvider<DummyLibraryProperties> {
  private static final LibraryKind GWT_LIBRARY_KIND = LibraryKind.create("gwt");

  public GwtLibraryPresentationProvider() {
    super(GWT_LIBRARY_KIND);
  }

  @Override
  public Icon getIcon(DummyLibraryProperties properties) {
    return GwtIcons.GoogleSmall;
  }

  @Override
  public DummyLibraryProperties detect(@NotNull List<VirtualFile> classesRoots) {
    for (VirtualFile root : classesRoots) {
      final VirtualFile jar = JarFileSystem.getInstance().getVirtualFileForJar(root);
      if (jar != null && jar.getName().equals("gwt-user.jar")) {
        return DummyLibraryProperties.INSTANCE;
      }
    }
    return null;
  }

  @Override
  public @Nls String getDescription(@NotNull DummyLibraryProperties properties) {
    return GwtBundle.message("library.presentation.description");
  }
}
