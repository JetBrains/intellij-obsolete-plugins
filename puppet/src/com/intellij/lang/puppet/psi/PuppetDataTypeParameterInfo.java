package com.intellij.lang.puppet.psi;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes puppet DataType parameter information
 * todo: add suitable types: name, capitalized name and string
 */
public final class PuppetDataTypeParameterInfo {
  private static final int CLASS = 1;
  private static final int DATA_TYPE = 2;
  private static final int RESOURCE_TYPE = 4;
  private static final int RESOURCE_INSTANCE = 8;

  private final int myFlags;
  private @Nullable String myResourceDataType;

  private PuppetDataTypeParameterInfo(int flags) {
    myFlags = flags;
  }

  private PuppetDataTypeParameterInfo(@NotNull String resourceDataType) {
    this(RESOURCE_INSTANCE);
    myResourceDataType = resourceDataType;
  }

  public boolean isClass() {
    return (myFlags & CLASS) == CLASS;
  }

  public boolean isDataType() {
    return (myFlags & DATA_TYPE) == DATA_TYPE;
  }

  public boolean isResourceType() {
    return (myFlags & RESOURCE_TYPE) == RESOURCE_TYPE;
  }

  public boolean isResourceInstance() {
    return (myFlags & RESOURCE_INSTANCE) == RESOURCE_INSTANCE;
  }

  public @Nullable String getResourceDataType() {
    return myResourceDataType;
  }

  public static @NotNull PuppetDataTypeParameterInfo forClass() {
    return new PuppetDataTypeParameterInfo(CLASS);
  }

  public static @NotNull PuppetDataTypeParameterInfo forDataType() {
    return new PuppetDataTypeParameterInfo(DATA_TYPE);
  }

  public static @NotNull PuppetDataTypeParameterInfo forResourceType() {
    return new PuppetDataTypeParameterInfo(RESOURCE_TYPE);
  }

  public static @NotNull PuppetDataTypeParameterInfo forAnyType() {
    return new PuppetDataTypeParameterInfo(RESOURCE_TYPE | DATA_TYPE);
  }

  @Contract("null -> null")
  public static PuppetDataTypeParameterInfo forResourceInstance(@Nullable String dataTypeName) {
    return dataTypeName == null ? null : new PuppetDataTypeParameterInfo(dataTypeName);
  }
}

