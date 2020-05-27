package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.json.psi.JsonValue;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Irina.Chernushina on 4/30/2015.
 */
public class JscsOptionDescriptor {
  private final Map<ValueType, ValueDescription> myTypes;
  private boolean myWithKeywords;
  private boolean myWithOperators;
  private boolean myWithBinaryOperators;
  private String myDescription;

  public JscsOptionDescriptor() {
    myTypes = new HashMap<>();
  }

  public JscsOptionDescriptor addType(@NotNull ValueType type, @NotNull ValueDescription description) {
    myTypes.put(type, description);
    return this;
  }

  public boolean canBe(@NotNull final ValueType type) {
    return myTypes.containsKey(type);
  }

  public Map<ValueType, ValueDescription> getTypes() {
    return myTypes;
  }

  public JscsOptionDescriptor canBool() {
    return addType(ValueType.bool, ValueDescription.NO_VALIDATION);
  }

  public JscsOptionDescriptor canTrueOrFalse() {
    return addType(ValueType.true_or_false, ValueDescription.NO_VALIDATION);
  }

  public JscsOptionDescriptor canInteger() {
    return addType(ValueType.num_int, ValueDescription.NO_VALIDATION);
  }

  public JscsOptionDescriptor canStringArray(final String... values) {
    return addType(ValueType.array, new ValuesCollection(values));
  }

  public JscsOptionDescriptor canExpandableStringArray(final String... values) {
    final ValuesCollection collection = new ValuesCollection(values).otherValuesAllowed();
    return addType(ValueType.array, collection);
  }

  public JscsOptionDescriptor canKeywordsArray() {
    myWithKeywords = true;
    return addType(ValueType.array, new ValuesCollection(Constants.keywords));
  }

  public JscsOptionDescriptor canOperatorsArray() {
    myWithOperators = true;
    return addType(ValueType.array, new ValuesCollection(Constants.operators));
  }

  public JscsOptionDescriptor canBinaryOperatorsArray() {
    myWithBinaryOperators = true;
    return addType(ValueType.array, new ValuesCollection(Constants.binaryOperators));
  }

  public JscsOptionDescriptor canString(final String... str) {
    return addType(ValueType.str, new ValuesCollection(str));
  }

  public JscsOptionDescriptor withDescription(final String description) {
    myDescription = description;
    return this;
  }

  public boolean isWithKeywords() {
    return myWithKeywords;
  }

  public boolean isWithOperators() {
    return myWithOperators;
  }

  public boolean isWithBinaryOperators() {
    return myWithBinaryOperators;
  }

  public String getDescription() {
    return myDescription;
  }

  public interface ValueDescription {
    ValueDescription NO_VALIDATION = new ValueDescription(){};
  }

  public static class CustomValidation implements ValueDescription {
    private final Function<? super JsonValue, String> myValidator;

    public CustomValidation(Function<? super JsonValue, String> validator) {
      myValidator = validator;
    }

    String check(JsonValue value) {
      return myValidator.fun(value);
    }
  }

  public static class ValuesCollection implements ValueDescription {
    private final Set<String> myValues;
    private final String[] myArrValues;
    private boolean myIsOtherValuesAllowed;
    private boolean myNoTrimValue;

    public ValuesCollection(String... values) {
      myArrValues = values;
      myValues = ContainerUtil.set(values);
    }

    public ValuesCollection doNotTrimValues() {
      myNoTrimValue = true;
      return this;
    }

    public String[] getArrValues() {
      return myArrValues;
    }

    public ValuesCollection otherValuesAllowed() {
      myIsOtherValuesAllowed = true;
      return this;
    }

    public boolean contains(@NotNull final String value) {
      return myIsOtherValuesAllowed || (myNoTrimValue ? myValues.contains(value) : myValues.contains(value.trim()));
    }

    public boolean isOtherValuesAllowed() {
      return myIsOtherValuesAllowed;
    }

    public boolean isNoTrimValue() {
      return myNoTrimValue;
    }
  }

  public static class ValuesObject implements ValueDescription {
    private final Map<String, JscsOptionDescriptor> myFields;
    private String myMandatory;
    private boolean myMustNotBeEmpty;

    public ValuesObject(Consumer<? super ValuesObject> init) {
      myFields = new HashMap<>(4);
      init.consume(this);
    }

    public ValuesObject addBoolFields(final String... names) {
      for (String name : names) {
        addField(name).addType(ValueType.bool, NO_VALIDATION);
      }
      return this;
    }

    public JscsOptionDescriptor addField(final String name) {
      final JscsOptionDescriptor field = new JscsOptionDescriptor();
      myFields.put(name, field);
      return field;
    }

    public Map<String, JscsOptionDescriptor> getFields() {
      return myFields;
    }

    public ValuesObject mandatory(@NotNull final String name) {
      myMandatory = name;
      return this;
    }

    public String getMandatory() {
      return myMandatory;
    }

    public void mustNotBeEmpty() {
      myMustNotBeEmpty = true;
    }

    public boolean isMustNotBeEmpty() {
      return myMustNotBeEmpty;
    }
  }

  public static class StringVerifier implements ValueDescription {
    private final Function<? super String, String> myValidator;

    public StringVerifier(Function<? super String, String> validator) {
      myValidator = validator;
    }

    String validate(final String value) {
      return myValidator.fun(value);
    }
  }
}
