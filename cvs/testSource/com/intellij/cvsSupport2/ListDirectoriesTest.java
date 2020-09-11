package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsBrowser.AbstractVcsDataProvider;
import com.intellij.cvsSupport2.cvsBrowser.FolderDataProvider;
import com.intellij.cvsSupport2.cvsBrowser.RootDataProvider;
import com.intellij.cvsSupport2.cvsoperations.common.CvsOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.DirectoryContent;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.DirectoryContentProvider;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetModuleContentOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetModulesListOperation;
import com.intellij.openapi.vcs.VcsException;
import org.netbeans.lib.cvsclient.command.checkout.Module;

import java.io.IOException;
import java.util.Collection;

/**
 * author: lesya
 */
public class ListDirectoriesTest extends CvsTestsWorkingWithImportedProject{

  public void testGetAllModules() throws Exception {
    String firstModuleName = "module1";
    String secondModuleName = "module2";

    addModules(new String[]{firstModuleName, secondModuleName});

    RootDataProvider rootDataProvider = RootDataProvider.createTestInstance(this);
    DirectoryContentProvider directoryContentProvider = rootDataProvider.createDirectoryContentProvider("");
    execute((CvsOperation)directoryContentProvider);
    Collection modules = directoryContentProvider.getDirectoryContent().getSubModules();
    assertEquals(2, modules.size());
    assertTrue(modules.contains(firstModuleName));
    assertTrue(modules.contains(secondModuleName));
  }

  public void testGetAllDirectories() throws Exception {
    String projectDirectoryName = TestObject.getProjectDirectory().getName();

    TestObject directory1 = createDirectoryAndAddToVcs("directory1");
    TestObject directory2 = createDirectoryAndAddToVcs("directory2");

    putTestFileToRepository();

    Collection directories = getDirectoryContent(".").getSubDirectories();

    assertEquals(2, directories.size());
    checkCollectionContainsObject("CVSROOT", directories);
    checkCollectionContainsObject(projectDirectoryName, directories);

    DirectoryContent directoryContent = getDirectoryContent(projectDirectoryName);
    Collection subdirectories = directoryContent.getSubDirectories();
    assertEquals(2, subdirectories.size());
    checkCollectionContainsObject(directory1.getName(), subdirectories);
    checkCollectionContainsObject(directory2.getName(), subdirectories);

    Collection files = directoryContent.getFiles();
    assertEquals(1, files.size());
    checkCollectionContainsObject(TEST_FILE.getName(), files);
  }

  public void testModuleContent() throws Exception {
    String dir1Name = "dir1";
    String dir2Name = "dir11";
    String dir11Name = "dir11";
    String dir21Name = "dir21";
    String dir111Name = "dir111";
    String dir211Name = "dir211";
    String dir1111Name = "dir1111";
    String dir2111Name = "dir2111";


    String fileName = "file.txt";
    String moduleName = "module1";

    TestDirectory dir1 = TestDirectory.createInProject(dir1Name);

    TestDirectory dir2 = TestDirectory.createInProject(dir2Name);

    TestDirectory dir11 = new TestDirectory(dir1, dir11Name);
    TestDirectory dir111 = new TestDirectory(dir11, dir111Name);
    TestDirectory dir1111 = new TestDirectory(dir111, dir1111Name);

    TestDirectory dir21 = new TestDirectory(dir2, dir21Name);
    TestDirectory dir211 = new TestDirectory(dir21, dir211Name);
    TestDirectory dir2111 = new TestDirectory(dir211, dir2111Name);
    String module2Name = "module2";

    addFile(dir1);
    addFile(dir2);
    addFile(dir11);
    addFile(dir21);
    addFile(dir111);
    addFile(dir211);
    addFile(dir1111);
    addFile(dir2111);


    TestFile file = new TestFile(dir1111, fileName);
    addFile(file);

    TestFile fileInModule = new TestFile(dir11, fileName);
    addFile(fileInModule);



    addModule(module2Name, getModuleName() + "/" + dir2Name);

    addModule(moduleName, getModuleName() + "/" + dir1Name + "/" + dir11Name + " &" + module2Name);

    GetModuleContentOperation moduleContentOperation = new GetModuleContentOperation(this, moduleName);
    execute(moduleContentOperation);

    Collection directories = moduleContentOperation.getDirectoryContent().getSubDirectories();

    assertEquals("Actual directories: " + directories, 1, directories.size());
    assertEquals(TestObject.getProjectDirectory().getName() + "/" + dir1Name + "/" + dir11Name + "/" + dir111Name, directories.iterator().next());

    Collection modules = moduleContentOperation.getDirectoryContent().getSubModules();
    assertEquals(1, modules.size());
    assertEquals(module2Name, modules.iterator().next());

    Collection files = moduleContentOperation.getDirectoryContent().getFiles();
    assertEquals(1, files.size());
    assertEquals(TestObject.getProjectDirectory().getName() + "/" + dir1Name + "/" + dir11Name + "/" + fileName,
                 files.iterator().next());
  }

