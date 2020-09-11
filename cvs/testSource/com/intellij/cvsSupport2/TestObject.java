package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.application.DeletedCVSDirectoryStorage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;
import org.netbeans.lib.cvsclient.connection.PServerPasswordScrambler;

import java.io.File;
import java.io.IOException;

public abstract class TestObject extends File {
  private static File WORKING_FOLDER;
  private static File REPOSITORY_DIRECTORY;
  private static TestDirectory PROJECT_DIRECTORY;

  private static String REPOSITORY_PATH;
  private static String USER;
  private static String SCRAMBLED_PASSWORD;

  public TestObject(String child) {
    super(child);
  }

  public TestObject(File parent, String child) {
    super(parent, child);
  }

  public TestObject setDeletedStorage(DeletedCVSDirectoryStorage deletedStorage) {
    return this;
  }

  public abstract void createInProject() throws IOException;

  public boolean isInRepository() throws IOException {
    final File repositoryVersion = myRepositoryVersion();
    if (!repositoryVersion.exists()) {
      return false;
    } else {
      if (repositoryVersion.isDirectory()) {
        return true;
      }
      else {
        String content = FileUtil.loadFile(repositoryVersion);
        final int stateIndex = content.indexOf("state");
        if (stateIndex < 0) {
          return false;
        } else {
          content = content.substring(stateIndex + 5).trim();
          return !content.startsWith("dead");
        }
      }
    }
  }

  protected File myRepositoryVersion() {

    File projectInRepository = new File(
            TestObject.getRepositoryDirectory(),
            TestObject.getProjectDirectory().getName());

    return new File(projectInRepository, repositoryName());
  }

  public String relativePathInProject() {
    return FileUtil.getRelativePath(getProjectDirectory(), this);
  }

  public String nameInRepository() {
    return getProjectDirectory().getName() + "/" + relativePathInProject();
  }

  public VirtualFile getVirtualFile() {

    try {
      final VirtualFile[] result = new VirtualFile[1];
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          result[0] = LocalFileSystem.getInstance().refreshAndFindFileByPath(getCanonicalPath().replace('\\', '/'));
        }
        catch (IOException ignored) {
        }
      });
      return result[0];
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract String repositoryName();

  public abstract void addToVcs(CvsVcs2 vcs) throws VcsException;
  public abstract void addToVcs(CvsVcs2 vcs, KeywordSubstitution ks) throws VcsException;

  public abstract void deleteFromVcs(CvsVcs2 vcs) throws VcsException;

  public abstract boolean deleteFromProject();

  protected abstract boolean deleteFromFileSystem();

  public static File getWorkingFolder() {
    if (WORKING_FOLDER == null) {
      init();
    }
    return WORKING_FOLDER;
  }

  private static void init() {
    try {
      String cvsRoot = System.getProperty("cvs.root");
      final String tempDirectory = FileUtil.getTempDirectory();
      if (cvsRoot == null) {
        cvsRoot = tempDirectory + File.separator + "TestRepository";
      }
      WORKING_FOLDER = new File(tempDirectory);
      REPOSITORY_DIRECTORY = new File(cvsRoot);
      PROJECT_DIRECTORY = new TestDirectory(WORKING_FOLDER, "VcsTestProject");
      REPOSITORY_PATH= REPOSITORY_DIRECTORY.getCanonicalPath();
      USER = System.getProperty("cvs.user");
      if (USER == null) {
        USER = "builduser";
      }
      String pwd = System.getProperty("cvs.password");
      if (pwd == null) {
        SCRAMBLED_PASSWORD = PServerPasswordScrambler.getInstance().scramble("");
      }
      else {
        SCRAMBLED_PASSWORD = PServerPasswordScrambler.getInstance().scramble(pwd);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static File getRepositoryDirectory() {
    if (REPOSITORY_DIRECTORY == null) {
      init();
    }
    return REPOSITORY_DIRECTORY;
  }

  public static TestDirectory getProjectDirectory() {
    if (PROJECT_DIRECTORY == null) {
      init();
    }
    return PROJECT_DIRECTORY;
  }

  public static String getRepositoryPath() {
    if (REPOSITORY_PATH == null) {
      init();
    }
    return REPOSITORY_PATH;
  }

  public static String getUser() {
    if (USER == null) {
      init();
    }
    return USER;
  }

  public static String getScrambledPassword() {
    if (SCRAMBLED_PASSWORD == null) {
      init();
    }
    return SCRAMBLED_PASSWORD;
  }
}
