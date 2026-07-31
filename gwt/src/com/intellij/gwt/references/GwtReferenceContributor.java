package com.intellij.gwt.references;

import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.i18n.GwtPropertyReference;
import com.intellij.gwt.junit.GwtJUnitConstants;
import com.intellij.gwt.module.index.GwtHtmlUtil;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.gwt.rpc.GwtServletUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.javaee.web.CommonServlet;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.*;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ProcessingContext;
import com.intellij.xml.util.HtmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.*;
import static com.intellij.patterns.XmlPatterns.*;

public final class GwtReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(final @NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(literalExpression().annotationParam(GwtI18nUtil.KEY_ANNOTATION_CLASS, "value"), new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(final @NotNull PsiElement element, final @NotNull ProcessingContext context) {
        if (element instanceof PsiLiteralExpression) {
          Object value = ((PsiLiteralExpression)element).getValue();
          if (value instanceof String) {
            PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (psiClass != null) {
              return new PsiReference[]{new GwtPropertyReference((String)value, element, psiClass)};
            }
          }
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    XmlAttributeValuePattern inheritsTag =
        xmlAttributeValue(xmlAttribute("name").withParent(xmlTag().withLocalName("inherits").withParent(xmlTag().withLocalName("module")
            .inFile(psiFile().withName(string().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX))))));
    ElementPattern<? extends PsiElement> returnInTestCase =
        literalExpression().withParent(psiReturnStatement().insideMethod("getModuleName", GwtJUnitConstants.GWT_TEST_CASE_CLASS));
    registrar.registerReferenceProvider(inheritsTag, new GwtModuleReferencesProvider(false), PsiReferenceRegistrar.HIGHER_PRIORITY);
    registrar.registerReferenceProvider(returnInTestCase, new GwtModuleReferencesProvider(false), PsiReferenceRegistrar.HIGHER_PRIORITY);

    registrar.registerReferenceProvider(
        literalExpression().and(psiExpression().methodCallParameter(0, psiMethod().withName("get")
            .definedInClass("com.google.gwt.user.client.ui.RootPanel"))), new GwtToHtmlReferencesProvider());

    StringPattern styleMethods = string().oneOf("addStyleName", "removeStyleName", "setStyleName", "setStylePrimaryName");
    registrar.registerReferenceProvider(
        literalExpression().andOr(
          psiExpression().methodCallParameter(0, psiMethod().withName(styleMethods).definedInClass(GwtClassNames.UI_OBJECT_CLASS)),
          psiExpression().methodCallParameter(1, psiMethod().withName(string().oneOf("setStyleName", "setStylePrimaryName")).definedInClass(GwtClassNames.UI_OBJECT_CLASS)),
          psiExpression().methodCallParameter(2, psiMethod().withName(styleMethods).definedInClass("com.google.gwt.user.client.ui.HTMLTable.CellFormatter"))
        ), new GwtToCssClassReferenceProvider(), PsiReferenceRegistrar.HIGHER_PRIORITY);

    final PsiMethodPattern setEntryPointMethodPattern =
      psiMethod().withName("setServiceEntryPoint").definedInClass("com.google.gwt.user.client.rpc.ServiceDefTarget");
    final PsiReferenceProvider servletPathReferenceProvider = new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(final @NotNull PsiElement element, final @NotNull ProcessingContext context) {
        if (element instanceof PsiLiteralExpression) {
          Object value = ((PsiLiteralExpression)element).getValue();
          if (value instanceof String) {
            return new PsiReference[]{new GwtServletPathReference((String)value, (PsiLiteralExpression)element)};
          }
        }
        return PsiReference.EMPTY_ARRAY;
      }
    };
    registrar.registerReferenceProvider(
      literalExpression()
        .withParent(psiBinaryExpression().operation(PsiJavaPatterns.psiElement(JavaTokenType.PLUS))
                     .and(psiExpression().methodCallParameter(0, setEntryPointMethodPattern))), servletPathReferenceProvider);

    registrar.registerReferenceProvider(literalExpression().annotationParam(RemoteServiceUtil.SERVICE_PATH_ANNOTATION_NAME, "value"),
                                        servletPathReferenceProvider, PsiReferenceRegistrar.DEFAULT_PRIORITY + 10);

    final ElementPattern<? extends PsiElement> srcAttributePattern =
        xmlAttributeValue(xmlAttribute("src").withParent(xmlTag().withLocalName(HtmlUtil.SCRIPT_TAG_NAME))).withLanguage(
          HTMLLanguage.INSTANCE);
    registrar.registerReferenceProvider(srcAttributePattern, new GeneratedJsReferenceProvider(), PsiReferenceRegistrar.DEFAULT_PRIORITY + 1);
    registrar.registerReferenceProvider(GwtHtmlUtil.createMetaValuePattern(), new GwtModuleReferencesProvider(true));
  }

  private static class GwtServletPathReference extends BaseGwtReference<PsiLiteralExpression> {
    private final String myValue;

    GwtServletPathReference(final String value, final PsiLiteralExpression element) {
      super(element);
      myValue = value;
    }

    @Override
    public PsiElement resolve() {
      final GwtModule gwtModule = findGwtModule();
      if (gwtModule != null) {
        final GwtVersion version = getGwtVersion(gwtModule);
        if (version != null && version.isHostedModeRequiresWebXml()) {
          final CommonServlet servlet = GwtServletUtil.findServletByPath(gwtModule, myValue);
          if (servlet != null) {
            return servlet.getXmlTag();
          }
        }
        else {
          for (GwtServlet servlet : gwtModule.getServlets()) {
            final String path = servlet.getPath().getValue();
            if (path != null && (path.equals(myValue) || path.equals("/" + myValue))) {
              return servlet.getXmlTag();
            }
          }
        }
      }
      return null;
    }

    @Override
    public boolean isSoft() {
      return true;
    }

    @Override
    public Object @NotNull [] getVariants() {
      final GwtModule module = findGwtModule();
      if (module != null) {
        final GwtVersion version = getGwtVersion(module);
        if (version != null && version.isHostedModeRequiresWebXml()) {
          final List<String> paths = GwtServletUtil.getAllGwtServletsPaths(module);
          return ArrayUtilRt.toStringArray(paths);
        }
        else {
          List<String> paths = new ArrayList<>();
          for (GwtServlet servlet : module.getServlets()) {
            final String path = servlet.getPath().getValue();
            if (path != null) {
              paths.add(StringUtil.trimStart(path, "/"));
            }
          }
          return ArrayUtilRt.toStringArray(paths);
        }
      }
      return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }
  }
}
