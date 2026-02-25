// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.grails.references.domain.namedQuery.NamedQueryDescriptor;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrClassReferenceType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class DomainDescriptor {
  
  public static final String GORM_INSTANCE_API_CLASS = "org.grails.datastore.gorm.GormInstanceApi";
  
  public static final Object ADD_TO_METHOD_MARKER = "Gorm:DomainDescriptor:addToMethod";
  public static final Object DOMAIN_DYNAMIC_METHOD = "Gorm:DomainDescriptor:DynamicMethod";

  // Methods which exists in Domain class in all Grails versions
  private static final String METACLASS_METHODS = "class DomainClass444 {" +

                                                  // See DomainClassGrailsPlugin.registerConstraintsProperty()
                                                  " Map<String, org.codehaus.groovy.grails.validation.ConstrainedProperty> getConstraints(){}\n" +

                                                  " /** @since 1.4 */\n" +
                                                  " public org.codehaus.groovy.grails.commons.GrailsDomainClass getDomainClass(){}\n" +

                                                  " public void setProperties(Object properties){}\n" +

                                                  // Provided by DomainClassGrailsPlugin.addValidationMethods()
                                                  " public org.springframework.validation.Errors getErrors() {}\n" +
                                                  " public void setErrors(org.springframework.validation.Errors errors) {}\n" +
                                                  //" public boolean hasErrors(){}\n" + Added by both DomainClassGrailsPlugin.addValidationMethods() and GormValidationTransformer
                                                  //" public void clearErrors(){}\n" + Added by both DomainClassGrailsPlugin.addValidationMethods() and GormValidationTransformer


                                                  "}";

  /**
   * The methods those was injected to metaclass in Grails 1.3.7 and older. Since Grails 1.4 these methods inserts to
   * compiled class via AST transformation.
   * <p/>
   */
  private static final String OLD_METHODS = "class OldDomainClassMethods<T> {" +
                                            // Provided by Domain plugin
                                            " public Long ident(){}\n" +

                                            // Provided by Hibernate Plugin
                                            " public void lock(){}\n" +
                                            " public static T lock(java.io.Serializable id){}\n" +
                                            " public T refresh(){}\n" +

                                            " public T save(){}\n" +
                                            " public T save(java.util.Map args){}\n" +
                                            " public T save(Boolean validate){}\n" +

                                            " public T merge(){}\n" +
                                            " public T merge(java.util.Map args){}\n" +

                                            " public T attach(){}\n" +
                                            " public boolean isAttached(){}\n" +
                                            " public boolean instanceOf(Class aClass){}\n" +

                                            " public T discard(){}\n" +

                                            " public void delete(){}\n" +
                                            " public void delete(java.util.Map args){}\n" +

                                            " public boolean isDirty(){}\n" +
                                            " public boolean isDirty(String fieldName){}\n" +
                                            " public List<String> getDirtyPropertyNames(){}\n" +
                                            " public Object getPersistentValue(String fieldName){}\n" +

                                            " public boolean hasErrors(){}\n" +
                                            // Added by both DomainClassGrailsPlugin.addValidationMethods() and GormValidationTransformer
                                            " public void clearErrors(){}\n" +
                                            // Added by both DomainClassGrailsPlugin.addValidationMethods() and GormValidationTransformer

                                            " public boolean validate(){}\n" +
                                            // Aded by
                                            " public boolean validate(java.util.Map args){}\n" +
                                            " public boolean validate(boolean b){}\n" +
                                            " public boolean validate(java.util.List args){}\n" +

                                            " public static T get(java.io.Serializable id){}\n" +
                                            " public static T read(java.io.Serializable id){}\n" +
                                            " public static T load(java.io.Serializable id){}\n" +
                                            " public static List<T> getAll() {}\n" +
                                            " public static List<T> getAll(java.util.List<java.io.Serializable> ids) {}\n" +

                                            " public static Object withCriteria(groovy.lang.Closure callable) {}\n" +
                                            " public static Object withCriteria(java.util.Map builderArgs, groovy.lang.Closure callable) {}\n" +
                                            " public static grails.orm.HibernateCriteriaBuilder createCriteria() {}\n" +

                                            " public static Integer count(){}\n" +
                                            " public static boolean exists(java.io.Serializable id) {}\n" +
                                            " public static List<T> list() {}\n" +
                                            " public static List<T> list(java.util.Map args) {}\n" +

                                            " public static List<T> findAll() {}\n" +
                                            " public static List<T> findAll(T example) {}\n" +
                                            " public static List<T> findAll(T example, java.util.Map args) {}\n" +

                                            " public static List<T> findAll(String query) {}\n" +
                                            " public static List<T> findAll(String query, java.util.Collection positionalParams) {}\n" +
                                            " public static List<T> findAll(String query, java.util.Collection positionalParams, java.util.Map paginateParams) {}\n" +
                                            " public static List<T> findAll(String query, java.util.Map namedArgs) {}\n" +
                                            " public static List<T> findAll(String query, java.util.Map namedArgs, java.util.Map paginateParams) {}\n" +

                                            " public static List<T> findAllWhere(java.util.Map query) {}\n" +

                                            " public static T find(String query) {}\n" +
                                            " public static T find(String query, java.util.Collection args) {}\n" +
                                            " public static T find(String query, java.util.Map namedArgs) {}\n" +
                                            " public static T find(T example) {}\n" +

                                            " public static T findWhere(java.util.Map query) {}\n" +

                                            " public static Object withSession(groovy.lang.Closure callable){}\n" +
                                            " public static Object withTransaction(groovy.lang.Closure callable){}\n" +
                                            " public static Object withNewSession(groovy.lang.Closure callable){}\n" +

                                            " public static int executeUpdate(String query) {}\n" +
                                            " public static int executeUpdate(String query, java.util.Collection args) {}\n" +
                                            " public static int executeUpdate(String query, java.util.Map argMap) {}\n" +

                                            " public static List<T> executeQuery(String query) {}\n" +
                                            " public static List<T> executeQuery(String query, java.util.Collection positionalParams) {}\n" +
                                            " public static List<T> executeQuery(String query, java.util.Collection positionalParams, java.util.Map paginateParams) {}\n" +
                                            " public static List<T> executeQuery(String query, java.util.Map namedParams) {}\n" +
                                            " public static List<T> executeQuery(String query, java.util.Map namedParams, java.util.Map paginateParams) {}\n" +
                                            "}";

  public static final Set<String> NOT_A_PERSISTENT_PROPERTIES = ContainerUtil.newHashSet("errors", "constraints", "properties", "metaClass", "class");

  private final PsiClass myDomainClass;

  private final PsiClassType myDomainClassType;

  private volatile Map<String, Pair<PsiType, PsiElement>> myPersistentProperties;
  private Map<String, Pair<PsiType, PsiElement>> myPropertiesWithTransients;

  private volatile Map<String, NamedQueryDescriptor> myNamedQueries;

  private volatile List<PsiMethod> myAddToAndRemoveFromMethods;

  private volatile List<PsiMethod> myGetReferenceIdMethods;

  private final Map<String, Pair<PsiType, PsiElement>> myHasMany;
  private final Map<String, Pair<PsiType, PsiElement>> myHasOne;
  private final Map<String, Pair<PsiType, PsiElement>> myBelongsTo;

  private Set<String> myEmbeddedList;
  
  private volatile String myGrailsVersion;
  
  private volatile Boolean myHasGormApi14;

  public DomainDescriptor(final PsiClass domainClass) {
    myDomainClass = domainClass;
    myDomainClassType = PsiTypesUtil.getClassType(domainClass);

    if (domainClass instanceof GrTypeDefinition grTypeDefinition) {
      myHasMany = new HashMap<>();
      Function<PsiClass, PsiClass> superClass = PsiClass::getSuperClass;
      findAllPropertiesFromField(myHasMany, DomainClassRelationsInfo.HAS_MANY_NAME, grTypeDefinition, superClass);
      findAllPropertiesFromField(myHasMany, DomainClassRelationsInfo.RELATES_TO_MANY_NAME, grTypeDefinition, superClass);
      myHasOne = findAllPropertiesFromField(DomainClassRelationsInfo.HAS_ONE_NAME, grTypeDefinition, superClass);
      myBelongsTo = findAllPropertiesFromField(DomainClassRelationsInfo.BELONGS_TO_NAME, grTypeDefinition, superClass);
    }
    else {
      myHasMany = Collections.emptyMap();
      myHasOne = Collections.emptyMap();
      myBelongsTo = Collections.emptyMap();
    }
  }

  public static @NotNull DomainDescriptor getDescriptor(@NotNull PsiClass domainClass) {
    final PsiClass aClass = PsiUtil.getOriginalClass(domainClass);
    return CachedValuesManager.getCachedValue(aClass, () -> CachedValueProvider.Result
      .create(new DomainDescriptor(aClass), PsiModificationTracker.MODIFICATION_COUNT));
  }

  public static Map<String, Pair<PsiType, PsiElement>> getPersistentProperties(@NotNull PsiClass domainClass) {
    return getDescriptor(domainClass).getPersistentProperties();
  }

  private static void findAllPropertiesFromField(Map<String, Pair<PsiType, PsiElement>> res,
                                                 @Nullable PsiField field,
                                                 GrTypeDefinition domainClass) {
    if (!(field instanceof GrField)) return;

    final GrExpression initializer = ((GrField)field).getInitializerGroovy();
    if (!(initializer instanceof GrListOrMap lom) || !lom.isMap()) return;

    final GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(field.getProject());

    for (GrNamedArgument argument : lom.getNamedArguments()) {
      final GrArgumentLabel label = argument.getLabel();
      final GrExpression expr = argument.getExpression();
      if (label != null && expr instanceof GrReferenceExpression) {
        final String name = label.getName();
        if (!GroovyNamesUtil.isIdentifier(name)) continue;
        GrCodeReferenceElement codeReference = factory.createCodeReference(expr.getText(), expr.getContext());
        GrClassReferenceType type = new GrClassReferenceType(codeReference);
        res.put(name, new Pair<>(type, resolveLabel(label, name, domainClass)));
      }
    }
  }

  /**
   * finds all properties defined in field.
   * returns Trinity of (
   * name of property,
   * type of property,
   * psiElement (GrField or GrArgumentLabel) in which property is defined
   * )
   *
   * @param fieldName   Name of fields (e.g. belongsTo, hasMany, mappedBy, ...)
   * @param domainClass domainClass
   */
  static void findAllPropertiesFromField(Map<String, Pair<PsiType, PsiElement>> res,
                                         String fieldName,
                                         GrTypeDefinition domainClass,
                                         Function<PsiClass, PsiClass> superClass) {
    Set<PsiClass> visited = new HashSet<>();
    for (PsiClass aClass = domainClass; aClass instanceof GrTypeDefinition; aClass = superClass.apply(aClass)) {
      if (!visited.add(aClass)) break;

      PsiField field = ((GrTypeDefinition)aClass).findCodeFieldByName(fieldName, false);
      findAllPropertiesFromField(res, field, domainClass);
    }
  }

  private static Map<String, Pair<PsiType, PsiElement>> findAllPropertiesFromField(String fieldName,
                                                                                   GrTypeDefinition domainClass,
                                                                                   Function<PsiClass, PsiClass> superClass) {
    Map<String, Pair<PsiType, PsiElement>> res = new HashMap<>();
    findAllPropertiesFromField(res, fieldName, domainClass, superClass);
    return res;
  }

  private static @Nullable PsiElement resolveLabel(GrArgumentLabel label, String name, GrTypeDefinition domainClass) {
    for (PsiMethod method : domainClass.findCodeMethodsByName(GroovyPropertyUtils.getGetterNameNonBoolean(name), true)) {
      if (method.getParameterList().getParametersCount() == 0) {
        return method;
      }
    }

    return label;
  }

  private static void parseFieldNameList(Collection<String> res, @Nullable PsiField field) {
    if (!(field instanceof GrField)) return;

    GrExpression initializer = ((GrField)field).getInitializerGroovy();
    if (!(initializer instanceof GrListOrMap lom)) return;

    if (lom.isMap()) return;
    
    for (GrExpression argument : lom.getInitializers()) {
      if (argument instanceof GrLiteralImpl) {
        Object value = ((GrLiteralImpl)argument).getValue();
        if (value instanceof String) {
          res.add((String)value);
        }
      }
    }
  }

  private static void collectAllEmbeddedPropertyNames(Collection<String> res, PsiClass domainClass) {
    Set<PsiClass> visited = new HashSet<>();
    for (PsiClass aClass = domainClass; aClass instanceof GrTypeDefinition; aClass = aClass.getSuperClass()) {
      if (!visited.add(aClass)) break;

      PsiField field = ((GrTypeDefinition)aClass).findCodeFieldByName("embedded", false);
      parseFieldNameList(res, field);
    }
  }

  public Set<String> getEmbeddedPropertyNames() {
    Set<String> res = myEmbeddedList;
    
    if (res == null) {
      res = new HashSet<>();
      collectAllEmbeddedPropertyNames(res, myDomainClass);
      myEmbeddedList = res;
    }
    
    return res;
  }

  public @NotNull String getGrailsVersion() {
    String res = myGrailsVersion;
    if (res == null) {
      GrailsStructure instance = GrailsStructure.getInstance(myDomainClass);
      if (instance != null) {
        res = instance.getGrailsVersion();
      }
      
      if (res == null) {
        res = "zzz"; // "zzz" - last version. (every version < "zzz")
      }
       
      myGrailsVersion = res;
    }

    return res;
  }

  private static boolean isDomainClassType(@Nullable PsiType type) {
    PsiClass aClass = PsiTypesUtil.getPsiClass(type);
    return GormUtils.isGormBean(aClass);
  }

  // See DomainClassGrailsPlugin.addRelationshipManagementMethods()
  public List<PsiMethod> getAddToAndRemoveFromMethods() {
    List<PsiMethod> res = myAddToAndRemoveFromMethods;
    if (res == null) {
      if (myHasMany.isEmpty()) {
        res = Collections.emptyList();
      }
      else {
        res = new ArrayList<>(myHasMany.size() * 3);
        PsiManager manager = myDomainClass.getManager();

        for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : myHasMany.entrySet()) {
          PsiElement element = entry.getValue().second;
          PsiType fieldType = entry.getValue().first;

          String propertyName = StringUtil.capitalizeWithJavaBeanConvention(entry.getKey());

          String addToName = "addTo" + propertyName;

          GrLightMethodBuilder addInstance = new GrLightMethodBuilder(manager, addToName);
          addInstance.addModifier(GrModifierFlags.PUBLIC_MASK);
          addInstance.setReturnType(myDomainClassType);
          addInstance.addParameter("instance", fieldType);
          addInstance.setNavigationElement(element);
          addInstance.setContainingClass(myDomainClass);

          res.add(addInstance);

          if (isDomainClassType(fieldType)) {
            GrLightMethodBuilder addMap = new GrLightMethodBuilder(manager, addToName);
            addMap.setMethodKind(ADD_TO_METHOD_MARKER);
            addMap.setModifiers(GrModifierFlags.PUBLIC_MASK);
            addMap.setReturnType(myDomainClassType);
            addMap.addParameter("args", CommonClassNames.JAVA_UTIL_MAP);
            addMap.setContainingClass(myDomainClass);
            addMap.setNavigationElement(element);

            res.add(addMap);
          }

          GrLightMethodBuilder remove = new GrLightMethodBuilder(manager, "removeFrom" + propertyName);
          remove.addModifier(GrModifierFlags.PUBLIC_MASK);
          remove.setReturnType(PsiTypes.voidType());
          remove.addParameter("instance", fieldType);
          remove.setNavigationElement(element);
          remove.setContainingClass(myDomainClass);

          res.add(remove);
        }
      }

      myAddToAndRemoveFromMethods = res;
    }

    return res;
  }

  public List<PsiMethod> getGetReferenceIdMethods() {
    List<PsiMethod> res = myGetReferenceIdMethods;
    
    if (res == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : getPersistentProperties().entrySet()) {
        if (GormUtils.isGormBean(PsiTypesUtil.getPsiClass(entry.getValue().first))) {
          String getterName = GroovyPropertyUtils.getGetterNameNonBoolean(entry.getKey() + "Id");
          
          GrLightMethodBuilder m = new GrLightMethodBuilder(myDomainClass.getManager(), getterName);
          m.setModifiers(GrModifierFlags.PUBLIC_MASK);
          m.setReturnType(CommonClassNames.JAVA_LANG_LONG, myDomainClass.getResolveScope());
          
          if (res == null) res = new ArrayList<>();
          res.add(m);
        }
      }
      
      if (res == null) {
        res = Collections.emptyList();
      }
      
      myGetReferenceIdMethods = res;
    }
      
    return res;
  }

  public Map<String, Pair<PsiType, PsiElement>> getHasMany() {
    return myHasMany;
  }

  public Map<String, Pair<PsiType, PsiElement>> getHasOne() {
    return myHasOne;
  }

  public Map<String, Pair<PsiType, PsiElement>> getBelongsTo() {
    return myBelongsTo;
  }

  private boolean processWithTypeReplace(DynamicMemberUtils.ClassMemberHolder classMemberHolder, PsiScopeProcessor processor, PsiMethod[] methods, ResolveState state) {
    if (methods.length == 0) return true;

    PsiSubstitutor substitutor = PsiSubstitutor.EMPTY.putAll(classMemberHolder.getParsedClass(), new PsiType[]{myDomainClassType});

    for (PsiMethod method : methods) {
      GrLightMethodBuilder methodBuilder = GrailsPsiUtil.substitute(method, substitutor);

      methodBuilder.setData(myDomainClass);
      methodBuilder.setMethodKind(DOMAIN_DYNAMIC_METHOD);

      if (!processor.execute(methodBuilder, state)) return false;
    }

    return true;
  }

  public boolean processDynamicMethods(PsiScopeProcessor processor, @Nullable String nameHint, ResolveState state) {
    final Project project = myDomainClass.getProject();

    if (!hasGormApi14()) {
      DynamicMemberUtils.ClassMemberHolder members = DynamicMemberUtils.getMembers(project, OLD_METHODS);

      if (!processWithTypeReplace(members, processor, members.getDynamicMethods(nameHint), state)) {
        return false;
      }
    }

    String version = getGrailsVersion();

    for (PsiMethod method : DynamicMemberUtils.getMembers(project, METACLASS_METHODS).getDynamicMethods(nameHint)) {
      if (DynamicMemberUtils.checkVersion(method, version)) {
        if (!processor.execute(method, state)) return false;
      }
    }

    if (nameHint == null || nameHint.startsWith("addTo") || nameHint.startsWith("removeFrom")) {
      for (PsiMethod method : getAddToAndRemoveFromMethods()) {
        if (nameHint == null || nameHint.equals(method.getName())) {
          if (!processor.execute(method, state)) return false;
        }
      }
    }

    if (nameHint == null || nameHint.endsWith("Id") || nameHint.length() > 2) {
      for (PsiMethod method : getGetReferenceIdMethods()) {
        if (nameHint == null || nameHint.equals(method.getName())) {
          if (!processor.execute(method, state)) return false;
        }
      }
    }

    return true;
  }

  private boolean hasGormApi14() {
    Boolean res = myHasGormApi14;
    
    if (res == null) {
      PsiClass api14Class =
        JavaPsiFacade.getInstance(myDomainClass.getProject()).findClass(GORM_INSTANCE_API_CLASS, myDomainClass.getResolveScope());
      
      res = api14Class != null;
      
      myHasGormApi14 = res;
    }
    
    return res;
  }

  public boolean processStaticMethods(PsiScopeProcessor processor, @Nullable String nameHint, ResolveState state) {
    final Project project = myDomainClass.getProject();

    if (!hasGormApi14()) {
      DynamicMemberUtils.ClassMemberHolder members = DynamicMemberUtils.getMembers(project, OLD_METHODS);

      if (!processWithTypeReplace(members, processor, members.getStaticMethods(nameHint), state)) {
        return false;
      }
    }

    String version = getGrailsVersion();
    
    for (PsiMethod method : DynamicMemberUtils.getMembers(project, METACLASS_METHODS).getDynamicMethods(nameHint)) {
      if (DynamicMemberUtils.checkVersion(method, version)) {
        if (!processor.execute(method, state)) return false;
      }
    }

    return true;
  }

  public Map<String, NamedQueryDescriptor> getNamedQueries() {
    Map<String, NamedQueryDescriptor> res = myNamedQueries;
    if (res == null) {
      PsiField namedQueryField = myDomainClass.findFieldByName("namedQueries", false);
      if (namedQueryField instanceof GrField) {
        res = new HashMap<>();

        GrExpression initializer = ((GrField)namedQueryField).getInitializerGroovy();
        if (initializer instanceof GrClosableBlock) {
          for (PsiElement e = initializer.getFirstChild(); e != null; e = e.getNextSibling()) {
            if (e instanceof GrMethodCall method) {
              GrClosableBlock closure = GrailsUtils.getClosureArgument(method);
              if (closure != null) {
                PsiElement invokedExpr = method.getInvokedExpression();
                if (invokedExpr instanceof GrReferenceExpression && !((GrReferenceExpression)invokedExpr).isQualified()) {
                  String name = ((GrReferenceExpression)invokedExpr).getReferenceName();
                  res.put(name, new NamedQueryDescriptor(this, name, method, closure));
                }
              }
            }
          }
        }
      }
      else {
        res = Collections.emptyMap();
      }

      myNamedQueries = res;
    }
    return res;
  }

  public PsiClass getDomainClass() {
    return myDomainClass;
  }

  public boolean isToManyRelation(String propertyName) {
    return myHasMany.containsKey(propertyName); // There is only one way to define property with type Collection - via hasMany = [].
  }
  
  public Map<String, Pair<PsiType, PsiElement>> getPersistentProperties() {
    Map<String, Pair<PsiType, PsiElement>> result = myPersistentProperties;
    if (result == null) {
      result = new HashMap<>();

      for (PsiMethod method : myDomainClass.getAllMethods()) {
        if (GroovyPropertyUtils.isSimplePropertyGetter(method) && !method.hasModifierProperty(PsiModifier.STATIC)) {
          final String propertyName = GroovyPropertyUtils.getPropertyNameByGetter(method);
          if (!NOT_A_PERSISTENT_PROPERTIES.contains(propertyName) && !result.containsKey(propertyName)) {
            PsiElement element = method;
            if (element instanceof GrAccessorMethod) {
              GrField field = ((GrAccessorMethod)element).getProperty();
              if (!(field instanceof LightElement) && field.getTypeElementGroovy() == null) continue;

              element = field;
            }

            PsiType returnType = method.getReturnType();
            assert returnType != null;

            if (InheritanceUtil.isInheritor(returnType, CommonClassNames.JAVA_UTIL_COLLECTION)) {
              // See DefaultGrailsDomainClass.establishRelationshipForCollection()
              Pair<PsiType, PsiElement> pair = myHasMany.get(propertyName);

              if (pair == null) continue; // All collection properties which is not exists in hasMany is a transient properties.

              if (com.intellij.psi.util.PsiUtil.extractIterableTypeParameter(returnType, false) == null) {
                PsiClass collectionClass = ((PsiClassType)returnType).resolve();
                returnType = new PsiImmediateClassType(collectionClass, PsiSubstitutor.EMPTY.putAll(collectionClass, new PsiType[]{pair.first}));
              }
            }

            result.put(propertyName, Pair.create(returnType, element));
          }
        }
      }

      myPropertiesWithTransients = (Map<String, Pair<PsiType, PsiElement>>)((HashMap<?, ?>)result).clone();

      removeTransientsProperties(result, "transients");
      removeTransientsProperties(result, "evanescent");

      myPersistentProperties = result;
    }

    return result;
  }

  public @NotNull Map<String, Pair<PsiType, PsiElement>> getPropertiesWithTransients() {
    getPersistentProperties(); // make sure what myPropertiesWithTransients is initialized.
    return myPropertiesWithTransients;
  }

  private void removeTransientsProperties(Map<String, Pair<PsiType, PsiElement>> result, String transientFieldName) {
    PsiField transientList = myDomainClass.findFieldByName(transientFieldName, false);
    if (transientList instanceof GrField && transientList.hasModifierProperty(PsiModifier.STATIC)) {
      final GrExpression initializer = ((GrField)transientList).getInitializerGroovy();
      if (initializer instanceof GrListOrMap && !((GrListOrMap)initializer).isMap()) {
        GrailsPsiUtil.removeValuesFromList(result.keySet(), (GrListOrMap)initializer);
      }
    }
  }
}
