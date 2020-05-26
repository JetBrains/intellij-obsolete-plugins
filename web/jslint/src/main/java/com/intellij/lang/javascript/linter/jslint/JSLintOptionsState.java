package com.intellij.lang.javascript.linter.jslint;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Simonchik
 */
public class JSLintOptionsState {

  private final Map<JSLintOption, Object> myValueByOptionMap;

  private JSLintOptionsState(@NotNull Map<JSLintOption, Object> valueByOptionMap) {
    myValueByOptionMap = Maps.newEnumMap(valueByOptionMap);
  }

  @NotNull
  public Set<JSLintOption> getOptions() {
    return myValueByOptionMap.keySet();
  }

  @Nullable
  public Object getValue(@NotNull JSLintOption option) {
    return myValueByOptionMap.get(option);
  }

  public static class Builder {
    private final EnumMap<JSLintOption, Object> myValueByOptionMap;

    public Builder() {
      myValueByOptionMap = Maps.newEnumMap(JSLintOption.class);
    }

    public Builder put(@NotNull JSLintOption option, @NotNull Object value) {
      JSLintOption.Type optionType = option.getType();
      if (!optionType.isProperValue(value)) {
        String message = String.format("Attempt to set value of JSLint option '%s' to '%s' (value class is %s)!",
                                       option.getOptionName(), value, value.getClass());
        throw new RuntimeException(message);
      }
      Object prevValue = myValueByOptionMap.get(option);
      if (prevValue != null || !optionType.isDefault(value)) {
        myValueByOptionMap.put(option, value);
      }
      return this;
    }

    @NotNull
    public JSLintOptionsState build() {
      return new JSLintOptionsState(myValueByOptionMap);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JSLintOptionsState that = (JSLintOptionsState)o;
    return myValueByOptionMap.equals(that.myValueByOptionMap);
  }

  @Override
  public int hashCode() {
    return myValueByOptionMap.hashCode();
  }

  @Override
  public String toString() {
    return myValueByOptionMap.toString();
  }
}
