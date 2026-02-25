// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.seachable;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public class GrailsSearchableMemberProvider extends MemberProvider {

  public static final Object METHOD_MARKER = "grails:plugins:searchable:seachmethod";
  
  // See DynamicDomainMethodUtils.attachDynamicMethods()
  private static final String CLASS_SOURCE = "class SearchableMethodsSource<D> {" +
                                             "  public static def search(String query)" +
                                             "  public static def search(String query, Map options)" +
                                             "  public static def search(Map options, String query)" +
                                             "  public static def search(Closure builder)" +
                                             "  public static def search(Closure builder, Map options)" +
                                             "  public static def search(Map options, Closure builder)" +

                                             "  public static Integer countHits(String query)" +
                                             "  public static Integer countHits(String query, Map options)" +
                                             "  public static Integer countHits(Map options, String query)" +
                                             "  public static Integer countHits(Closure builder)" +
                                             "  public static Integer countHits(Closure builder, Map options)" +
                                             "  public static Integer countHits(Map options, Closure builder)" +

                                             "  public static D[] searchEvery(String query)" +
                                             "  public static D[] searchEvery(String query, Map options)" +
                                             "  public static D[] searchEvery(Map options, String query)" +
                                             "  public static D[] searchEvery(Closure builder)" +
                                             "  public static D[] searchEvery(Closure builder, Map options)" +
                                             "  public static D[] searchEvery(Map options, Closure builder)" +

                                             "  public static D searchTop(String query)" +
                                             "  public static D searchTop(String query, Map options)" +
                                             "  public static D searchTop(Map options, String query)" +
                                             "  public static D searchTop(Closure builder)" +
                                             "  public static D searchTop(Closure builder, Map options)" +
                                             "  public static D searchTop(Map options, Closure builder)" +

                                             "  public def moreLikeThis()" +
                                             "  public def moreLikeThis(Map options)" +
                                             "  public def moreLikeThis(LinkedHashMap options)" +
                                             "  public static def moreLikeThis(Map options)" +
                                             "  public static def moreLikeThis(LinkedHashMap options)" +
                                             "  public static def moreLikeThis(Serializable id)" +
                                             "  public static def moreLikeThis(Serializable id, Map options)" +
                                             "  public static def moreLikeThis(D domainClass)" +
                                             "  public static def moreLikeThis(D domainClass, Map options)" +

                                             "  public static String suggestQuery(String query)" +
                                             "  public static String suggestQuery(String query, Map options)" +
                                             "  public static String suggestQuery(Map options, String query)" +

                                             "  public static org.compass.core.CompassTermFreq[] termFreqs()" +
                                             "  public static org.compass.core.CompassTermFreq[] termFreqs(String ... propertyNames)" +
                                             "  public static org.compass.core.CompassTermFreq[] termFreqs(Map options, String ... propertyNames)" +
                                             "  public static org.compass.core.CompassTermFreq[] termFreqs(Map options)" +

                                             "  public def index()" +
                                             "  public static def index()" +
                                             "  public static def index(D ... instances)" +
                                             "  public static def index(Serializable ... instances)" +

                                             "  public def unindex()" +
                                             "  public static def unindex()" +
                                             "  public static def unindex(D ... instances)" +
                                             "  public static def unindex(Serializable ... instances)" +

                                             "  public def reindex()" +
                                             "  public static def reindex()" +
                                             "  public static def reindex(D ... instances)" +
                                             "  public static def reindex(Serializable ... instances)" +

                                             "  public static def indexAll(Object ... params)" +
                                             "  public static def unindexAll(Object ... params)" +
                                             "  public static def reindexAll(Object ... params)" +
                                             "}";

  @Override
  public void processMembers(PsiScopeProcessor processor, final PsiClass psiClass, GrReferenceExpression ref) {
    PsiField searchable = psiClass.findFieldByName("searchable", false);
    if (searchable == null || !searchable.hasModifierProperty(PsiModifier.STATIC)) return;

    GrailsStructure structure = GrailsStructure.getInstance(psiClass);
    if (structure == null || !structure.isPluginInstalled("searchable")) return;

    DelegatingScopeProcessor delegateProcessor = new DelegatingScopeProcessor(processor) {
      
      private PsiSubstitutor mySubstitutor;

      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (!(element instanceof PsiMethod)) return true;

        if (mySubstitutor == null) {
          mySubstitutor = PsiSubstitutor.EMPTY.putAll(((DynamicMemberUtils.DynamicElement)element).getSourceClass(), new PsiType[]{PsiTypesUtil.getClassType(psiClass)});
        }

        GrLightMethodBuilder lightMethod = GrailsPsiUtil.substitute((PsiMethod)element, mySubstitutor);

        lightMethod.setMethodKind(METHOD_MARKER);

        lightMethod.setData(psiClass);

        return super.execute(lightMethod, state);
      }
    };

    DynamicMemberUtils.process(delegateProcessor, psiClass, ref, CLASS_SOURCE);
  }

}
