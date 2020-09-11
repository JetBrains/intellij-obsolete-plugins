package com.intellij.cvsSupport2;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

import java.io.File;
import java.io.IOException;


class TestFile extends TestObject {
  public static final String INITIAL_CONTENT = "initial' content\nlalala";

  public static TestFile createInProject(String name){
    return new TestFile(getProjectDirectory(), name);
  }

  TestFile(String name) {
    super(name);
  }

  TestFile(File base, String name) {
    super(base, name);
  }

  @Override
  public void addToVcs(CvsVcs2 vcs) throws VcsException {
    addToVcs(vcs, KeywordSubstitution.KEYWORD_EXPANSION);
  }

  @Override
  public void addToVcs(CvsVcs2 vcs, KeywordSubstitution ks) throws VcsException {
    vcs.getStandardOperationsProvider().addFile(getParent(), getName(), ks, null);
  }

  @Override
  protected String repositoryName() {
    return relativePathInProject() + ",v";
  }

  @Override
  public void createInProject() throws IOException {
    changeContentTo(INITIAL_CONTENT);
  }

  public void changeContentTo(String content) throws IOException {
    try {
      Thread.sleep(1500);
    }
    catch (InterruptedException e) {

    }
    FileUtil.writeToFile(this, content);
  }

  public boolean repositoryVersionContains(String newContent) throws IOException {
    String preparedString = newContent.replaceAll(System.getProperty("line.separator"), "\n");
    return FileUtil.loadFile(myRepositoryVersion()).contains(preparedString);
  }

  public boolean projectVersionContains(String newContent) throws IOException {
    return getContent().contains(newContent);
  }

  @Override
  public void deleteFromVcs(CvsVcs2 vcs) throws VcsException {
    vcs.getStandardOperationsProvider().removeFile(getAbsolutePath(), null, null);
  }

  @Override
  public boolean deleteFromProject() {
    return deleteFromFileSystem();
  }

  @Override
  protected boolean deleteFromFileSystem() {
    if (!exists()) return true;
    for (int i = 0; i < 10; i++){
      if (delete()) return true;
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  public String getContent() throws IOException {
    return FileUtil.loadFile(this);
  }

  public boolean isLocallyAdded() {
    return getMyEntry().isAddedFile();
  }

  private Entry getMyEntry() {
    return CvsUtil.getEntryFor(this);
  }

  public boolean isLocallyRemoved() {
    return getMyEntry().isRemoved();
  }
}


