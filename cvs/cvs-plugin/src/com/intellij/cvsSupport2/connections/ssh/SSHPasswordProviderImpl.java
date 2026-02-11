/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.cvsSupport2.connections.ssh;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.netbeans.lib.cvsclient.connection.PServerPasswordScrambler;

import java.util.Map;

/**
 * author: lesya
 */
public class SSHPasswordProviderImpl implements SSHPasswordProvider, NamedComponent {

  private static final PasswordSafe PASSWORD_SAFE = PasswordSafe.getInstance();
  private static final PServerPasswordScrambler PASSWORD_SCRAMBLER = PServerPasswordScrambler.getInstance();
  private static final String SUBSYSTEM_SSH = "SSHPasswordProvider";
  private static final String SUBSYSTEM_SSH_PPK = "SSHPasswordProviderPPK";

  private final Map<String, Credentials> myCvsRootToPasswordMap = new HashMap<>();

  private final Map<String, Credentials> myCvsRootToPPKPasswordMap = new HashMap<>();

  private final Object myLock = new Object();

  public static SSHPasswordProviderImpl getInstance() {
    return ApplicationManager.getApplication().getService(SSHPasswordProviderImpl.class);
  }

  @Override
  @NotNull
  public String getComponentName() {
    return SUBSYSTEM_SSH;
  }

  @Override
  @Nullable
  public String getPasswordForCvsRoot(String cvsRoot) {
    return getPasswordFor(SUBSYSTEM_SSH, cvsRoot, myCvsRootToPasswordMap);
  }

  @Override
  @Nullable
  public String getPPKPasswordForCvsRoot(String cvsRoot) {
    return getPasswordFor(SUBSYSTEM_SSH_PPK, cvsRoot, myCvsRootToPPKPasswordMap);
  }

  public void storePasswordForCvsRoot(String cvsRoot, String password, boolean storeInWorkspace) {
    storePasswordFor(SUBSYSTEM_SSH, cvsRoot, password, storeInWorkspace, myCvsRootToPasswordMap);
  }

  public void storePPKPasswordForCvsRoot(String cvsRoot, String password, boolean storeInWorkspace) {
    storePasswordFor(SUBSYSTEM_SSH_PPK, cvsRoot, password, storeInWorkspace, myCvsRootToPPKPasswordMap);
  }

  public void removePasswordFor(String cvsRoot) {
    removePasswordFor(SUBSYSTEM_SSH, cvsRoot, myCvsRootToPasswordMap);
  }

  public void removePPKPasswordFor(String cvsRoot) {
    removePasswordFor(SUBSYSTEM_SSH_PPK, cvsRoot, myCvsRootToPPKPasswordMap);
  }

  //

  private String getPasswordFor(String subSystem, String key, Map<String, Credentials> passwordMap) {
    synchronized (myLock) {
      Credentials credentials;
      if (passwordMap.containsKey(key)) {
        credentials = passwordMap.get(key);
      } else {
        credentials = retrieveCredentials(
                createCredentialAttributes(subSystem, key)
        );
        if (credentials != null) {
          passwordMap.put(key, credentials);
        }
      }
      if (credentials != null) {
        return unscramblePassword(credentials.getPasswordAsString());
      }
      return null;
    }
  }

  private void storePasswordFor(String subSystem, String key, String password, boolean storeInWorkspace, Map<String, Credentials> passwordMap) {
    synchronized (myLock) {
      final Credentials credentials = new Credentials(key, scramblePassword(password));
      if (storeInWorkspace) {
        storeCredentials(
                createCredentialAttributes(subSystem, key),
                credentials
        );
      }
      passwordMap.put(key, credentials);
    }
  }

  private void removePasswordFor(String subSystem, String key, Map<String, Credentials> passwordMap) {
    synchronized (myLock) {
      passwordMap.remove(key);
      removeCredentials(
              createCredentialAttributes(subSystem, key)
      );
    }
  }

  private static String scramblePassword(String password) {
    return PASSWORD_SCRAMBLER.scramble(password);
  }

  private static String unscramblePassword(String password) {
    return PASSWORD_SCRAMBLER.unscramble(password);
  }

  private static CredentialAttributes createCredentialAttributes(String subSystem, String key) {
    return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName(subSystem, key)
    );
  }

  private static @Nullable Credentials retrieveCredentials(CredentialAttributes credentialAttributes) {
    return PASSWORD_SAFE.get(credentialAttributes);
  }

  private static void storeCredentials(CredentialAttributes credentialAttributes, Credentials credentials) {
    PASSWORD_SAFE.set(credentialAttributes, credentials);
  }

  private static void removeCredentials(CredentialAttributes credentialAttributes) {
    final Credentials credentials = retrieveCredentials(credentialAttributes);
    if (credentials != null) {
      PASSWORD_SAFE.set(credentialAttributes, null);
    }
  }

}
