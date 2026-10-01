package com.intellij.aiplayground.models.settings

import com.intellij.aiplayground.models.CredentialStorage
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

/**
 * Implementation of CredentialStorage using IntelliJ's PasswordSafe
 */
class PasswordSafeCredentialStorage() : CredentialStorage {

  private val SERVICE_NAME = "AI.Playground"

  private fun createCredentialAttributes(key: String): CredentialAttributes {
    return CredentialAttributes("$SERVICE_NAME.$key", null)
  }

  override fun storeCredential(key: String, value: String) {
    val credentialAttributes = createCredentialAttributes(key)
    val credentials = Credentials(null, value)
    PasswordSafe.instance.set(credentialAttributes, credentials)
  }

  override fun retrieveCredential(key: String): String? {
    val credentialAttributes = createCredentialAttributes(key)
    return PasswordSafe.instance.getPassword(credentialAttributes)
  }

  override fun removeCredential(key: String): Boolean {
    val credentialAttributes = createCredentialAttributes(key)
    PasswordSafe.instance.set(credentialAttributes, null)
    return true
  }
}