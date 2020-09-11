package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.common.UpdatedFilesManager;
import com.intellij.cvsSupport2.cvsoperations.cvsErrors.ErrorMessagesProcessor;
import com.intellij.cvsSupport2.cvsoperations.cvsMessages.CvsMessagesTranslator;
import com.intellij.openapi.vcs.VcsException;
import junit.framework.TestCase;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


public class ErrorMessagesProcessorTest extends TestCase {

  private final ErrorMessagesProcessor myErrorMessagesProcessor = new ErrorMessagesProcessor();
  private final CvsMessagesTranslator myCvsMessagesTranslator = new CvsMessagesTranslator(myErrorMessagesProcessor,
                                                                                    null,
                                                                                    new UpdatedFilesManager(),
                                                                                    "");
  private static final String ANY_MESSAGE = "Any message";
  private static final String ERROR_MESSAGE = "cvs [server aborted]" + " bla-bla-bla";

  public ErrorMessagesProcessorTest(String s) {
    super(s);
  }

  public void testNoMessages() {
    assertTrue(getErrors().isEmpty());
  }

  public void testErrorMessage() {
    addMessage(ANY_MESSAGE, true);
    addMessage(ERROR_MESSAGE, true);
    assertEquals(1, getErrorsSize());
    checkError();
  }

  public void testSimpleMessage() {
    addMessage(ANY_MESSAGE, true);
    assertTrue(getErrors().isEmpty());
  }

  private void checkError() {
    assertEquals(Arrays.asList(ERROR_MESSAGE), Arrays.asList(getLastError()));
  }

  private String[] getLastError() {
    VcsException lastException = (VcsException)getErrors().get(getErrors().size() - 1);
    return lastException.getMessages();
  }

  private int getErrorsSize() {
    return getErrors().size();
  }

  private void addMessage(String message, boolean error) {
    myCvsMessagesTranslator.messageSent(message, message.getBytes(StandardCharsets.UTF_8), error, false);
  }


  private List getErrors() {
    return myErrorMessagesProcessor.getErrors();
  }
}
