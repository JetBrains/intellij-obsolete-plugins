package com.intellij.aiplayground.settings.tests

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.openapi.components.service
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@TestApplication
class CredentialStorageTests {

  @Test
  fun testMySimpleTest() {
    val llmProvider = LlmProvider(
      id = LlmProviderId("myId"),
      displayName = "myDisplayName"
    )
    val service = service<ApplicationSettingsManagerService>()
    service.storeProviderApiKey(llmProvider.id, "abacaba")
    val apiKey = service.getProviderApiKey(llmProvider.id)
    Assertions.assertEquals(apiKey, "abacaba")
  }
}