package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.actions.AnnotationRequest;
import com.intellij.lang.jvm.actions.CreateMethodRequest;
import com.intellij.lang.jvm.actions.ExpectedParameter;
import com.intellij.lang.jvm.actions.ExpectedType;
import com.intellij.lang.jvm.actions.ExpectedTypesKt;
import com.intellij.lang.jvm.actions.JvmElementActionFactories;
import com.intellij.lang.jvm.actions.ParametersKt;
import com.intellij.lang.jvm.types.JvmSubstitutor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJvmSubstitutor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.util.Pair.pair;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

public class GwtUiComponentDescriptor extends GwtXmlElementDescriptorBase {
  private static final Key<CachedValue<Pair<Boolean, Map<String, XmlAttributeDescriptor>>>> GWT_UI_TAG_ADDITIONAL_ATTRIBUTES_KEY = Key.create("GWT_UI_TAG_ADDITIONAL_ATTRIBUTES");
  private static final @NonNls Set<String> IGNORED_PROPERTIES = Collections.singleton("HTML");
  private static final @NonNls String[] ADDITIONAL_ATTRIBUTES = {"addStyleNames", "addStyleDependentNames", "debugId"};
  private final PsiClass myClass;
  private final String myNamespacePackage;
  private Map<String, XmlAttributeDescriptor> myAttributesMap;
  private XmlAttributeDescriptor[] myAttributes;
  private GwtUiChildElementDescriptor[] myElementDescriptors;

  public GwtUiComponentDescriptor(@NotNull PsiClass aClass, @NotNull GwtUiComponentsNSDescriptor xmlNSDescriptor,
                                  @NotNull String namespacePackage, @NotNull String namespacePrefix) {
    super(xmlNSDescriptor, namespacePrefix);
    myClass = aClass;
    myNamespacePackage = namespacePackage;
  }

  @Override
  public PsiNamedElement getDeclaration() {
    return myClass;
  }

  @Override
  public String getDefaultName() {
    String qualifiedName = myClass.getQualifiedName();
    if (qualifiedName != null && qualifiedName.startsWith(myNamespacePackage)) {
      return myNamespacePrefix + ":" + qualifiedName.substring(myNamespacePackage.length() + 1);
    }

    return myNamespacePrefix + ":";
  }

  @Override
  public GwtUiChildElementDescriptor[] getElementsDescriptors(XmlTag context) {
    if (myElementDescriptors == null) {
      List<GwtUiChildElementDescriptor> uiChildDescriptors = new SmartList<>();
      for (PsiMethod method : myClass.getAllMethods()) {
        final PsiAnnotation annotation = method.getModifierList().findAnnotation(UiBinderUtil.UI_CHILD_ANNOTATION);
        if (annotation != null) {
          uiChildDescriptors.add(new GwtUiChildElementDescriptor(myXmlNSDescriptor, myNamespacePrefix, method, annotation));
        }
      }
      if (uiChildDescriptors.isEmpty()) {
        myElementDescriptors = GwtUiChildElementDescriptor.EMPTY_ARRAY;
      }
      else {
        myElementDescriptors = uiChildDescriptors.toArray(GwtUiChildElementDescriptor.EMPTY_ARRAY);
      }
    }

    return myElementDescriptors;
  }

  public XmlElementDescriptor[] getComponentDescriptors(XmlTag context) {
    return myXmlNSDescriptor.getComponentDescriptors(context);
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
    for (GwtUiChildElementDescriptor descriptor : getElementsDescriptors(contextTag)) {
      if (descriptor.getTagName().equals(childTag.getLocalName())) {
        return descriptor;
      }
    }
    return new AnyXmlElementDescriptor(this, myXmlNSDescriptor);
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
    loadAttributes(context);

    final Map<String, XmlAttributeDescriptor> additionalAttributes = getAdditionalAttributes(context).getSecond();
    if (!additionalAttributes.isEmpty()) {
      final XmlAttributeDescriptor[] result = new XmlAttributeDescriptor[myAttributes.length + additionalAttributes.size()];
      System.arraycopy(myAttributes, 0, result, 0, myAttributes.length);
      int i = myAttributes.length;
      for (XmlAttributeDescriptor descriptor : additionalAttributes.values()) {
        result[i++] = descriptor;
      }
      return result;
    }

    return myAttributes;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    final XmlAttributeDescriptor descriptor = getAdditionalAttributes(context).getSecond().get(attributeName);
    if (descriptor != null) {
      return descriptor;
    }

    loadAttributes(context);
    return myAttributesMap.get(attributeName);
  }

