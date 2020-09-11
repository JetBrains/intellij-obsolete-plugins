package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsstatuses.CvsChangeProvider;
import com.intellij.openapi.util.Clock;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.charset.StandardCharsets;

public class CvsChangeProviderTest extends CvsTestsWorkingWithImportedProject {
  private CvsChangeProvider provider;
  private VirtualFile f;
  private long UP_TO_DATE_TIME;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addFile(TEST_FILE);
    checkoutToAnotherLocation();

    provider = (CvsChangeProvider)myVcs.getChangeProvider();
    f = TEST_FILE.getVirtualFile();
    UP_TO_DATE_TIME = provider.getUpToDateTimeForFile(f);
  }

  @Override
  public void tearDown() throws Exception {
    Clock.reset();
    provider = null;
    super.tearDown();
  }

  public void testUpToDateContent() throws Exception {
    Clock.setTime(UP_TO_DATE_TIME + 10000);
    f.setBinaryContent("new content".getBytes(StandardCharsets.UTF_8));
    assertEquals(TestFile.INITIAL_CONTENT, new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));
  }

  public void testUpToDateContentWithEpsilonInThreeSeconds() throws Exception {
    Clock.setTime(UP_TO_DATE_TIME + 1000);
    f.setBinaryContent("one".getBytes(StandardCharsets.UTF_8));
    assertEquals("one", new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));

    Clock.setTime(UP_TO_DATE_TIME + 2000);
    f.setBinaryContent("two".getBytes(StandardCharsets.UTF_8));
    assertEquals("two", new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));

    Clock.setTime(UP_TO_DATE_TIME + 2500);
    f.setBinaryContent("three".getBytes(StandardCharsets.UTF_8));
    assertEquals("three", new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));

    Clock.setTime(UP_TO_DATE_TIME + 3500);
    f.setBinaryContent("four".getBytes(StandardCharsets.UTF_8));
    assertEquals("three", new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));
  }

  public void testUpToDateContentWithTimeDifferentInOneHour() throws Exception {
    // difference between server and client time due to day light saving
    Clock.setTime(UP_TO_DATE_TIME + 60 * 60 * 1000);
    f.setBinaryContent("new content".getBytes(StandardCharsets.UTF_8));
    assertEquals("new content", new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));
  }

  public void testUpToDateContentForNewlyFileIsNull() throws Exception {
    TestFile testFile = TestFile.createInProject("unknown." + TEST_FILE_EXTENSION);
    addFile(testFile);

    f = testFile.getVirtualFile();
    Clock.setTime(f.getTimeStamp() + 10000);
    f.setBinaryContent("new content".getBytes(StandardCharsets.UTF_8));

    assertEquals(TestFile.INITIAL_CONTENT, new String(provider.getLastUpToDateContentFor(f), StandardCharsets.UTF_8));
  }
}