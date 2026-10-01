package com.intellij.aiplayground.tests.chat

import com.intellij.aiplayground.tests.annotations.GherkinTest
import com.intellij.aiplayground.tests.gherkin.GherkinTestBase
import com.intellij.aiplayground.tests.gherkin.GherkinTestRunner
import org.junit.Ignore
import org.junit.runner.RunWith

@Ignore("AT-3959")
@RunWith(GherkinTestRunner::class)
@GherkinTest("features/chat.feature")
class ChatFeatureTest : GherkinTestBase() {

  override fun setUp() {
    super.setUp()
    registerChatSteps()
  }

  private fun registerChatSteps() {
    // Background steps

  }

  override fun tearDown() {
    super.tearDown()
  }
} 