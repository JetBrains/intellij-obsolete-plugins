package com.intellij.seam.model.references;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.AndFilter;
import com.intellij.psi.filters.PsiMethodCallFilter;
import com.intellij.psi.filters.ScopeFilter;
import com.intellij.psi.filters.XmlTagFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.converters.jam.SeamContextVariableReferenceProvider;
import com.intellij.seam.converters.jam.SeamJamAnnotationParameterReferenceProvider;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.string;

public class SeamReferenceContributor extends PsiReferenceContributor {
  @NonNls public static final String VALIDATOR_ID_ATTR_NAME = "validatorId";
  @NonNls public static final String CONVERTER_ID_ATTR_NAME = "converterId";

  @Override
  public void registerReferenceProviders(@NotNull final PsiReferenceRegistrar registrar) {
    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{VALIDATOR_ID_ATTR_NAME}, new ScopeFilter(
      new ParentElementFilter(new AndFilter(XmlTagFilter.INSTANCE, new NamespaceFilter(XmlUtil.JSF_CORE_URIS)), 2)),
                                                       new SeamValidatorReferenceProvider());

    //XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{CONVERTER_ID_ATTR_NAME}, new ScopeFilter(
    //  new ParentElementFilter(new AndFilter(XmlTagFilter.INSTANCE, new NamespaceFilter(XmlUtil.JSF_CORE_URI)), 2)),
    //                                                   new SeamConverterRefertenceProvider());


    // @In and @Out
    registrar.registerReferenceProvider(
      literalExpression().annotationParam(string().oneOf(SeamAnnotationConstants.IN_ANNOTATION, SeamAnnotationConstants.OUT_ANNOTATION), "value"),
      new SeamContextVariableReferenceProvider());

     //@Validator and @Converter
    registrar.registerReferenceProvider(
      literalExpression().annotationParam(SeamAnnotationConstants.JSF_VALIDATOR_ANNOTATION, "id"),
      new SeamJamAnnotationParameterReferenceProvider());

    registrar.registerReferenceProvider(
      literalExpression().annotationParam(SeamAnnotationConstants.JSF_CONVERTER_ANNOTATION, "id"),
      new SeamJamAnnotationParameterReferenceProvider());

    //@Observer("eventType") and Events.raiseEvent("eventType")
    final ElementPattern methodCall = new FilterPattern(
      new PsiMethodCallFilter(SeamEventTypeReferenceProvider.SEAM_EVENTS_CLASSNAME, SeamEventTypeReferenceProvider.METHODS));
    registrar.registerReferenceProvider(literalExpression().withSuperParent(2, methodCall), new SeamEventTypeReferenceProvider());

    registrar.registerReferenceProvider(
      literalExpression().insideAnnotationParam(SeamAnnotationConstants.OBSERVER_ANNOTATION),
      new SeamObserverEventTypeReferenceProvider());


    registrar.registerReferenceProvider(
      literalExpression().annotationParam(SeamAnnotationConstants.RAISE_EVENT_ANNOTATION, "value"),
      new SeamRaiseEventRefernceProvider());

  }
}
