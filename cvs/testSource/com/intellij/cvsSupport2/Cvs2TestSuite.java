package com.intellij.cvsSupport2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
  CheckoutFileOrDirectoryTest.class,
  FileContentTest.class,
  CheckinProjectTest.class,
  CheckoutProjectTest.class,
  //DeletedDirectoriesTestCase.class,
  ConfigurationTest.class,
  ImportTest.class,
  ErrorMessagesProcessorTest.class,
  UpdateTest.class,
  MergingTest.class,
  RcsDiffErrorTest.class,
  CvsRootFormatterTest.class,
  CvsStatusTest.class,
  EditFileTest.class,
  WorkingWithBinaryFilesTest.class,
  WorkingWithBranchesTest.class,
  //PruneEmptyDirectoriesTest.class,
  HistoryForEmptyFileTest.class,
  HistoryTest.class,
  //UpdateInfoTest.class,
  SimpleStringPatternTest.class,
  MakeNewFileReadOnlyTest.class,
  ListDirectoriesTest.class,
  EntriesReadWriteTest.class,
  RefactoringSupportTest.class,
  LoginTest.class,
  LineSeparatorsTest.class,
  AnnotationCommandTest.class,
  WorkingWithTemplatesTest.class,
  EntriesStaticTest.class,
  DoNotUpdatetModulesNotUnderCvsTest.class,
})
public class Cvs2TestSuite {
}
