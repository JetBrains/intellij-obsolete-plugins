package com.intellij.gwt.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class GwtLibrarySearchingPolicy extends RootPolicy<Boolean> {
  private final VirtualFile myUserJar;
  private final List<LibraryOrderEntry> myGwtLibraries = new ArrayList<>();
  private final FacetEditorContext myEditorContext;

  GwtLibrarySearchingPolicy(FacetEditorContext editorContext, final VirtualFile userJar) {
    myUserJar = userJar;
    myEditorContext = editorContext;
  }

  public @Nullable LibraryOrderEntry getGwtLibrary() {
    return myGwtLibraries.size() == 1 ? myGwtLibraries.get(0) : null;
  }

  public boolean containsLibrary(final ModuleRootModel rootModel) {
    final Ref<Boolean> contains = Ref.create(false);
    rootModel.orderEntries().using(myEditorContext.getModulesProvider()).recursively().librariesOnly().forEach(orderEntry -> {
      if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
        Library library = libraryOrderEntry.getLibrary();
        if (library != null) {
          VirtualFile[] files = myEditorContext.getLibraryFiles(library, OrderRootType.CLASSES);
          for (VirtualFile file : files) {
            if (file.equals(myUserJar)) {
              contains.set(true);
              return false;
            }
          }
          if (files.length == 1 && LibraryUtil.isClassAvailableInLibrary(new VirtualFile[]{files[0]}, GwtSdkUtil.GWT_CLASS_NAME)) {
            myGwtLibraries.add(libraryOrderEntry);
          }
        }
      }
      return true;
    });
    return contains.get();
  }
}