  public void testModuleWithMinusDOptionContent() throws Exception {
    String dir1Name = "dir1";
    String dir11Name = "dir11";
    String dir111Name = "dir111";
    String dir1111Name = "dir1111";

    String fileName = "file.txt";
    String moduleName = "module1";

    TestDirectory dir1 = TestDirectory.createInProject(dir1Name);

    TestDirectory dir11 = new TestDirectory(dir1, dir11Name);
    TestDirectory dir111 = new TestDirectory(dir11, dir111Name);
    TestDirectory dir1111 = new TestDirectory(dir111, dir1111Name);

    addFile(dir1);
    addFile(dir11);
    addFile(dir111);
    addFile(dir1111);


    TestFile file = new TestFile(dir1111, fileName);
    addFile(file);

    TestFile fileInModule = new TestFile(dir11, fileName);
    addFile(fileInModule);


    addModule(moduleName, "-d test " + getModuleName() + "/" + dir1Name + "/" + dir11Name);

    GetModuleContentOperation moduleContentOperation = new GetModuleContentOperation(this, moduleName);
    execute(moduleContentOperation);

    Collection directories = moduleContentOperation.getDirectoryContent().getSubDirectories();

    assertEquals(1, directories.size());
    assertEquals(TestObject.getProjectDirectory().getName() + "/" + dir1Name + "/" + dir11Name + "/" + dir111Name, directories.iterator().next());

    Collection files = moduleContentOperation.getDirectoryContent().getFiles();
    assertEquals(1, files.size());
    assertEquals(TestObject.getProjectDirectory().getName() + "/" + dir1Name + "/" + dir11Name + "/" + fileName,
                 files.iterator().next());
  }

  public void testModuleWithAliasContent() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir1");
    TestDirectory dir2 = new TestDirectory(dir1, "dir2");
    TestDirectory dir3 = new TestDirectory(dir2, "dir3");
    TestDirectory dir4 = new TestDirectory(dir3, "dir4");


    addFile(dir1);
    addFile(dir2);
    addFile(dir3);
    addFile(dir4);

    TestFile file = new TestFile(dir4, "file.txt");
    addFile(file);

    String moduleName = "module1";
    addModule(moduleName, getModuleName() + "/" + "dir1/dir2");

    String alias = "alias";
    addModules(new String[] {alias +  " -a "  + getModuleName()});

    GetModuleContentOperation moduleContentOperation = new GetModuleContentOperation(this, alias);
    execute(moduleContentOperation);

    Collection directories = moduleContentOperation.getDirectoryContent().getSubDirectories();

    assertEquals(directories.toString(),1, directories.size());
  }

  public void testIncorrectModuleWithAliasContent() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir1");
    TestDirectory dir2 = new TestDirectory(dir1, "dir2");
    TestDirectory dir3 = new TestDirectory(dir2, "dir3");
    TestDirectory dir4 = new TestDirectory(dir3, "dir4");


    addFile(dir1);
    addFile(dir2);
    addFile(dir3);
    addFile(dir4);

    TestFile file = new TestFile(dir4, "file.txt");
    addFile(file);

    String alias = "alias";
    addModules(new String[] {"invalid" +  " -a "  + alias});

    GetModuleContentOperation moduleContentOperation = new GetModuleContentOperation(this, alias);
    try{
    execute(moduleContentOperation);
    } catch (Exception ex){
      assertEquals("Cannot expand 'alias'", ex.getLocalizedMessage());
      return;
    }

    fail("Exception expected");

  }

  public void testModuleSynonims() throws Exception {
    String moduleName = "moduleName";
    String options = "-a syn1 syn2";
    addModule(moduleName, options);
    Module module = getFirstModules();
    assertEquals(moduleName, module.getModuleName());
    assertEquals(options, module.getOptions());

  }

  public void testGetMultiLineModules() throws Exception {
    String moduleName = "_ant-src-minimum";
    String options = "-d source_lib/org/apache/tools \\\nshare/utils/buildtools/ant/apache-ant-1.5.3-1/src/main/org/apache/tools";
    addModule(moduleName, options);
    Module module = getFirstModules();
    assertEquals(moduleName, module.getModuleName());
    assertEquals("-d source_lib/org/apache/tools share/utils/buildtools/ant/apache-ant-1.5.3-1/src/main/org/apache/tools", module.getOptions());

  }

  private Module getFirstModules() throws Exception {
    GetModulesListOperation operation = new GetModulesListOperation(this);
    execute(operation);
    Collection modules = operation.getModulesInRepository();
    return (Module) modules.iterator().next();
  }

  private DirectoryContent getDirectoryContent(String projectDirectoryName) throws Exception {
    AbstractVcsDataProvider provider = projectDirectoryName.equals(".") ? RootDataProvider.createTestInstance(this) :
                                        new FolderDataProvider(this);
    DirectoryContentProvider directoryContentProvider = provider.createDirectoryContentProvider(projectDirectoryName);
    execute((CvsOperation)directoryContentProvider);
    return directoryContentProvider.getDirectoryContent();

  }

  private static void checkCollectionContainsObject(Object object, Collection directories) {
    assertTrue("Cannot find " + object + " in " + directories, directories.contains(object));
  }

  private TestObject createDirectoryAndAddToVcs(String name) throws IOException, VcsException {
    TestObject directory1 = new TestDirectory(TestObject.getProjectDirectory(), name);
    directory1.createInProject();
    directory1.addToVcs(myVcs);
    return directory1;
  }

}
