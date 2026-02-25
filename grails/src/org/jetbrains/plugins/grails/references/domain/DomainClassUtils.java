// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassNode;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinitionBody;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrClassTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation.BELONGS_TO;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation.DOUBLESTRONG;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation.HAS_MANY;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation.MANY_TO_MANY;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation.STRONG;

public final class DomainClassUtils {
  public static final @NonNls String DOMAIN_FIND = "findBy";
  public static final @NonNls String DOMAIN_FIND_ALL = "findAllBy";
  public static final @NonNls String DOMAIN_COUNT = "countBy";
  public static final @NonNls String DOMAIN_FIND_OR_CREATE = "findOrCreateBy";
  public static final @NonNls String DOMAIN_FIND_OR_SAVE = "findOrSaveBy";

  public static final @NonNls String DOMAIN_LIST_ORDER = "listOrderBy";
  public static final @NonNls String[] DOMAIN_CONNECTIVES = {"Or", "And"};

  // #CHECK# See org.grails.datastore.gorm.finders.DynamicFinder
  // For Grails >= 2.0
  public static final String[] DOMAIN_FINDER_EXPRESSIONS_2_0 =
    {"Equal", "NotEqual", "InList", "InRange", "Between", "Like", "Ilike", "Rlike", "GreaterThanEquals", "LessThanEquals", "GreaterThan",
    "LessThan", "IsNull", "IsNotNull", "IsEmpty", "IsNotEmpty"};

  // #CHECK# See AbstractClausedStaticPersistentMethod.GrailsMethodExpression#create()
  // For Grails <= 1.3.7
  public static final String[] DOMAIN_FINDER_EXPRESSIONS_OLD =
    {"LessThanEquals", "LessThan", "GreaterThanEquals", "GreaterThan", "Between", "Like", "Ilike", "Rlike", "IsNotNull", "IsNull", "NotEqual",
      "InList"};

  public static final Set<String> DOMAIN_FINDER_EXPRESSIONS_WITH_ONE_PARAMETER = ContainerUtil.newHashSet(
    "LessThan", "LessThanEquals", "GreaterThan", "GreaterThanEquals", "Like", "Ilike", "Rlike", "NotEqual", "Equal");

  public static final String[] FINDER_PREFIXES = {DOMAIN_COUNT, DOMAIN_FIND, DOMAIN_FIND_ALL, DOMAIN_FIND_OR_CREATE, DOMAIN_FIND_OR_SAVE};

  private static final Pattern FINDER_METHOD_PART_PATTERN = Pattern.compile("(\\w+?)(?:(Not)?(" + StringUtil.join(
    DOMAIN_FINDER_EXPRESSIONS_2_0, "|") + "))?");

  private static final Pattern[] AND_OPERATOR = new Pattern[]{ // See DynamicFinder.operatorPatterns
    Pattern.compile("\\w+(And)\\p{Upper}"),
  };

  private static final Pattern[] OPERATOR_PATTERNS = new Pattern[]{ // See DynamicFinder.operatorPatterns
    AND_OPERATOR[0],
    Pattern.compile("\\w+(Or)\\p{Upper}"),
  };

  private DomainClassUtils(){}

  private static PsiFile @NotNull [] getDomainClasses(Project project, @Nullable VirtualFile domainDirectory) {
    if (domainDirectory == null) return PsiFile.EMPTY_ARRAY;
    PsiDirectory domainPsiDirectory = PsiManager.getInstance(project).findDirectory(domainDirectory);
    return getAllDomainClasses(domainPsiDirectory);
  }

  private static PsiFile @NotNull [] getAllDomainClasses(PsiDirectory domainDirectory) {
    List<PsiFile> children = new ArrayList<>();
    final List<PsiFile> list = getAllChildrenRecursively(domainDirectory, children);
    return PsiUtilCore.toPsiFileArray(list);
  }