  private Pair<Boolean, Map<String, XmlAttributeDescriptor>> getAdditionalAttributes(final XmlTag context) {
    if (context == null) {
      return pair(true, emptyMap());
    }

    CachedValue<Pair<Boolean, Map<String, XmlAttributeDescriptor>>> cachedValue = context.getUserData(GWT_UI_TAG_ADDITIONAL_ATTRIBUTES_KEY);
    if (cachedValue == null) {
      cachedValue = CachedValuesManager.getManager(myClass.getProject()).createCachedValue(() -> {
        final PsiFile file = context.getContainingFile().getOriginalFile();
        if (file instanceof XmlFile xmlFile) {
          if (UiBinderUtil.isUiXmlFile(xmlFile)) {
            final List<PsiClass> uiClasses = UiBinderMappingService.getBoundClassesForFile(xmlFile);
            boolean uiConstructorParametersRequired = uiClasses.isEmpty();
            Map<String, XmlAttributeDescriptor> result = new HashMap<>();
            for (PsiClass uiClass : uiClasses) {
              final PsiMethod factoryMethod = findUiFactoryMethod(uiClass);
              if (factoryMethod != null) {
                for (PsiParameter parameter : factoryMethod.getParameterList().getParameters()) {
                  result.put(parameter.getName(), new GwtUiParameterAttributeDescriptor(parameter, true));
                }
              }
              else {
                uiConstructorParametersRequired = true;
              }
            }
            return CachedValueProvider.Result.create(pair(uiConstructorParametersRequired, result),
                                                     PsiModificationTracker.MODIFICATION_COUNT);
          }
        }

        return CachedValueProvider.Result.create(pair(true, emptyMap()), PsiModificationTracker.MODIFICATION_COUNT);
      }, false);
      context.putUserData(GWT_UI_TAG_ADDITIONAL_ATTRIBUTES_KEY, cachedValue);
    }
    return cachedValue.getValue();
  }

  private @Nullable PsiMethod findUiFactoryMethod(@Nullable PsiClass uiClass) {
    if (uiClass == null) return null;

    for (PsiMethod psiMethod : uiClass.getMethods()) {
      if (psiMethod.getModifierList().hasAnnotation(UiBinderUtil.UI_FACTORY_ANNOTATION)) {
        final PsiType returnType = psiMethod.getReturnType();
        if (returnType != null && returnType.getCanonicalText().equals(myClass.getQualifiedName())) {
          return psiMethod;
        }
      }
    }

    return null;
  }

