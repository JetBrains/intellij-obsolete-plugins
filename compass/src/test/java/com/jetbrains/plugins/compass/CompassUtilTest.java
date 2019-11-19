package com.jetbrains.plugins.compass;

import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;

public class CompassUtilTest extends UsefulTestCase {
  public void testParsingCompassImportsOutput() {
    CommonProcessors.CollectProcessor<String> processor = new CommonProcessors.CollectProcessor<>();
    CompassUtil.processCompassImportsOutput("-I /test/path", processor);
    assertEquals(ContainerUtil.newArrayList("/test/path"), processor.getResults());

    processor = new CommonProcessors.CollectProcessor<>();
    CompassUtil.processCompassImportsOutput("-I /test/path -I", processor);
    assertEquals(ContainerUtil.newArrayList("/test/path"), processor.getResults());

    processor = new CommonProcessors.CollectProcessor<>();
    CompassUtil.processCompassImportsOutput("-S -I /test/path", processor);
    assertEquals(ContainerUtil.newArrayList("/test/path"), processor.getResults());

    processor = new CommonProcessors.CollectProcessor<>();
    CompassUtil.processCompassImportsOutput("", processor);
    assertEquals(new ArrayList<String>(), processor.getResults());

    processor = new CommonProcessors.CollectProcessor<>();
    CompassUtil.processCompassImportsOutput("-I", processor);
    assertEquals(new ArrayList<String>(), processor.getResults());
  }
}