  private static List<PsiFile> getAllChildrenRecursively(PsiDirectory domainDirectory, List<PsiFile> children) {
    final PsiFile[] files = domainDirectory.getFiles();
    for (PsiFile file : files) {
      if (file instanceof GroovyFile && !((GroovyFile)file).isScript()) {
        children.add(file);
      }
    }

    for (PsiDirectory file : domainDirectory.getSubdirectories()) {
      getAllChildrenRecursively(file, children);
    }

    return children;
  }

  public static Map<DomainClassNode, List<DomainClassRelationsInfo>> buildNodesAndEdges(Project project, @Nullable VirtualFile domainDirectory) {
    if (project.isDisposed()) return Collections.emptyMap();

    PsiFile[] domainClasses = getDomainClasses(project, domainDirectory);
    Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges = new HashMap<>();

    for (PsiFile domainClass : domainClasses) {
      if (!(domainClass instanceof GroovyFile groovyDomainClass)) continue;

      GrTypeDefinition[] typeDefinitions = groovyDomainClass.getTypeDefinitions();

      for (GrTypeDefinition typeDefinition : typeDefinitions) {
        if (typeDefinition.getQualifiedName() == null) continue;

        buildMapForTypeDefinition(sourcesToOutEdges, typeDefinition);
      }
    }

    for (DomainClassNode node : sourcesToOutEdges.keySet()) {
      processRightsRelationOfThisClass(node.getTypeDefinition(), sourcesToOutEdges);
    }

    return sourcesToOutEdges;
  }

  public static void buildMapForTypeDefinition(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges, GrTypeDefinition typeDefinition) {
    final GrTypeDefinitionBody body = typeDefinition.getBody();
    Set<String> transients = new HashSet<>();

    if (body == null) return;
    GrField[] fields = body.getFields();

    buildTransients(fields, transients);

    //if this class doesn't connect with anything we have to add it in nodes set
    sourcesToOutEdges.put(new DomainClassNode(typeDefinition), new ArrayList<>());
    buildSourceToOutEdgesMapByFields(fields, sourcesToOutEdges, transients);
  }

  private static boolean isTransient(String varName, Set<String> transients) {
    return transients.contains(varName);
  }

  private static void buildTransients(GrField[] fields, Set<String> transients) {
    for (GrField field : fields) {
      PsiModifierList modifierList = field.getModifierList();
      assert modifierList != null;

      if (modifierList.hasModifierProperty(PsiModifier.STATIC) && DomainClassRelationsInfo.TRANSIENTS_NAME.equals(field.getName())) {

        GrExpression initializer = field.getInitializerGroovy();
        if (!(initializer instanceof GrListOrMap list)) return;

        for (GrExpression expression : list.getInitializers()) {
          if (expression instanceof GrString) {
            String varNameStr = expression.getText();

            String varName = varNameStr.substring(1, varNameStr.length() - 2);

            transients.add(varName);
            continue;
          }

          transients.add(expression.getText());
        }
      }
    }
  }

  private static boolean buildSourceToOutEdgesMapByFields(GrField[] fields,
                                                          Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                          Set<String> transients) {
    boolean wasAdded = false;
    for (GrField field : fields) {
      if (isTransient(field.getName(), transients)) continue;

      if (isBelongsToField(field)) {
        buildBelongsToSourcesToOutEdges(sourcesToOutEdges, field);
        wasAdded = true;
      }
      else if (isHasManyField(field)) {
        buildHasManySourcesToOutEdgesMap(sourcesToOutEdges, field);
        wasAdded = true;
      }
    }

    for (GrField field : fields) {
      if (isTransient(field.getName(), transients) || isBelongsToField(field) || isHasManyField(field)) continue;

      final PsiClass thisClass = field.getContainingClass();

      final List<DomainClassRelationsInfo> outEdges = getOutEdgesByTypeDef(thisClass, sourcesToOutEdges);

      final String typeCanonicalString = field.getType().getCanonicalText();
      final String fieldName = field.getName();

      boolean isStrong = true;
      if (outEdges != null) {
        for (DomainClassRelationsInfo edge : outEdges) {

          //if reference was in belongs to return
          if (BELONGS_TO == edge.getRelation() &&
              typeCanonicalString.equals(edge.getTarget().getTypeDefinition().getQualifiedName())) {
            isStrong = false;
            break;
          }

          //has many
          if (HAS_MANY == edge.getRelation() &&
              typeCanonicalString.equals(edge.getTarget().getTypeDefinition().getQualifiedName()) &&
              fieldName.equals(edge.getVarName())) {
            isStrong = false;
            break;
          }
        }
      }

      if (isStrong) {
        buildStrongSourceToOutEdgesMap(sourcesToOutEdges, field);
        wasAdded = true;
      }
      else {
        wasAdded = false;
      }
    }
    return wasAdded;
  }

