package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.json.psi.*;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PairConsumer;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Irina.Chernushina on 5/4/2015.
 */
public class JscsConfigFileAnnotator implements Annotator {
  interface ProblemSink {
    void createProblem(@NotNull PsiElement elt, @NotNull String message);
  }

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof JsonProperty) ||
        !(element.getParent() instanceof JsonObject) ||
        checkFile(element.getParent().getParent()) == null) {
      return;
    }

    ProblemSink problemSink = (elt, message) -> {
      holder.newAnnotation(HighlightSeverity.WARNING, message).range(elt).needsUpdateOnTyping().create();
    };
    THashSet<JsonProperty> visited = new THashSet<>();
    checkProperty(problemSink, new JscsOptionsChecker(visited), (JsonProperty)element, visited);
  }

  static void annotate(@NotNull JsonFile file, @NotNull ProblemSink problemSink) {
    final JsonFile jsonFile = checkFile(file);
    if (jsonFile != null) {
      annotateMe(jsonFile, problemSink, new THashSet<>());
    }
  }

  private static JsonFile checkFile(PsiElement element) {
    final JsonFile file = ObjectUtils.tryCast(element, JsonFile.class);
    if (file != null) {
      final VirtualFile vFile = file.getViewProvider().getVirtualFile();
      if (FileTypeRegistry.getInstance().isFileOfType(vFile, JscsConfigFileType.INSTANCE)) {
        return file;
      }
    }
    return null;
  }

  private static void annotateMe(@NotNull JsonFile file, @NotNull ProblemSink problemSink, @NotNull Set<? super JsonProperty> visited) {
    for (PsiElement element : file.getChildren()) {
      JsonObject objectLiteralExpression = ObjectUtils.tryCast(element, JsonObject.class);
      if (objectLiteralExpression != null) {
        iterateOptions(objectLiteralExpression, problemSink, new JscsOptionsChecker(visited), visited);
        break;
      }
    }
  }

  private interface OptionChecker<T> {
    T isValidName(@NotNull String name);
    void processOption(@NotNull JsonProperty property, @NotNull T parsedOption, @NotNull ProblemSink holder);
    @NotNull
    String getOptionClassName();
  }

  private static <T> void iterateOptions(@NotNull JsonObject jsonObject,
                                         @NotNull ProblemSink holder,
                                         @NotNull OptionChecker<T> checker,
                                         @NotNull Set<? super JsonProperty> visited) {
    final List<JsonProperty> properties = jsonObject.getPropertyList();
    for (JsonProperty property : properties) {
      checkProperty(holder, checker, property, visited);
    }
  }

  private static <T> void checkProperty(@NotNull ProblemSink holder,
                                        @NotNull OptionChecker<T> checker,
                                        @NotNull JsonProperty property,
                                        @NotNull Set<? super JsonProperty> visited) {
    if (!visited.add(property)) return;
    final String name = property.getName();
    final T parsedOption = checker.isValidName(name);
    if (parsedOption != null) {
      checker.processOption(property, parsedOption, holder);
    }
    else {
      createWarningAnnotation(holder, property.getNameElement(), "Unknown " + checker.getOptionClassName() + " option name");
    }
  }

  private static class JscsOptionsChecker implements OptionChecker<JscsOption> {
    private final Set<? super JsonProperty> visited;

    private JscsOptionsChecker(@NotNull Set<? super JsonProperty> visited) {
      this.visited = visited;
    }

    @Override
    public JscsOption isValidName(@NotNull String name) {
      return JscsOption.safeValueOf(name);
    }

    @NotNull
    @Override
    public String getOptionClassName() {
      return "JSCS";
    }

    @Override
    public void processOption(@NotNull JsonProperty property, @NotNull JscsOption option, @NotNull ProblemSink holder) {
      final JsonValue value = property.getValue();
      if (value == null || ObjectUtils.tryCast(value, JsonNullLiteral.class) != null) return;

      if (option.canBe(ValueType.bool) && isBooleanLiteral(option.otherTypes(null), holder, value)) {
        // everything is done inside if
      } else if (option.canBe(ValueType.true_or_false) && isTrueOrFalse(value)) {
        // everything is done inside if
      }
      else {
        final Map<ValueType, JscsOptionDescriptor.ValueDescription> types = option.getDescriptor().getTypes();
        if (customValidation(types, option.otherTypes(null), value, holder)) return;

        if (option.canBe(ValueType.obj) && isObjectLiteral(types, holder, value, option.name(), visited)) {
          // everything is done inside if
        }
        else if (option.canBe(ValueType.str) && isStringLiteral(types, option.getTypesList(), holder, value)) {
          // everything is done inside if
        }
        else if (option.canBe(ValueType.array) && isArray(types, holder, value, getReplacementValuesMessage(option))) {
          // everything is done inside if
        }
        else if (option.canBe(ValueType.num_int) && isIntLiteral(option.otherTypes(null), holder, value)) {
          // everything is done inside if
        }
        else {
          createExpectedTypesAnnotation(option.otherTypes(null), holder, value);
        }
      }
    }

    private static final Map<Class<? extends JsonValue>, ValueType> typesCombinations = new HashMap<>();
    static {
      typesCombinations.put(JsonStringLiteral.class, ValueType.str);
      typesCombinations.put(JsonNumberLiteral.class, ValueType.num_int);
      typesCombinations.put(JsonArray.class, ValueType.array);
      typesCombinations.put(JsonObject.class, ValueType.obj);
    }

    private static boolean customValidation(@NotNull Map<ValueType, JscsOptionDescriptor.ValueDescription> types,
                                            @NotNull List<? extends ValueType> typesList,
                                            @NotNull JsonValue value,
                                            @NotNull ProblemSink holder) {
      ValueType foundType = null;
      for (Map.Entry<Class<? extends JsonValue>, ValueType> entry : typesCombinations.entrySet()) {
        final Class<? extends JsonValue> aClass = entry.getKey();
        if (aClass.isAssignableFrom(value.getClass())) {
          if (! types.containsKey(entry.getValue())) return createExpectedTypesAnnotation(typesList, holder, value);
          foundType = entry.getValue();
        }
      }
      if (foundType == null) {
        return createExpectedTypesAnnotation(typesList, holder, value);
      }

      final JscsOptionDescriptor.ValueDescription description = types.get(foundType);
      if (description instanceof JscsOptionDescriptor.CustomValidation) {
        final String checkResult = ((JscsOptionDescriptor.CustomValidation)description).check(value);
        if (checkResult != null) {
          createWarningAnnotation(holder, value, checkResult);
        }
        return true;
      }

      return false;
    }

    private static boolean isObjectLiteral(@NotNull Map<ValueType, JscsOptionDescriptor.ValueDescription> map,
                                           @NotNull ProblemSink holder,
                                           @NotNull JsonValue value,
                                           @NotNull String parentName,
                                           @NotNull Set<? super JsonProperty> visited) {
      final JsonObject jsonObj = ObjectUtils.tryCast(value, JsonObject.class);
      if (jsonObj == null) return false;
      final JscsOptionDescriptor.ValueDescription valDescriptor = map.get(ValueType.obj);
      if (valDescriptor.equals(JscsOptionDescriptor.ValueDescription.NO_VALIDATION)) return true;
      final JscsOptionDescriptor.ValuesObject descriptor = (JscsOptionDescriptor.ValuesObject)valDescriptor;
      final String mandatory = descriptor.getMandatory();
      if (mandatory != null) {
        if (jsonObj.findProperty(mandatory) == null) {
          createWarningAnnotation(holder, value, "Required property '" + mandatory + "' should be defined");
        }
      }
      if (descriptor.isMustNotBeEmpty()) {
        if (jsonObj.getPropertyList().isEmpty()) {
          createWarningAnnotation(holder, value, "At least one of the properties should be defined");
        }
      }
      iterateOptions(jsonObj, holder, new OptionChecker<Object>() {
        @Override
        public Object isValidName(@NotNull String name) {
          return descriptor.getFields().containsKey(name.trim()) ? true : null;
        }

        @Override
        public void processOption(@NotNull JsonProperty property, @NotNull Object parsedOption, @NotNull ProblemSink holder) {
          final String trim = property.getName().trim();
          final JscsOptionDescriptor field = descriptor.getFields().get(trim);
          if (field == null) return;
          JsonValue value = property.getValue();
          if (value == null) return;

          final List<ValueType> types = new ArrayList<>(field.getTypes().keySet());
          if (field.canBe(ValueType.bool) && isBooleanLiteral(types, holder, value)) return;
          if (field.canBe(ValueType.true_or_false) && isTrueOrFalse(value)) return;

          if (customValidation(field.getTypes(), types, value, holder)) return;
          if (field.canBe(ValueType.num_int) && isIntLiteral(types, holder, value)) return;
          if (field.canBe(ValueType.str) && isStringLiteral(field.getTypes(), types, holder, value)) return;
          if (field.canBe(ValueType.array) && isArray(field.getTypes(), holder, value, null)) return;
          if (field.canBe(ValueType.obj) && isObjectLiteral(field.getTypes(), holder, value, trim, visited)) return;
          createExpectedTypesAnnotation(types, holder, value);
        }

        @NotNull
        @Override
        public String getOptionClassName() {
          return parentName;
        }
      }, visited);
      return true;
    }

    private static boolean isIntLiteral(@NotNull List<? extends ValueType> types, @NotNull ProblemSink holder, @NotNull JsonValue value) {
      final JsonNumberLiteral literal = ObjectUtils.tryCast(value, JsonNumberLiteral.class);
      if (literal == null) return false;

      try {
        Integer.parseInt(StringUtil.unquoteString(literal.getText()));
      }
      catch (NumberFormatException e) {
        new AnnotationTextBuilder(holder, value).withTypes(types).build();
      }
      return true;
    }

    private static boolean isTrueOrFalse(@NotNull JsonValue value) {
      return value instanceof JsonBooleanLiteral;
    }

    private static boolean isBooleanLiteral(@NotNull List<? extends ValueType> types, @NotNull ProblemSink holder, @NotNull JsonValue value) {
      final JsonBooleanLiteral boolLiteral = ObjectUtils.tryCast(value, JsonBooleanLiteral.class);
      if (boolLiteral == null) return false;

      if (! Boolean.TRUE.toString().equals(boolLiteral.getText())) {
        new AnnotationTextBuilder(holder, value).withTypes(types).build();
      }
      return true;
    }

    private static boolean isArray(@NotNull Map<ValueType, JscsOptionDescriptor.ValueDescription> map, @NotNull ProblemSink holder,
                                   @NotNull JsonValue value, @Nullable final String replacementMessage) {
      final JsonArray array = ObjectUtils.tryCast(value, JsonArray.class);
      if (array == null) return false;

      checkForList(map, array, holder, replacementMessage);
      return true;
    }

    private static void checkForList(@NotNull Map<ValueType, JscsOptionDescriptor.ValueDescription> map, @NotNull JsonArray array,
                                     @NotNull ProblemSink holder, @Nullable final String expectedMessage) {
      final JscsOptionDescriptor.ValueDescription descriptor = map.get(ValueType.array);
      if (JscsOptionDescriptor.ValueDescription.NO_VALIDATION.equals(descriptor)) {
        validateArray(array, holder, (element, literal) -> {
        });
        return;
      }

      if (descriptor instanceof JscsOptionDescriptor.StringVerifier) {
        final JscsOptionDescriptor.StringVerifier verifier = (JscsOptionDescriptor.StringVerifier)descriptor;
        validateArray(array, holder, (element, literal) -> {
          final String error = verifier.validate(literal.getValue());
          if (error != null) {
            createWarningAnnotation(holder, element, error);
          }
        });
        return;
      }

      if (descriptor instanceof JscsOptionDescriptor.ValuesCollection) {
        final JscsOptionDescriptor.ValuesCollection valuesCollection = (JscsOptionDescriptor.ValuesCollection)descriptor;
        final String[] values = valuesCollection.getArrValues();
        validateArray(array, holder, (element, stringLiteral) -> {
          if (valuesCollection.isOtherValuesAllowed() || values.length == 0) return;
          if (!valuesCollection.contains(StringUtil.unquoteString(stringLiteral.getText()))) {
            new AnnotationTextBuilder(holder, element).withOptions(values).withOptionsReplacement(expectedMessage).build();
          }
        });
      }
    }

    private static void validateArray(@NotNull JsonArray array, @NotNull ProblemSink holder, @NotNull PairConsumer<? super PsiElement, ? super JsonStringLiteral> consumer) {
      for (PsiElement element : array.getChildren()) {
        final JsonStringLiteral stringLiteral = ObjectUtils.tryCast(element, JsonStringLiteral.class);
        if (stringLiteral == null) {
          createWarningAnnotation(holder, element, "string expected");
          continue;
        }
        consumer.consume(element, stringLiteral);
      }
    }

    private static boolean isStringLiteral(@NotNull Map<ValueType, JscsOptionDescriptor.ValueDescription> map,
                                           @NotNull List<? extends ValueType> types,
                                           @NotNull ProblemSink holder, @NotNull JsonValue value) {
      final JsonStringLiteral stringLiteral = ObjectUtils.tryCast(value, JsonStringLiteral.class);
      if (stringLiteral == null) return false;

      final JscsOptionDescriptor.ValueDescription descriptor = map.get(ValueType.str);
      if (JscsOptionDescriptor.ValueDescription.NO_VALIDATION.equals(descriptor)) {
        return true;
      }
      final String strValue = StringUtil.unquoteString(stringLiteral.getValue());
      if (descriptor instanceof JscsOptionDescriptor.StringVerifier) {
        final String error = ((JscsOptionDescriptor.StringVerifier)descriptor).validate(strValue);
        if (error != null) {
          createWarningAnnotation(holder, value, error);
        }
        return true;
      }
      if (descriptor instanceof JscsOptionDescriptor.ValuesCollection) {
        final JscsOptionDescriptor.ValuesCollection valuesCollection = (JscsOptionDescriptor.ValuesCollection)descriptor;
        final String[] values = valuesCollection.getArrValues();
        if (valuesCollection.isOtherValuesAllowed() || values.length == 0) return true;
        if (! valuesCollection.contains(strValue)) {
          final List<ValueType> list = new ArrayList<>(types);
          list.remove(ValueType.str);
          new AnnotationTextBuilder(holder, value).withOptions(values).withTypes(list).build();
        }
        return true;
      }
      return false;
    }
  }

  private static String getReplacementValuesMessage(@NotNull final JscsOption option) {
    if (option.getDescriptor().isWithKeywords()) {
      return "keywords (if, else, while, ...)";
    }
    if (option.getDescriptor().isWithBinaryOperators()) {
      return "binary operators (+,>,/, ...)";
    }
    if (option.getDescriptor().isWithOperators()) {
      return "operators (+,>,?, ...)";
    }
    return null;
  }

  private static boolean createExpectedTypesAnnotation(@NotNull List<? extends ValueType> types, @NotNull ProblemSink holder, @NotNull JsonValue value) {
    new AnnotationTextBuilder(holder, value).withTypes(types).build();
    return true;
  }

  public static class AnnotationTextBuilder {
    @NotNull
    private final ProblemSink myHolder;
    @NotNull
    private final PsiElement myElt;

    private String myOptionsReplacement;
    private String[] myOptions;

    private boolean myWithOptions;
    private boolean myWithTypes;
    private List<? extends ValueType> myOtherTypes;

    AnnotationTextBuilder(@NotNull ProblemSink holder, @NotNull PsiElement elt) {
      myHolder = holder;
      myElt = elt;
    }

    @NotNull
    AnnotationTextBuilder withOptionsReplacement(String optionsReplacement) {
      myOptionsReplacement = optionsReplacement;
      myWithOptions = true;
      return this;
    }

    @NotNull
    AnnotationTextBuilder withOptions(String[] options) {
      if (options != null && options.length == 0) return this;

      myOptions = options;
      myOptionsReplacement = null;
      myWithOptions = true;
      return this;
    }

    @NotNull
    AnnotationTextBuilder withTypes(@NotNull final List<? extends ValueType> otherTypes) {
      myWithTypes = true;
      myOtherTypes = otherTypes;
      return this;
    }

    public void build() {
      final String text = getText();
      if (text == null) return;
      createWarningAnnotation(myHolder, myElt, text);
    }

    @Nullable
    public String getText() {
      if (!myWithOptions && !myWithTypes) return null;

      final StringBuilder sb = new StringBuilder("Expected");
      int cnt = 0;
      if (myWithOptions) {
        // if options == null, replacement text is used, which means there're many options
        if (myOptions == null || myOptions.length > 1) {
          sb.append(" values: ");
        }
        else {
          sb.append(" value: ");
        }
        cnt = appendOptionsValues(sb, cnt);
      }
      if (myWithTypes) {
        if (!myWithOptions) sb.append(": ");
        appendTypes(myOtherTypes, sb, cnt);
      }
      return sb.toString();
    }

    private static void appendTypes(@NotNull List<? extends ValueType> otherTypes, @NotNull StringBuilder sb, int cnt) {
      for (ValueType otherType : otherTypes) {
        if (cnt > 0) sb.append(" or ");
        sb.append(otherType.getNameOrFixedValue());
        ++ cnt;
      }
    }

    private int appendOptionsValues(@NotNull StringBuilder sb, int cnt) {
      if (myOptionsReplacement != null) {
        sb.append(myOptionsReplacement);
        ++cnt;
      }
      else {
        if (myOptions != null) {
          for (String s : myOptions) {
            if (cnt > 0) sb.append(", ");
            if ("\t".equals(s)) {
              sb.append("\"\\t\"");
            }
            else if (s.length() == 1) {
              sb.append(s);
            }
            else {
              sb.append("\"").append(s).append("\"");
            }
            ++cnt;
          }
        }
      }
      return cnt;
    }
  }

  private static void createWarningAnnotation(@NotNull ProblemSink holder, @NotNull PsiElement elt, @NotNull String message) {
    holder.createProblem(elt, message);
  }
}
