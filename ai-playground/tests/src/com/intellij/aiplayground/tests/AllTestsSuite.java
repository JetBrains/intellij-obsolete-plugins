package com.intellij.aiplayground.tests;

import com.intellij.aiplayground.tests.chat.ChatFeatureTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ChatFeatureTest.class,
})
public class AllTestsSuite {
}