  private static void processRightsRelationOfThisClass(final PsiClass thisClass,
                                                       final Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges) {
//    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
//      public void run() {
    processRightRelationsInNode(sourcesToOutEdges, thisClass);
    processRightRelationsBetweenNodes(sourcesToOutEdges, thisClass);
//      }
//    }, ModalityState.NON_MODAL);
  }

  private static synchronized void processRightRelationsBetweenNodes(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                                     PsiClass thisClass) {
    final DomainClassNode thisClassNode = new DomainClassNode(thisClass);
    final List<DomainClassRelationsInfo> thisOutEdges = sourcesToOutEdges.get(thisClassNode);

    if (thisOutEdges == null) return;
    final Iterator<DomainClassRelationsInfo> thisOutEdgeIterator = thisOutEdges.iterator();
    while (thisOutEdgeIterator.hasNext()) {
      DomainClassRelationsInfo thisOutEdge = thisOutEdgeIterator.next();

      final DomainClassNode target = thisOutEdge.getTarget();
      if (target.equals(thisClassNode)) continue;

      final List<DomainClassRelationsInfo> backs = sourcesToOutEdges.get(target);

      if (backs == null) continue;
      final Iterator<DomainClassRelationsInfo> backIt = backs.iterator();
      while (backIt.hasNext()) {
        DomainClassRelationsInfo back = backIt.next();

        if (thisClassNode.equals(back.getTarget())) {
          //Strong - Strong
          if (thisOutEdge.getRelation() == STRONG && back.getRelation() == STRONG) {
            back.setRelation(DOUBLESTRONG);

            thisOutEdgeIterator.remove();
            break;
          }
          else if (thisOutEdge.getRelation() == STRONG && back.getRelation() == BELONGS_TO) {
            //Strong - Belongs To
            backIt.remove();
          }
          else if (thisOutEdge.getRelation() == BELONGS_TO && back.getRelation() == STRONG) {
            //Belongs To - Strong
            thisOutEdgeIterator.remove();
            break;
          }
          else if (thisOutEdge.getRelation() == BELONGS_TO && back.getRelation() == BELONGS_TO) {
            //Belongs To - Belongs To

            thisOutEdgeIterator.remove();
            backIt.remove();
            break;
          }
          else if (thisOutEdge.getRelation() == HAS_MANY && back.getRelation() == HAS_MANY) {
            //Has Many - Has Many

            back.setRelation(MANY_TO_MANY);
            thisOutEdgeIterator.remove();
            break;
          }
          else if (thisOutEdge.getRelation() == MANY_TO_MANY /*&& back.getRelation() == HAS_MANY*/) {
            //Many To Many - Has Many

            backIt.remove();
            break;
          }
          else if (/*thisOutEdge.getRelation() == HAS_MANY &&*/ back.getRelation() == MANY_TO_MANY) {
            //Has many - Many To Many

            thisOutEdgeIterator.remove();
            break;
          }
          else if (thisOutEdge.getRelation() == HAS_MANY /*&& (back.getRelation() == BELONGS_TO || back.getRelation() == STRONG)*/) {
            //Has Many - Belongs To or Strong
            backIt.remove();
            break;
          }
          else if (back.getRelation() == HAS_MANY /*&& (thisOutEdge.getRelation() == BELONGS_TO || thisOutEdge.getRelation() == STRONG)*/) {
            //Belongs To or Strong - Has Many

            thisOutEdgeIterator.remove();
            break;
          } /*else if (thisOutEdge.getRelation() == MANY_TO_MANY && back.getRelation() == MANY_TO_MANY) {
            //Has many - Many To Many

            thisOutEdgeIterator.remove();
            break;
          }*/
        }
      }
    }
  }

