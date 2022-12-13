package com.intellij.seam.dependencies;

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SeamDependenciesVirtualFileSystem extends DeprecatedVirtualFileSystem {
  @NonNls static final String PROTOCOL = "SEAM_DEPENDENCIES";
  @NonNls static final String FILE_GRAPH = "SEAM_DEPENDENCIES_GRAPH";

  @Override
  @NotNull
  public String getProtocol() {
    return PROTOCOL;
  }

  @Override
  @Nullable
  public VirtualFile findFileByPath(@NotNull String path) {
   return new MyVirtualFile(path);
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  @Nullable
  public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
    return findFileByPath(path);
  }

  private class MyVirtualFile extends VirtualFile {
    private final String myName;

    MyVirtualFile(final String name) {

      myName = name;
    }

    @Override
    @NotNull
    public String getName() {
      return myName;
    }

    @Override
    @NotNull
    public VirtualFileSystem getFileSystem() {
      return SeamDependenciesVirtualFileSystem.this;
    }

    @Override
    @NotNull
    public String getPath() {
      return getName();
    }

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    @Nullable
    public VirtualFile getParent() {
      return null;
    }

    @Override
    public VirtualFile[] getChildren() {
      return VirtualFile.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
      throw new UnsupportedOperationException("getOutputStream is not implemented in : " + getClass());
    }

    @Override
    public byte @NotNull [] contentsToByteArray() throws IOException {
      return ArrayUtilRt.EMPTY_BYTE_ARRAY;
    }

    @Override
    public long getTimeStamp() {
      return 0;
    }

    @Override
    public long getLength() {
      return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {

    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
      throw new UnsupportedOperationException("getInputStream is not implemented in : " + getClass());
    }

    @Override
    public long getModificationStamp() {
      return 0;
    }

    public Icon getIcon() {
      return SeamIcons.Seam;
    }

    @Override
    public @NotNull String getPresentableName() {
      return SeamBundle.message("seam.dependencies.file.name", getName());
    }
  }
}
