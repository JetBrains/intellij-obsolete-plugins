package com.intellij.lang.javascript.linter.jslint;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents all <a href="http://www.jslint.com/lint.html#options">JSLint option</a>.
 */
public enum JSLintOption {

  BITWISE(Type.BOOLEAN, "Tolerate bitwise operators", "check if bitwise operators should be allowed"),
  BROWSER(Type.BOOLEAN, "Assume a browser", "check if the standard browser globals should be predefined"),
  COUCH(Type.BOOLEAN, "Assume CouchDB", "check if Couch DB globals should be predefined"),
  DEVEL(Type.BOOLEAN, "Assume in development", "check if browser globals that are useful in development should be predefined"),
  ES6(Type.BOOLEAN, "ES6", "check if using the good parts of ECMAScript Sixth Edition, it adds ES6 globals"),
  EVAL(Type.BOOLEAN, "Tolerate eval", "check if eval should be allowed"),
  FOR(Type.BOOLEAN, "Tolerate for statement", "check if for statement should be allowed"),
  MAXERR(Type.INTEGER, JSLintBundle.message("maximum.number.of.errors.text"), "Maximum number of warnings reported"),
  MAXLEN(Type.INTEGER, JSLintBundle.message("maximum.line.length.text"), "Maximum number of characters in a line"),
  MULTIVAR(Type.BOOLEAN, "multiple vars",
           "check if a var, let, or const statement can declare two or more variables in a single statement"),
  NODE(Type.BOOLEAN, "Assume Node.js", "check if Node.js globals should be predefined"),
  GLOBALS(Type.STRING, JSLintBundle.message("globals.text"), "An array of strings, the names of predefined global variables," +
                                                 " or an object whose keys are global variable names, and whose values are booleans" +
                                                 " that determine if each variable is assignable." +
                                                 " predef is used with the option object, but not with the /*jslint */ directive." +
                                                 " You can also use the var statement to declare global variables in a script file."),
  SINGLE(Type.BOOLEAN, "Tolerate single quote strings", "check if ' (single quote) should be allowed to enclose string literals"),
  THIS(Type.BOOLEAN, "Tolerate this", "check if this should be allowed"),
  WHITE(Type.BOOLEAN, "Tolerate whitespace mess", "check if the whitespace rules should be ignored")
  ;
  private final String myOptionName;
  private final Type myType;
  private final String myDescription;
  private final String myMeaning;

  private static final ImmutableMap<String, JSLintOption> OPTION_BY_NAME_MAP;

  static {
    ImmutableMap.Builder<String, JSLintOption> builder = ImmutableMap.builder();
    for (JSLintOption option : values()) {
      builder.put(option.getOptionName(), option);
    }
    OPTION_BY_NAME_MAP = builder.build();
  }

  JSLintOption(@NotNull Type type, @NotNull String description, @NotNull String meaning) {
    myOptionName = StringUtil.toLowerCase(name());
    myType = type;
    myDescription = description;
    myMeaning = meaning;
  }

  @NotNull
  public Type getType() {
    return myType;
  }

  @NotNull
  public String getOptionName() {
    return myOptionName;
  }

  @NotNull
  public String getDescription() {
    return myDescription;
  }

  @NotNull
  public String getMeaning() {
    return myMeaning;
  }

  @Nullable
  public static JSLintOption findByName(@NotNull String optionName) {
    return OPTION_BY_NAME_MAP.get(optionName);
  }

  public enum Type {

    BOOLEAN(Boolean.class) {
      @Override
      public Boolean createObject(@NotNull String valueStr) {
        return Boolean.parseBoolean(valueStr);
      }

      @Override
      public boolean isDefault(@NotNull Object value) {
        return value == Boolean.FALSE;
      }
    },
    STRING(String.class) {
      @Override
      public String createObject(@NotNull String valueStr) {
        return valueStr;
      }

      @Override
      public boolean isDefault(@NotNull Object value) {
        return "".equals(value);
      }
    },
    INTEGER(Integer.class) {
      @Override
      public Integer createObject(@NotNull String valueStr) {
        try {
          return Integer.parseInt(valueStr);
        } catch (Exception e) {
          return null;
        }
      }

      @Override
      public boolean isDefault(@NotNull Object value) {
        return false;
      }

      @Override
      public boolean isProperValue(@Nullable Object obj) {
        if (obj == null) {
          return true;
        }
        if (obj instanceof Integer || obj instanceof Long) {
          Number n = (Number) obj;
          return n.intValue() >= 0;
        }
        return false;
      }
    };

    private final Class<?> myClass;

    Type(Class<?> clazz) {
      myClass = clazz;
    }

    @Nullable
    public abstract Object createObject(@NotNull String valueStr);

    public abstract boolean isDefault(@NotNull Object value);

    public boolean isProperValue(@Nullable Object obj) {
      return obj == null || myClass.isInstance(obj);
    }
  }

}