  private static synchronized void processRightRelationsInNode(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                               PsiClass thisClass) {
    final List<DomainClassRelationsInfo> outEdges = sourcesToOutEdges.get(new DomainClassNode(thisClass));
    if (outEdges == null) return;

    int i = 0;
    while (i < outEdges.size()) {
      DomainClassRelationsInfo edge1 = outEdges.get(i);

      int j = i + 1;
      while (j < outEdges.size()) {
        DomainClassRelationsInfo edge2 = outEdges.get(j);
        if (edge1.getTarget().equals(edge2.getTarget())) {
          if (edge1.getRelation() == HAS_MANY && (edge2.getRelation() == BELONGS_TO || edge2.getRelation() == STRONG)) {
            outEdges.remove(edge2);
          }
          else if (edge2.getRelation() == HAS_MANY &&
                   (edge1.getRelation() == BELONGS_TO || edge1.getRelation() == STRONG)) {
            outEdges.remove(edge1);
            break;
          }
        }
        j++;
      }
      i++;
    }
  }

  private static List<DomainClassRelationsInfo> getOutEdgesByTypeDef(PsiClass thisClass,
                                                                     Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges) {
    for (DomainClassNode node : sourcesToOutEdges.keySet()) {
      if (node.getTypeDefinition().equals(thisClass)) return sourcesToOutEdges.get(node);
    }

    return new ArrayList<>();
  }

  public static boolean isBelongsToField(GrField field) {
    return field.hasModifierProperty(PsiModifier.STATIC) && DomainClassRelationsInfo.BELONGS_TO_NAME.equals(field.getName());
  }

  public static boolean isHasManyField(GrField field) {
    return field.hasModifierProperty(PsiModifier.STATIC) && DomainClassRelationsInfo.HAS_MANY_NAME.equals(field.getName());
  }

  private static void buildStrongSourceToOutEdgesMap(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                     GrField field) {
    final PsiClass sourceClass = field.getContainingClass();
    final String varName = field.getName();

    GrTypeElement type = field.getTypeElementGroovy();
    if (type == null) return;

    if (!(type instanceof GrClassTypeElement)) return;
    PsiReference targetClass = ((GrClassTypeElement)type).getReferenceElement();

    PsiElement psiClass = targetClass.resolve();
    if (!(psiClass instanceof PsiClass)) return;

    if (!GormUtils.isGormBean((PsiClass)psiClass)) {
      return;
    }

    addEdgeWithName(sourcesToOutEdges, varName, targetClass, STRONG, sourceClass);
  }

  public static void buildBelongsToSourcesToOutEdges(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                     GrField field) {
    GrExpression expression = field.getInitializerGroovy();
    if (expression == null) return;

    final PsiClass sourceClass = field.getContainingClass();
    if (sourceClass.getQualifiedName() == null) return;

    if (expression instanceof GrListOrMap) {
      //static belongsTo = [...]
      buildSourcesToOutEdgesMapFromListOrMap(sourcesToOutEdges, field.getInitializerGroovy(), BELONGS_TO, sourceClass);

    }
    else {
      //static belongsTo = Book
      findVarNameAndAddEdge(sourcesToOutEdges, expression, BELONGS_TO, sourceClass);
    }
  }

  public static void buildHasManySourcesToOutEdgesMap(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                      GrField field) {
    final PsiClass psiClass = field.getContainingClass();
    if (psiClass.getQualifiedName() == null) return;

    buildSourcesToOutEdgesMapFromListOrMap(sourcesToOutEdges, field.getInitializerGroovy(), HAS_MANY, psiClass);
  }

