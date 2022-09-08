package com.intellij.seam.impl.model.metadata;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.seam.model.metadata.SeamEventType;
import com.intellij.seam.model.metadata.SeamEventTypeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

final class SeamEventTypeFactoryImpl extends SeamEventTypeFactory {
  private final Map<String, SeamEventType> myEvents = new HashMap<>();

  private final PsiFile myDummyFile;

  SeamEventTypeFactoryImpl(final Project project) {
    myDummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.java", "");
  }

  @Override
  @NotNull
  public SeamEventType getOrCreateEventType(final String eventType) {
    if (!myEvents.containsKey(eventType)) {
       myEvents.put(eventType, new SeamEventType(eventType, myDummyFile));
    }
    return myEvents.get(eventType);
  }

  @Override
  public void dispose() {
      myEvents.clear();
  }
}
