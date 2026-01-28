// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.utils;

import com.intellij.helidon.HelidonIcons;
import com.intellij.helidon.providers.HelidonRequestMethods;
import com.intellij.helidon.providers.HelidonUrlPathSpecification;
import com.intellij.microservices.jvm.url.UastUrlAttributeUtils;
import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.HttpUrlResolver;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PartiallyKnownString;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.intellij.microservices.url.UrlConstants.HTTP_SCHEMES;

public final class HelidonUrlTargetInfo implements UrlTargetInfo {
  private final String urlDefinition;

  private final SmartPsiElementPointer<PsiElement> myElementPointer;
  private HelidonRequestMethods myType = HelidonRequestMethods.UNKNOWN;
  private String myParentUrl = null;
  private final NotNullLazyValue<UrlPath> myUrlPath = NotNullLazyValue.createValue(() -> {
    return computeUrlPath();
  });

  public static HelidonUrlTargetInfo create(@NotNull String url, @NotNull PsiElement resolveTo) {
    return new HelidonUrlTargetInfo(url, resolveTo);
  }

  public HelidonUrlTargetInfo ofType(HelidonRequestMethods type) {
    myType = type;
    return this;
  }

  @Override
  public @NotNull Set<String> getMethods() {
    if (myType == HelidonRequestMethods.UNKNOWN || myType == HelidonRequestMethods.ANY_OF) return Collections.emptySet();

    return Collections.singleton(myType.name().toUpperCase(Locale.ENGLISH));
  }

  @Override
  public @NotNull String getSource() {
    return UastUrlAttributeUtils.getUastDeclaringLocation(resolveToPsiElement());
  }

  @Override
  public @Nullable PsiElement getDocumentationPsiElement() {
    return UastUrlAttributeUtils.getUastDeclaringDocumentationElement(resolveToPsiElement());
  }

  public HelidonUrlTargetInfo withParentUrl(String parentUrl) {
    myParentUrl = parentUrl;
    return this;
  }

  private HelidonUrlTargetInfo(@NotNull String url, @NotNull PsiElement resolveTo) {
    urlDefinition = url;
    myElementPointer = SmartPointerManager.getInstance(resolveTo.getProject()).createSmartPsiElementPointer(resolveTo);
  }

  private @NotNull UrlPath computeUrlPath() {
    StringBuilder sb = new StringBuilder();
    if (myParentUrl != null) {
      sb.append(myParentUrl);
    }
    if (sb.toString().endsWith("/")) {
      sb.append(StringsKt.removePrefix(urlDefinition, "/"));
    }
    else {
      if (!urlDefinition.startsWith("/")) {
        sb.append("/");
      }
      sb.append(urlDefinition);
    }

    String url = StringsKt.removePrefix(sb.toString(), "/");
    var urlPath = HelidonUrlPathSpecification.INSTANCE.getParser().parseUrlPath(new PartiallyKnownString(url));
    return urlPath.getUrlPath();
  }

  @Override
  public @NotNull List<String> getSchemes() { return HTTP_SCHEMES; }

  @Override
  public @NotNull List<Authority> getAuthorities() {
    return new ArrayList<>(HttpUrlResolver.Companion.getHTTP_AUTHORITY());
  }

  @Override
  public @NotNull UrlPath getPath() {
    return myUrlPath.getValue();
  }

  @Override
  public @NotNull Icon getIcon() {
    return HelidonIcons.Helidon;
  }

  @Override
  public boolean isDeprecated() {
    return UastUrlAttributeUtils.isUastDeclarationDeprecated(resolveToPsiElement());
  }

  public @Nullable String getParentUrl() {
    return myParentUrl;
  }

  public @NotNull String getUrlDefinition() {
    return urlDefinition;
  }

  @Override
  public @Nullable PsiElement resolveToPsiElement() {
    return myElementPointer.getElement();
  }

  public HelidonRequestMethods getType() {
    return myType;
  }
}