  private static void buildSourcesToOutEdgesMapFromListOrMap(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                                             GrExpression initializer,
                                                             Relation relation,
                                                             PsiClass sourceClass) {
    String varName;

    if (!(initializer instanceof GrListOrMap list)) return;

    GrExpression[] expressions = list.getInitializers();

    for (GrExpression expression : expressions) {
      findVarNameAndAddEdge(sourcesToOutEdges, expression, relation, sourceClass);
    }

    GrNamedArgument[] grNamedArguments = list.getNamedArguments();
    for (GrNamedArgument namedArgument : grNamedArguments) {
      //var name
      GrArgumentLabel argumentLabel = namedArgument.getLabel();
      if (argumentLabel == null) return;
      varName = argumentLabel.getName();
      if (varName == null) return;

      GrExpression grExpression = namedArgument.getExpression();
      if (grExpression == null) return;

      addEdgeWithName(sourcesToOutEdges, varName, grExpression.getReference(), relation, sourceClass);
    }
  }

  private static void findVarNameAndAddEdge(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                            GrExpression expression,
                                            Relation relation,
                                            PsiClass containingClass) {
    String varName;
    PsiReference reference = expression.getReference();
    if (reference == null) return;

    PsiElement targetClass = reference.resolve();
    if (!(targetClass instanceof PsiClass) || ((PsiClass)targetClass).getQualifiedName() == null) return;

    DomainClassNode target = new DomainClassNode((PsiClass)targetClass);
    varName = findBelongsToItemFieldName(containingClass, target.getUniqueName());
    addEdgeWithName(sourcesToOutEdges, varName, expression.getReference(), relation, containingClass);
  }

  private static void addEdgeWithName(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                      String varName,
                                      PsiReference domainClassReference,
                                      Relation relation,
                                      PsiClass sourceClass) {
    DomainClassNode source = new DomainClassNode(sourceClass);

    if (domainClassReference == null) return;

    PsiElement targetClass = domainClassReference.resolve();
    if (!(targetClass instanceof PsiClass)) return;

    DomainClassNode target = new DomainClassNode((PsiClass)targetClass);
    DomainClassRelationsInfo outEdge = new DomainClassRelationsInfo(source, target, relation);
    outEdge.setVarName(varName);

    addOutEdgeToSourceMap(sourcesToOutEdges, source, outEdge);
  }

  private static String findBelongsToItemFieldName(PsiClass typeDefinition, String type) {
    final PsiField[] fields = typeDefinition.getFields();

    for (PsiField field : fields) {
      if (field.getType().equalsToText(type)) return field.getName();
    }

    return type;
  }

  private static void addOutEdgeToSourceMap(Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges,
                                            DomainClassNode source,
                                            DomainClassRelationsInfo outEdge) {
    List<DomainClassRelationsInfo> outEdges = sourcesToOutEdges.get(source);

    if (outEdges == null) {
      outEdges = new ArrayList<>();
    }

    outEdges.add(outEdge);
    sourcesToOutEdges.put(source, outEdges);
  }

  public static boolean endsWithDomainConnectivity(String name) {
    for (String connective : DOMAIN_CONNECTIVES) {
      if (name.endsWith(connective)) return true;
    }
    return false;
  }

  public static @Nullable PsiType getDCPropertyType(PsiElement property) {
    if (property instanceof GrField) {
      return ((GrField)property).getType();
    } else if (property instanceof PsiMethod) {
      return PsiUtil.getSmartReturnType((PsiMethod)property);
    }
    else if (property instanceof GrArgumentLabel) {
      final PsiElement namedArgument = property.getParent();
      if (!(namedArgument instanceof GrNamedArgument)) return null;
      final GrExpression expression = ((GrNamedArgument)namedArgument).getExpression();
      if (!(expression instanceof GrReferenceExpression)) return null;
      final PsiElement resolved = ((GrReferenceExpression)expression).resolve();
      if (!(resolved instanceof PsiClass)) return null;
      return JavaPsiFacade.getElementFactory(property.getProject()).createType((PsiClass)resolved);
    }
    return null;
  }

