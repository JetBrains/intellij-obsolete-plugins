package com.intellij.frameworks.jboss.seam.rename;

public class SeamObserversRenameTest extends SeamRenameTestCase {

  public void testRenameObserver() {
    myFixture.copyFileToProject("ObserverOwner.java");
    myFixture.copyFileToProject("ObserverEventRaiser.java");
    myFixture.copyFileToProject("ObserverRaiseEventAnno.java");

    myFixture.testRename("ObserverOwner.java", "ObserverOwner_after.java", "new_event_type", "ObserverEventRaiser.java");
    myFixture.checkResultByFile("ObserverEventRaiser.java", "ObserverEventRaiser_after.java", true);
    myFixture.checkResultByFile("ObserverRaiseEventAnno.java", "ObserverRaiseEventAnno_after.java", true);
  }
}