  private void loadAttributes(@Nullable XmlTag context) {
    if (myAttributesMap != null && myAttributes != null) {
      return;
    }

    Map<String, XmlAttributeDescriptor> attributesMap = new HashMap<>();
    if (context != null) {
      final String uiPrefix = context.getPrefixByNamespace(UiBinderUtil.UI_BINDER_NAMESPACE);
      if (uiPrefix != null) {
        final String qualifiedName = uiPrefix + ":" + UiBinderUtil.UI_FIELD_ATTRIBUTE;
        attributesMap.put(qualifiedName, new AnyXmlAttributeDescriptor(qualifiedName));
      }
    }

    if (InheritanceUtil.isInheritor(myClass, true, UiBinderUtil.WIDGET_BASE_CLASS)) {
      for (String attribute : ADDITIONAL_ATTRIBUTES) {
        attributesMap.put(attribute, new AnyXmlAttributeDescriptor(attribute));
      }
    }

    for (PsiMethod method : myClass.getAllMethods()) {
      if (!canBePropertySetter(method)) continue;

      String propertyName = getPropertyName(method);
      if (propertyName == null || IGNORED_PROPERTIES.contains(propertyName)) continue;

      final PsiParameter[] parameters = method.getParameterList().getParameters();
      if (parameters.length == 0) {
        continue;
      }
      final PsiType type = parameters.length == 1 ? parameters[0].getType() : null;
      attributesMap.put(propertyName, new GwtUiPropertyAttributeDescriptor(method, type, propertyName));

      // cases like "setURL" should allow both "URL" and "uRL" names
      if (Character.isUpperCase(propertyName.charAt(0))) {
        propertyName = decapitalizeFirstChar(propertyName);
        attributesMap.put(propertyName, new GwtUiPropertyAttributeDescriptor(method, type, propertyName));
      }
    }

    boolean uiConstructorParametersRequired = getAdditionalAttributes(context).getFirst();
    if (uiConstructorParametersRequired) {
      for (PsiMethod constructor : myClass.getConstructors()) {
        if (constructor.getModifierList().hasAnnotation(UiBinderUtil.UI_CONSTRUCTOR_ANNOTATION)) {
          for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
            attributesMap.put(parameter.getName(), new GwtUiParameterAttributeDescriptor(parameter, true));
          }
        }
      }
    }

    final Collection<XmlAttributeDescriptor> attributesSet = attributesMap.values();
    myAttributes = attributesSet.toArray(XmlAttributeDescriptor.EMPTY);
    myAttributesMap = attributesMap;
  }

  private static @Nullable String getPropertyName(@NotNull PsiMethod method) {
    String methodName = method.getName();
    if (!methodName.startsWith("set")) return null;

    String propertyName = methodName.substring(3);
    if (propertyName.isEmpty() || Character.isLowerCase(propertyName.charAt(0))) return null;

    return StringUtil.decapitalize(propertyName);
  }

  private static String decapitalizeFirstChar(@NotNull String propertyName) {
    char[] chars = propertyName.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return new String(chars);
  }

  private static boolean canBePropertySetter(@NotNull PsiMethod method) {
    return !method.hasModifierProperty(PsiModifier.STATIC)
           && method.hasModifierProperty(PsiModifier.PUBLIC)
           && !method.isConstructor() && PsiTypes.voidType().equals(method.getReturnType());
  }

  public @NotNull List<IntentionAction> getCreateSetterQuickFixes(XmlAttribute attribute) {
    final Project project = myClass.getProject();
    final String propertyName = attribute.getLocalName();
    final String setterName = PropertyUtilBase.suggestSetterName(propertyName);

    return JvmElementActionFactories.createMethodActions(myClass, new GwtCreateSetterRequest(setterName, project, propertyName));
  }

  private static class GwtCreateSetterRequest implements CreateMethodRequest {
    private final String mySetterName;
    private final Project myProject;
    private final String myPropertyName;

    GwtCreateSetterRequest(String setterName, Project project, String propertyName) {
      mySetterName = setterName;
      myProject = project;
      myPropertyName = propertyName;
    }

    @Override
    public @NotNull String getMethodName() {
      return mySetterName;
    }

    @Override
    public @NotNull List<ExpectedType> getReturnType() {
      return ExpectedTypesKt.expectedTypes(PsiTypes.voidType(), ExpectedType.Kind.EXACT);
    }

    @Override
    public @NotNull Collection<JvmModifier> getModifiers() {
      return singletonList(JvmModifier.PUBLIC);
    }

    @Override
    public @NotNull Collection<AnnotationRequest> getAnnotations() {
      return emptyList();
    }

    @Override
    public @NotNull JvmSubstitutor getTargetSubstitutor() {
      return new PsiJvmSubstitutor(myProject, PsiSubstitutor.EMPTY);
    }

    @Override
    public @NotNull List<ExpectedParameter> getExpectedParameters() {
      PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(myProject);
      PsiClassType javaLangString = psiElementFactory.createTypeByFQClassName(JAVA_LANG_STRING, GlobalSearchScope.allScope(myProject));
      return singletonList(ParametersKt.expectedParameter(javaLangString, myPropertyName));
    }

    @Override
    public boolean isValid() {
      return true;
    }
  }
}