  public static @Nullable FinderMethod parseFinderMethod(@NotNull String methodName) {
    String prefix = null;

    for (String p : FINDER_PREFIXES) {
      if (methodName.startsWith(p)) {
        prefix = p;
        break;
      }
    }

    if (prefix == null) return null;

    String queryText = methodName.substring(prefix.length());

    boolean saveOrCreate = prefix.equals(DOMAIN_FIND_OR_CREATE) || prefix.equals(DOMAIN_FIND_OR_SAVE);

    // See parsing of method name in DynamicFinder.createFinderInvocation()
    for (Pattern operatorPattern : (saveOrCreate ? AND_OPERATOR : OPERATOR_PATTERNS)) {
      Matcher matcher = operatorPattern.matcher(queryText);
      if (matcher.find()) {
        String operator = matcher.group(1); // 'Or' or 'And'
        String[] conditionsText = queryText.split(operator);

        Condition[] conditions = new Condition[conditionsText.length];

        for (int i = 0; i < conditionsText.length; i++) {
          Condition condition = Condition.createFromText(conditionsText[i]);
          if (condition == null) return null;

          if (saveOrCreate) {
            if (condition.isNegative() || (condition.getFinderExpr() != null && !"Equal".equals(condition.getFinderExpr()))) {
              return null; // See http://grails.org/doc/latest/ref/Domain%20Classes/findOrCreateBy.html
            }
          }

          conditions[i] = condition;
        }

        return new FinderMethod(prefix, operator, conditions);
      }
    }

    Condition condition = Condition.createFromText(queryText);
    if (condition == null) return null;

    return new FinderMethod(prefix, condition);
  }

  public static class FinderMethod {
    private final String myPrefix;

    private final Condition[] myConditions;

    private String myOperator; // All conditions must be join by same operators ('Or' or 'And')

    public FinderMethod(@NotNull String prefix, @NotNull  Condition condition) {
      myPrefix = prefix;
      myConditions = new Condition[]{condition};
    }

    public FinderMethod(@NotNull String prefix, @NotNull String operator, Condition @NotNull [] conditions) {
      assert operator.equals("Or") || operator.equals("And");
      assert conditions.length > 1;

      myPrefix = prefix;
      myConditions = conditions;
      myOperator = operator;
    }

    public String getPrefix() {
      return myPrefix;
    }

    public Condition[] getConditions() {
      return myConditions;
    }

    public String getOperator() {
      return myOperator;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(myPrefix);
      myConditions[0].appendTo(sb);
      if (myOperator != null) {
        for (int i = 1; i < myConditions.length; i++) {
          sb.append(myOperator);
          myConditions[i].appendTo(sb);
        }
      }
      else {
        assert myConditions.length == 1;
      }

      return sb.toString();
    }
  }

  public static class Condition {
    private String myFieldName;
    private boolean myNegative;
    private final String myFinderExpr;

    public Condition(@NotNull String fieldName, boolean negative, @Nullable String myFinderExpr) {
      assert !negative || myFinderExpr != null;
      myFieldName = fieldName;
      myNegative = negative;
      this.myFinderExpr = myFinderExpr;
    }

    private static @Nullable Condition createFromText(String text) {
      Matcher m = FINDER_METHOD_PART_PATTERN.matcher(text);
      if (!m.matches()) return null;

      return new Condition(m.group(1), m.group(2) != null, m.group(3));
    }

    public int getLength() {
      return myFieldName.length() + (myNegative ? 3 : 0) + (myFinderExpr == null ? 0 : myFinderExpr.length());
    }

    public @NotNull String getFieldName() {
      return myFieldName;
    }

    public void setFieldName(@NotNull String fieldName) {
      this.myFieldName = fieldName;
    }

    public boolean isNegative() {
      return myNegative;
    }

    public void setNegative(boolean negative) {
      this.myNegative = negative;
    }

    public @Nullable String getFinderExpr() {
      return myFinderExpr;
    }

    public void appendTo(StringBuilder sb) {
      sb.append(myFieldName);
      if (myNegative) sb.append("Not");
      if (myFinderExpr != null) sb.append(myFinderExpr);
    }
  }
}
