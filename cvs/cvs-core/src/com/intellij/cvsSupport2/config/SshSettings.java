/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.cvsSupport2.config;

import com.intellij.cvsSupport2.connections.ssh.SshTypesToUse;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.Converter;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * author: lesya
 */

public class SshSettings implements PersistentStateComponent<SshSettings>, Cloneable {

  private static final Logger LOG = Logger.getInstance(SshSettings.class);

  public boolean USE_PPK = false;
  public String PATH_TO_PPK = "";

  @Attribute(converter = SshTypesToUseConverter.class)
  public SshTypesToUse SSH_TYPE = SshTypesToUse.ALLOW_BOTH;

  private static class SshTypesToUseConverter extends Converter<SshTypesToUse> {

    @Override
    public @Nullable SshTypesToUse fromString(@NotNull String value) {
      return SshTypesToUse.fromName(value);
    }

    @Override
    public @Nullable String toString(@NotNull SshTypesToUse value) {
      return value.toString();
    }
  }

  @Override
  public @Nullable SshSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull SshSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Override
  public SshSettings clone() {
    try {
      return (SshSettings)super.clone();
    }
    catch (CloneNotSupportedException e) {
      LOG.error(e);
      return new SshSettings();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SshSettings that = (SshSettings)o;

    if (!SSH_TYPE.equals(that.SSH_TYPE)) return false;
    if (USE_PPK != that.USE_PPK) return false;
    if (!USE_PPK) {
      return true;
    }
    return PATH_TO_PPK.equals(that.PATH_TO_PPK);
  }

  @Override
  public int hashCode() {
    int result = (USE_PPK ? 1 : 0);
    if (USE_PPK) {
      result = 31 * result + PATH_TO_PPK.hashCode();
    }
    result = 31 * result + SSH_TYPE.hashCode();
    return result;
  }
}
