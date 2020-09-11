package com.intellij.cvsSupport2;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

import java.io.File;
import java.io.IOException;

class TestDirectory extends TestObject {
  public static final String NEW_CONTENT = "new content";
  public static final String INITIAL_CONTENT = "initial content\nlalala\n";

  TestDirectory(String name) {
    super(name);
  }

  TestDirectory(File base, String name) {
    super(base, name);
  }

  @Override
  public void addToVcs(CvsVcs2 vcs) throws VcsException {
    vcs.getStandardOperationsProvider().addDirectory(getParent(), getName(), null, null);
  }

  @Override
  public void addToVcs(CvsVcs2 vcs, KeywordSubstitution ks) throws VcsException {
    vcs.getStandardOperationsProvider().addDirectory(getParent(), getName(), null, null);
  }

  @Override
  protected String repositoryName() {
    return relativePathInProject();
  }

  @Override
  public void createInProject() {
    mkdir();
  }

  @Override
  public void deleteFromVcs(CvsVcs2 vcs) throws VcsException {
    vcs.getStandardOperationsProvider().removeDirectory(getAbsolutePath(), null, null);
  }

  @Override
  public boolean deleteFromProject() {
    return true;
  }

  @Override
  protected boolean deleteFromFileSystem() {
    return FileUtil.delete(this);
  }

  public void changeContentTo(String content) throws IOException {
    FileUtil.writeToFile(this, content);
  }

  public boolean repositoryVersionContains(String newContent) throws IOException {
    return FileUtil.loadFile(myRepositoryVersion()).contains(newContent);
  }

  public static TestDirectory createInProject(String s) {
    return new TestDirectory(TestObject.getProjectDirectory(), s);
  }
}


