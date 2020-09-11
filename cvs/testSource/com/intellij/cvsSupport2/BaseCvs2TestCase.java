package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.actions.update.UpdateSettings;
import com.intellij.cvsSupport2.actions.update.UpdateSettingsOnCvsConfiguration;
import com.intellij.cvsSupport2.application.CvsEntriesManager;
import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import com.intellij.cvsSupport2.config.CvsConfiguration;
import com.intellij.cvsSupport2.config.CvsRootConfiguration;
import com.intellij.cvsSupport2.connections.CvsEnvironment;
import com.intellij.cvsSupport2.connections.login.CvsLoginWorker;
import com.intellij.cvsSupport2.connections.pserver.PServerCvsSettings;
import com.intellij.cvsSupport2.connections.pserver.PServerLoginProvider;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutor;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutorCallback;
import com.intellij.cvsSupport2.cvsExecution.ModalityContextImpl;
import com.intellij.cvsSupport2.cvshandlers.CommandCvsHandler;
import com.intellij.cvsSupport2.cvshandlers.CvsHandler;
import com.intellij.cvsSupport2.cvshandlers.CvsUpdatePolicy;
import com.intellij.cvsSupport2.cvsoperations.common.CvsOperation;
import com.intellij.cvsSupport2.cvsoperations.common.ReceivedFileProcessor;
import com.intellij.cvsSupport2.cvsoperations.cvsCheckOut.CheckoutProjectOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsCommit.CommitFilesOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsImport.ImportOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsMessages.CvsCompositeListener;
import com.intellij.cvsSupport2.cvsoperations.cvsMessages.CvsMessagesListener;
import com.intellij.cvsSupport2.cvsoperations.cvsUpdate.UpdateOperation;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDate;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.SimpleRevision;
import com.intellij.cvsSupport2.errorHandling.CannotFindCvsRootException;
import com.intellij.cvsSupport2.javacvsImpl.CvsCommandStopper;
import com.intellij.cvsSupport2.javacvsImpl.FileReadOnlyHandler;
import com.intellij.cvsSupport2.javacvsImpl.io.*;
import com.intellij.cvsSupport2.updateinfo.UpdatedFilesProcessor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.LineTokenizer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.ThreeState;
import com.intellij.util.WaitFor;
import com.intellij.vcsUtil.VcsUtil;
import junit.framework.TestResult;
import org.jetbrains.annotations.NotNull;
import org.netbeans.lib.cvsclient.*;
import org.netbeans.lib.cvsclient.admin.AdminReader;
import org.netbeans.lib.cvsclient.admin.AdminWriter;
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.IConnection;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.file.LocalFileReader;
import org.netbeans.lib.cvsclient.file.LocalFileWriter;
import org.netbeans.lib.cvsclient.progress.IProgressViewer;
import org.netbeans.lib.cvsclient.util.DefaultIgnoreFileFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class BaseCvs2TestCase extends HeavyPlatformTestCase implements CvsEnvironment {
  protected static final String LINE_SEPARATOR = System.getProperty("line.separator");
  protected CvsVcs2 myVcs;
  protected static final String PROJECT_PATH = TestObject.getProjectDirectory().getAbsolutePath();
  protected Collection myCreatedConnections = new ArrayList();
  protected static final String TEST_FILE_EXTENSION = "txt";
  protected static final TestFile TEST_FILE = TestFile.createInProject("file1." + TEST_FILE_EXTENSION);
  protected static final TestObject TEST_DIRECTORY = new TestDirectory(TestObject.getProjectDirectory(), "directory");
  protected static final TestFile FILE_TO_EXCLUDE = TestFile.createInProject("fileToExclude.txt");
  protected static final TestFile FILE_TO_ADD = TestFile.createInProject("fileToAdd.txt");
  protected static final TestFile FILE_TO_REMOVE = TestFile.createInProject("fileToRemove.txt");
  protected static final TestFile FILE_TO_CHECKIN = TestFile.createInProject("fileCheckin.txt");
  public static final String NEW_CONTENT = "new content";
  protected PServerCvsSettings myConfig;
  protected static final String COMMENT = "log message";
  protected File myAnotherLocation = new File(TestObject.getWorkingFolder(), "AnotherLocation");
  private String myTag;
  protected int myLogins;
  protected UpdateSettings myUpdateSettings = null;
  protected final CvsConfiguration myConfiguration;
  protected Collection<VcsException> myErrors;
  protected Collection<VcsException> myWarnings;
  private LocalFileSystem.WatchRequest myWorkingFolderWatchRequest;

  public BaseCvs2TestCase() {
    myConfiguration = new CvsConfiguration();
  }


  @Override
  public TestResult run() {
    try {
      final TestResult result = new TestResult();
      AdminWriter.WRITE_RELATIVE_PATHS = false;
      run(result);
      AdminWriter.WRITE_RELATIVE_PATHS = true;
      run(result);
      return result;
    }
    finally {
      AdminWriter.WRITE_RELATIVE_PATHS = true;
    }
  }


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final File parentFile = TestObject.getWorkingFolder().getParentFile();
    myWorkingFolderWatchRequest = LocalFileSystem.getInstance().addRootToWatch(parentFile.getCanonicalPath(), true);

    CvsEntriesManager.getInstance().activate();

    CvsApplicationLevelConfiguration.getInstance().CHECKOUT_PRUNE_EMPTY_DIRECTORIES = false;

    myAnotherLocation.mkdir();

    PServerLoginProvider.registerPasswordProvider(new PServerLoginProvider() {
      @Override
      public String getScrambledPasswordForCvsRoot(String cvsroot) {
        return getPassword(cvsroot);
      }

      @Override
      public CvsLoginWorker getLoginWorker(Project project, PServerCvsSettings pServerCvsSettings) {
        return new CvsLoginWorker() {
          @Override
          public boolean promptForPassword() {
            return true;
          }

          @Override
          public ThreeState silentLogin(boolean forceCheck) {
            ++ myLogins;
            return ThreeState.YES;
          }

          @Override
          public void goOffline() {
          }
        };
      }
    });

    initTestConfiguration();

    myVcs = new CvsVcs2(myProject);

    Thread.sleep(1000);

    deleteRepository();
    deleteProjectDirectory();

    createRepository();
    createProjectDirectory();
//    ((StartupManagerImpl) StartupManager.getInstance(myProject)).runStartupActivities();

    final ProjectLevelVcsManagerEx manager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);
    manager.getConfirmation(VcsConfiguration.StandardConfirmation.ADD).setValue(VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);
    manager.getConfirmation(VcsConfiguration.StandardConfirmation.REMOVE).setValue(VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);

    //createFileInCvsRoot("rcstemplate", "PR:\n");
    appendStringsToFileInCvsRoot("rcsinfo", new String[]{"ALL $CVSROOT/CVSROOT/rcstemplate"});

    TestObject.getProjectDirectory().mkdirs();

    PsiTestUtil.addContentRoot(myModule, LocalFileSystem.getInstance().findFileByIoFile(TestObject.getProjectDirectory()));

    //((ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(myProject)).registerVcs(CvsVcs2.getInstance(myProject));

    //((ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(myProject)).initialize();
    ProjectLevelVcsManager.getInstance(myProject).setDirectoryMapping("", CvsVcs2.getInstance(myProject).getName());
  }

  @Override
  protected void tearDown() throws Exception {
    ((ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(myProject)).unregisterVcs(CvsVcs2.getInstance(myProject));
    myAnotherLocation.delete();
    CvsEntriesManager.getInstance().clearAll();

    new WaitFor(10000) {
      @Override
      protected boolean condition() {
        return ReadThread.READ_THREADS.isEmpty();
      }
    }.assertCompleted(ReadThread.READ_THREADS.toString());

    CvsEntriesManager.getInstance().deactivate();
    if (myWorkingFolderWatchRequest != null) {
      LocalFileSystem.getInstance().removeWatchedRoot(myWorkingFolderWatchRequest);
    }
    myVcs = null;
    myCreatedConnections = null;
    super.tearDown();
  }

  private void initTestConfiguration() {
    CvsRootConfiguration cvsRootConfiguration =
      CvsApplicationLevelConfiguration.createNewConfiguration(CvsApplicationLevelConfiguration.getInstance());
    cvsRootConfiguration.CVS_ROOT = ":pserver:" + TestObject.getUser() + "@127.0.0.1:" + TestObject.getRepositoryPath();
    myConfig = new PServerCvsSettings(cvsRootConfiguration);
    myConfig.HOST = "127.0.0.1";
    myConfig.USER = TestObject.getUser();
    myConfig.REPOSITORY = TestObject.getRepositoryPath();
    myConfig.PASSWORD = getPassword("");
  }

  public void importAndCheckoutProject() throws Exception {
    importProject();
    checkoutProject();
  }

  protected void importProject() throws Exception {
    ImportOperation importOperation = ImportOperation.createTestInstance(TestObject.getProjectDirectory(), this);
    execute(importOperation);
  }

  protected void checkoutProject() {
    checkoutProjectTo(getProjectRoot());
  }

  protected void checkoutProjectTo(File checkoutLocation) {
    checkoutModuleTo(checkoutLocation, getModuleName());
  }

  protected void checkoutModuleTo(File checkoutLocation, String moduleName) {

    deleteDirectory(new File(checkoutLocation, moduleName));
    refreshFileSystem();
    CvsHandler checkoutHandler = CommandCvsHandler.createCheckoutHandler(this, new String[]{moduleName}, checkoutLocation, false, false,
                                                                         null);
    checkoutHandler.login(myProject);
    checkoutHandler.internalRun(myProject, ModalityContextImpl.NON_MODAL, true);
    refreshFileSystem();
  }

  protected void executeCommand(Command command, File base) throws VcsException {
    refreshFileSystem();
    IConnection connection = openConnection();
    try {
      executeCommand(connection, command);
    }
    finally {
      closeConnection(connection);
    }
  }

  private void executeCommand(IConnection connection, Command command) throws VcsException {
    refreshFileSystem();
    final IClientEnvironment clientEnvironment = new ClientEnvironment(connection, new File(PROJECT_PATH), new File(PROJECT_PATH),
                                                                       getCvsRoot(), new LocalFileReader(new SendTextFilePreprocessor()),
                                                                       new LocalFileWriter(
                                                                         new ReceiveTextFilePreprocessor(ReceivedFileProcessor.DEFAULT)),
                                                                       new AdminReader(CvsApplicationLevelConfiguration.getCharset()),
                                                                       new AdminWriter(CodeStyleSettingsManager.getInstance()
                                                                         .getCurrentSettings().getLineSeparator(),
                                                                                       CvsApplicationLevelConfiguration.getCharset()),
                                                                       new DefaultIgnoreFileFilter(), new FileReadOnlyHandler(),
                                                                       CvsApplicationLevelConfiguration.getCharset());
    final EventManager eventManager = new EventManager(CvsApplicationLevelConfiguration.getCharset());
    final IRequestProcessor requestProcessor =
      new RequestProcessor(clientEnvironment, command.getGlobalOptions(), eventManager, new StreamLogger(), new CvsCommandStopper(), -1);

    try {
      connection.open(new StreamLogger());
      command.execute(requestProcessor, eventManager, eventManager, clientEnvironment, new IProgressViewer() {
        @Override
        public void setProgress(double value) {
        }
      });
    }
    catch (CommandException e) {
      throw new VcsException(e);
    }
    catch (AuthenticationException e) {
    }
    finally {
      try {
        connection.close();
      }
      catch (IOException e) {
      }
    }
  }

  private static void closeConnection(IConnection connection) throws VcsException {
    try {
      connection.close();
    }
    catch (IOException e) {
      throw new VcsException(e);
    }
  }

  private IConnection openConnection() throws VcsException {
    IConnection result;
    try {
      result = createConnection(new ReadWriteStatistics());
      result.open(new StreamLogger());
    }
    catch (AuthenticationException e) {
      throw new VcsException(e);
    }
    return result;
  }

  protected static File getProjectRoot() {
    return TestObject.getWorkingFolder();
  }

  @Override
  public IConnection createConnection(ReadWriteStatistics statistics) {
    return myConfig.createConnection(new ReadWriteStatistics());
  }

  protected static String getModuleName() {
    return TestObject.getProjectDirectory().getName();
  }

  protected static void deleteRepository() {
    deleteDirectory(TestObject.getRepositoryDirectory());
  }

  protected static void deleteDirectory(File directory) {
    boolean deleted = FileUtil.delete(directory);
    assertTrue(directory.getAbsolutePath(), deleted);
  }

  protected static void deleteProjectDirectory() {
    deleteDirectory(TestObject.getProjectDirectory());
  }

  protected static void createProjectDirectory() {
    assertTrue(CheckinProjectTest.PROJECT_PATH, TestObject.getProjectDirectory().mkdir());
  }

  protected static void createRepository() throws Exception {
    String[] cmdarray = {"cvs", "-d", TestObject.getRepositoryPath(), "init"};
    final OSProcessHandler process =
      new OSProcessHandler(Runtime.getRuntime().exec(cmdarray), StringUtil.join(cmdarray, " "));
    process.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(@NotNull final ProcessEvent event, @NotNull final Key outputType) {
        System.out.println(event.getText());
      }
    });
    process.startNotify();
    process.waitFor();
    process.destroyProcess();

    if (TestObject.getUser().equals("builduser")) {   // otherwise assume it's the logged in user and no config is required
      createCvsRootConfigFile("passwd", "builduser::LABS\\" + SystemProperties.getUserName());
      createCvsRootConfigFile("admin", "builduser");
    }
  }

  private static void createCvsRootConfigFile(String fileName, String content) throws IOException {
    File f = new File(TestObject.getRepositoryPath(), "CVSROOT\\" + fileName);
    try (FileWriter w = new FileWriter(f)) {
      w.write(content);
    }
  }

  protected void putTestFileToRepository() throws IOException, VcsException {
    BaseCvs2TestCase.TEST_FILE.createInProject();
    BaseCvs2TestCase.TEST_FILE.addToVcs(myVcs);
    commitTransaction();
  }

  protected void commitTransaction() throws VcsException {
    commitTransaction(COMMENT);
  }

  protected void commitTransaction(String comment) throws VcsException {
    refreshFileSystem();
    myVcs.commitTransaction(comment);
  }

  protected void execute(CvsOperation operation) throws Exception {
    refreshFileSystem();
    CvsOperationExecutor executor = new CvsOperationExecutor(myProject, ModalityState.NON_MODAL);
    executor.performActionSync(new CommandCvsHandler("Test Operation", operation), CvsOperationExecutorCallback.EMPTY);
    if (!executor.hasNoErrors()) throw executor.getFirstError();
  }

  protected void appendStringsToFileInCvsRoot(String fileInCvsRoot, String[] stringsToAppend) throws Exception {
    File cvsroot = new File(TestObject.getWorkingFolder(), "CVSROOT");
    final File file = new File(cvsroot, fileInCvsRoot);

    try {
      String moduleName = cvsroot.getName() + "/" + file.getName();

      execute(CheckoutProjectOperation.createTestInstance(this, moduleName, cvsroot.getParentFile()));

      assertTrue("File " + file.getAbsolutePath() + " should be checked out", file.isFile());

      for (String aStringsToAppend : stringsToAppend) {
        appendStringToFile(file, aStringsToAppend);
      }

      ApplicationManager.getApplication().runWriteAction(() -> {
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      });

      CommitFilesOperation commitFilesOperation = new CommitFilesOperation(false);
      commitFilesOperation.addFile(file.getAbsolutePath());


      execute(commitFilesOperation);
    }
    catch (CannotFindCvsRootException ignored) {

    }
    finally {
      FileUtil.delete(cvsroot);
    }

  }

  private static void appendStringToFile(File moduleFile, String s) throws IOException {
    FileUtil.writeToFile(moduleFile, "\n"+s, true);
  }

  @Override
  public String getCvsRootAsString() {
    return ":pserver:" + TestObject.getUser() + "@localhost:" + TestObject.getRepositoryPath() + "\n";
  }

  public static String getPassword(String cvsroot) {
    return TestObject.getScrambledPassword();
  }

  protected static void assertEqualsAsText(String expected, String actual) {
    String expectedNew = replaceNewLines(expected);
    String actualNew = replaceNewLines(actual);
    assertEquals(expectedNew, actualNew);
  }

  private static String replaceNewLines(String expected) {
    StringBuilder buffer = new StringBuilder();
    String[] strings = LineTokenizer.tokenize(expected.toCharArray(), false, false);
    for (int i = 0; i < strings.length; i++) {
      if (i > 0) buffer.append("\n");
      buffer.append(strings[i]);
    }
    return buffer.toString();
  }

  protected void createNewTestFileRevisionWithContent(String fileContent) throws Exception {
    checkoutToAnotherLocation();
    TestFile anotherTestFile = getTestFileIn(myAnotherLocation);
    anotherTestFile.changeContentTo(fileContent);
    myVcs.getStandardOperationsProvider().checkinFile(anotherTestFile.getAbsolutePath(), null, null);
    commitTransaction();
  }

  protected void setIsBinary(String extension) throws Exception {
    appendStringsToFileInCvsRoot("cvswrappers", new String[]{"*." + extension + " -k 'b'"});
  }

  @Override
  public CvsLoginWorker getLoginWorker(Project project) {
    return new CvsLoginWorker() {
      @Override
      public boolean promptForPassword() {
        return true;
      }

      @Override
      public ThreeState silentLogin(boolean forceCheck) {
        ++ myLogins;
        return ThreeState.YES;
      }

      @Override
      public void goOffline() {
      }
    };
  }

  @Override
  public RevisionOrDate getRevisionOrDate() {
    return new SimpleRevision(myTag);
  }

  protected static TestFile getTestFileIn(File anotherLocation) {
    return getFileIn(anotherLocation, TEST_FILE);
  }

  protected static TestFile getFileIn(File anotherLocation, File file) {
    String oldName = file.getAbsolutePath();
    String oldParent = TestObject.getWorkingFolder().getAbsolutePath();
    String newParent = anotherLocation.getAbsolutePath();
    String newName = newParent + oldName.substring(oldParent.length());
    return new TestFile(newName);
  }

  protected void checkoutToAnotherLocation() {
    myAnotherLocation.delete();
    myAnotherLocation.mkdir();
    checkoutProjectTo(myAnotherLocation);
  }

  protected void setWorkingTag(String branchName) {
    myTag = branchName;
  }

  @Override
  public String getRepository() {
    return myConfig.getRepository();
  }

  @Override
  public CvsRoot getCvsRoot() {
    return myConfig.getCvsRoot();
  }

  protected static void refreshFileSystem() {
    try {
      Thread.sleep(300);
    }
    catch (InterruptedException e) {
      LOG.error(e);
    }

    ApplicationManager.getApplication().runWriteAction(() -> LocalFileSystem.getInstance().refresh(false));

  }

  protected UpdateSettings getUpdateSettings(CvsConfiguration configuration) {
    if (myUpdateSettings != null) {
      return myUpdateSettings;
    }
    return new UpdateSettingsOnCvsConfiguration(configuration, false, false);
  }

  @Override
  public boolean isValid() {
    return true;
  }

  protected void addFile(TestObject testFile, KeywordSubstitution ks) throws IOException, VcsException {

    addFile(testFile, ks, COMMENT);
  }

  protected void addFile(TestObject testFile, KeywordSubstitution ks, String message) throws IOException, VcsException {

    refreshFileSystem();

    testFile.createInProject();
    assertFalse(testFile.isInRepository());
    testFile.addToVcs(myVcs, ks);
    assertFalse(testFile.isInRepository());

    refreshFileSystem();

    commitTransaction(message);
    assertTrue(testFile.isInRepository());

    refreshFileSystem();
  }

  protected void addFile(TestObject testFile) throws IOException, VcsException {
    addFile(testFile, null);
  }

  @Override
  public boolean isOffline() {
    return false;
  }

  @Override
  public CommandException processException(CommandException t) {
    return t;
  }

  protected UpdateSettings getUpdateSettings() {
    return getUpdateSettings(myConfiguration);
  }

  protected UpdatedFiles updateProjectDirectory() throws Exception {
    refreshFileSystem();
    UpdatedFiles info = CvsUpdatePolicy.createUpdatedFiles();
    CvsCompositeListener cvsCompositeListener = new CvsCompositeListener();
    cvsCompositeListener.addCvsListener(CvsMessagesListener.STANDARD_OUTPUT);
    cvsCompositeListener.addCvsListener(new UpdatedFilesProcessor(info));

    UpdateOperation operation =
      new UpdateOperation(createArrayOn(TestObject.getProjectDirectory().getVirtualFile()), getUpdateSettings(), myProject);

    CommandCvsHandler handler = new CommandCvsHandler("Update", operation);
    handler.addCvsListener(cvsCompositeListener);

    CvsOperationExecutor executor = new CvsOperationExecutor(myProject);
    executor.performActionSync(handler, CvsOperationExecutorCallback.EMPTY);
    myErrors = executor.getResult().getErrors();
    myWarnings = executor.getResult().getWarnings();

    if (!myErrors.isEmpty()) {
      throw myErrors.iterator().next();
    }

    return info;
  }

  public static FilePath[] createArrayOn(VirtualFile file) {
    return new FilePath[]{VcsUtil.getFilePath(file)};
  }
}
