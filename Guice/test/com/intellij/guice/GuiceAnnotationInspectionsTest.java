// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice;

import com.intellij.guice.inspections.BindingAnnotationWithoutInjectInspection;
import com.intellij.guice.inspections.ConflictingAnnotationsInspection;
import com.intellij.guice.inspections.InterceptionAnnotationWithoutRuntimeRetentionInspection;
import com.intellij.guice.inspections.InvalidImplementedByInspection;
import com.intellij.guice.inspections.InvalidProvidedByInspection;
import com.intellij.guice.inspections.InvalidRequestParametersInspection;
import com.intellij.guice.inspections.MultipleBindingAnnotationsInspection;
import com.intellij.guice.inspections.SessionScopedInjectsRequestScopedInspection;
import com.intellij.guice.inspections.SingletonInjectsScopedInspection;
import com.intellij.guice.inspections.UninstantiableImplementedByClassInspection;
import com.intellij.guice.inspections.UninstantiableProvidedByClassInspection;

public class GuiceAnnotationInspectionsTest extends GuiceTestBase {

  public void testConflictingAnnotationsWithJavaxAndJakartaSingletonAndKotlin() {
    myFixture.enableInspections(new ConflictingAnnotationsInspection());
    myFixture.addClass("""
      package com.google.inject.servlet;
      import java.lang.annotation.*;
      import com.google.inject.ScopeAnnotation;
      @Target({ElementType.TYPE, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      @ScopeAnnotation
      public @interface SessionScoped {}
      """);
    myFixture.addClass("""
      package com.google.inject.servlet;
      import java.lang.annotation.*;
      import com.google.inject.ScopeAnnotation;
      @Target({ElementType.TYPE, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      @ScopeAnnotation
      public @interface RequestScoped {}
      """);

    myFixture.configureByText("JavaxConflict.java", """
      import javax.inject.Singleton;
      import com.google.inject.servlet.SessionScoped;
      <warning descr="Annotation @Singleton conflicts with other declared annotations">@Singleton</warning>
      <warning descr="Annotation @SessionScoped conflicts with other declared annotations">@SessionScoped</warning>
      public class JavaxConflict {}
      """);
    myFixture.testHighlighting(true, false, true);

    myFixture.configureByText("JakartaConflict.java", """
      import jakarta.inject.Singleton;
      import com.google.inject.servlet.RequestScoped;
      <warning descr="Annotation @Singleton conflicts with other declared annotations">@Singleton</warning>
      <warning descr="Annotation @RequestScoped conflicts with other declared annotations">@RequestScoped</warning>
      public class JakartaConflict {}
      """);
    myFixture.testHighlighting(true, false, true);

    myFixture.configureByText("KotlinConflict.kt", """
      import com.google.inject.ImplementedBy
      import com.google.inject.ProvidedBy
      import com.google.inject.Singleton
      import com.google.inject.servlet.RequestScoped

      <warning descr="Annotation @Singleton conflicts with other declared annotations">@Singleton</warning>
      <warning descr="Annotation @RequestScoped conflicts with other declared annotations">@RequestScoped</warning>
      class KotlinScopeConflict

      <warning descr="Annotation @ImplementedBy(MyServiceImpl::class) conflicts with other declared annotations">@ImplementedBy(MyServiceImpl::class)</warning>
      <warning descr="Annotation @ProvidedBy(MyServiceProvider::class) conflicts with other declared annotations">@ProvidedBy(MyServiceProvider::class)</warning>
      interface KotlinTypeConflict : MyService
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testSingletonInjectsScopedWithJavaxJakartaAndKotlin() {
    myFixture.enableInspections(new SingletonInjectsScopedInspection());
    myFixture.addClass("""
      package com.google.inject.servlet;
      import java.lang.annotation.*;
      import com.google.inject.ScopeAnnotation;
      @Target({ElementType.TYPE, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      @ScopeAnnotation
      public @interface RequestScoped {}
      """);
    myFixture.addClass("""
      package test;
      import com.google.inject.servlet.RequestScoped;
      @RequestScoped
      public class ReqScopedBean {}
      """);

    myFixture.configureByText("JakartaSingletonBean.java", """
      import jakarta.inject.Inject;
      import jakarta.inject.Singleton;
      import test.ReqScopedBean;
      @Singleton
      public class JakartaSingletonBean {
        @Inject <warning descr="@Inject of scoped class ReqScopedBean from @Singleton class">ReqScopedBean</warning> field;
        @Inject
        public JakartaSingletonBean(<warning descr="@Inject of scoped class ReqScopedBean from @Singleton class">ReqScopedBean</warning> param) {}
      }
      """);
    myFixture.testHighlighting(true, false, true);

    myFixture.configureByText("KotlinSingletonBean.kt", """
      import com.google.inject.Inject
      import com.google.inject.Singleton
      import test.ReqScopedBean
      @Singleton
      class KotlinSingletonBean @Inject constructor(
        val param: <warning descr="@Inject of scoped class ReqScopedBean from @Singleton class">ReqScopedBean</warning>
      ) {
        @Inject
        lateinit var prop: <warning descr="@Inject of scoped class ReqScopedBean from @Singleton class">ReqScopedBean</warning>
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testSessionScopedInjectsRequestScopedInKotlin() {
    myFixture.enableInspections(new SessionScopedInjectsRequestScopedInspection());
    myFixture.addClass("""
      package com.google.inject.servlet;
      import java.lang.annotation.*;
      import com.google.inject.ScopeAnnotation;
      @Target({ElementType.TYPE, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      @ScopeAnnotation
      public @interface SessionScoped {}
      """);
    myFixture.addClass("""
      package com.google.inject.servlet;
      import java.lang.annotation.*;
      import com.google.inject.ScopeAnnotation;
      @Target({ElementType.TYPE, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      @ScopeAnnotation
      public @interface RequestScoped {}
      """);
    myFixture.addClass("""
      package test;
      import com.google.inject.servlet.RequestScoped;
      @RequestScoped
      public class ReqScopedBean {}
      """);

    myFixture.configureByText("KotlinSessionBean.kt", """
      import com.google.inject.Inject
      import com.google.inject.servlet.SessionScoped
      import test.ReqScopedBean
      @SessionScoped
      class KotlinSessionBean @Inject constructor(
        val param: <warning descr="@Inject of @RequestScoped class ReqScopedBean from @SessionScoped class">ReqScopedBean</warning>
      ) {
        @Inject
        lateinit var prop: <warning descr="@Inject of @RequestScoped class ReqScopedBean from @SessionScoped class">ReqScopedBean</warning>
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testBindingAnnotationWithoutInjectInKotlin() {
    myFixture.enableInspections(new BindingAnnotationWithoutInjectInspection());

    myFixture.configureByText("KotlinMissingInject.kt", """
      import com.google.inject.AbstractModule
      import com.google.inject.Inject
      import com.google.inject.Provides
      import com.google.inject.name.Named

      class KotlinMissingInject @Inject constructor(
        @Named("okParam") okParam: String
      ) {
        <warning descr="Binding annotation @Named(\\"missingOnProp\\") without @Inject declared">@Named("missingOnProp")</warning>
        var missingProp: String = ""

        @Inject
        @Named("okProp")
        lateinit var okProp: String

        fun regularMethod(<warning descr="Binding annotation @Named(\\"missingOnParam\\") without @Inject declared">@Named("missingOnParam")</warning> s: String) {}
      }

      class KotlinModule : AbstractModule() {
        @Provides
        fun provideString(@Named("okProvidesParam") dep: String): String = dep
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testMultipleBindingAnnotationsInKotlin() {
    myFixture.enableInspections(new MultipleBindingAnnotationsInspection());
    myFixture.addClass("""
      package test;
      import java.lang.annotation.*;
      import javax.inject.Qualifier;
      @Qualifier
      @Retention(RetentionPolicy.RUNTIME)
      public @interface CustomQualifier {}
      """);

    myFixture.configureByText("KotlinMultipleQualifiers.kt", """
      import com.google.inject.Inject
      import com.google.inject.name.Named
      import test.CustomQualifier

      class KotlinMultipleQualifiers @Inject constructor(
        @Named("a") @CustomQualifier <warning descr="Variable badParam has multiple binding annotations">badParam</warning>: String,
        @Named("ok") okParam: String
      ) {
        @Inject
        @Named("a")
        @CustomQualifier
        lateinit var <warning descr="Variable badProp has multiple binding annotations">badProp</warning>: String
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testImplementedByAndProvidedByInspectionsInKotlin() {
    myFixture.enableInspections(
      new InvalidImplementedByInspection(),
      new InvalidProvidedByInspection(),
      new UninstantiableImplementedByClassInspection(),
      new UninstantiableProvidedByClassInspection()
    );
    myFixture.addClass("package test; public class Unrelated { public Unrelated() {} }");
    myFixture.addClass("""
      public class WrongKeyProvider implements javax.inject.Provider<MyKey> {
        public MyKey get() { return null; }
      }
      """);

    myFixture.configureByText("KotlinContracts.kt", """
      import com.google.inject.ImplementedBy
      import com.google.inject.ProvidedBy
      import test.Unrelated

      @ImplementedBy(<warning descr="Class Unrelated doesn't implement annotated class">Unrelated</warning>::class)
      interface BadImplementedBy

      @ProvidedBy(<warning descr="Class WrongKeyProvider doesn't provide annotated class">WrongKeyProvider</warning>::class)
      interface BadProvidedBy

      @ImplementedBy(<warning descr="Class AbstractImpl is uninstantiable, and thus can not be @ImplementedBy">AbstractImpl</warning>::class)
      interface UninstantiableImplContract
      abstract class AbstractImpl : UninstantiableImplContract

      @ProvidedBy(<warning descr="Class AbstractProvider is uninstantiable, and thus can not be @ProvidedBy">AbstractProvider</warning>::class)
      interface UninstantiableProviderContract
      abstract class AbstractProvider : javax.inject.Provider<UninstantiableProviderContract>
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInterceptionAnnotationWithoutRuntimeRetentionInKotlin() {
    myFixture.enableInspections(new InterceptionAnnotationWithoutRuntimeRetentionInspection());
    myFixture.addClass("""
      package test;
      import java.lang.annotation.*;
      @Retention(RetentionPolicy.CLASS)
      public @interface NonRuntimeAnno {}
      """);
    myFixture.addClass("""
      package test;
      import java.lang.annotation.*;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface RuntimeAnno {}
      """);

    myFixture.configureByText("KotlinInterceptorModule.kt", """
      import com.google.inject.AbstractModule
      import com.google.inject.matcher.Matchers
      import test.NonRuntimeAnno
      import test.RuntimeAnno

      class KotlinInterceptorModule : AbstractModule() {
        override fun configure() {
          Matchers.annotatedWith(<warning descr="Annotation NonRuntimeAnno::class.java does not have Runtime retention">NonRuntimeAnno::class.java</warning>)
          Matchers.annotatedWith(RuntimeAnno::class.java)
        }
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testInvalidRequestParametersInJavaAndKotlin() {
    myFixture.enableInspections(new InvalidRequestParametersInspection());
    myFixture.addClass("""
      package com.google.inject.servlet;
      import java.lang.annotation.*;
      import com.google.inject.BindingAnnotation;
      @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface RequestParameters {}
      """);

    myFixture.configureByText("JavaServletBean.java", """
      import com.google.inject.Inject;
      import com.google.inject.servlet.RequestParameters;
      import java.util.Map;

      public class JavaServletBean {
        @Inject
        <warning descr="Variables or parameters labeled @RequestParameters must have type Map<String, String[]>">@RequestParameters</warning>
        Map<String, String> badField;

        @Inject
        @RequestParameters
        Map<String, String[]> okField;

        @Inject
        public JavaServletBean(
          <warning descr="Variables or parameters labeled @RequestParameters must have type Map<String, String[]>">@RequestParameters</warning> Map<String, String> badParam,
          @RequestParameters Map<String, String[]> okParam
        ) {}
      }
      """);
    myFixture.testHighlighting(true, false, true);

    myFixture.configureByText("KotlinServletBean.kt", """
      import com.google.inject.Inject
      import com.google.inject.servlet.RequestParameters

      class KotlinServletBean @Inject constructor(
        <warning descr="Variables or parameters labeled @RequestParameters must have type Map<String, String[]>">@RequestParameters</warning> badParam: Map<String, String>,
        @RequestParameters okParam: Map<String, Array<String>>
      ) {
        @Inject
        <warning descr="Variables or parameters labeled @RequestParameters must have type Map<String, String[]>">@RequestParameters</warning>
        lateinit var badProp: Map<String, String>

        @Inject
        @RequestParameters
        lateinit var okProp: Map<String, Array<String>>
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testMultipleInjectedConstructorsInJavaAndKotlin() {
    myFixture.enableInspections(new com.intellij.guice.inspections.MultipleInjectedConstructorsForClassInspection());

    myFixture.configureByText("JavaMultiCtor.java", """
      import com.google.inject.Inject;
      public class JavaMultiCtor {
        @Inject
        public <warning descr="Class JavaMultiCtor has multiple @Inject constructors">JavaMultiCtor</warning>(MyService s) {}
        @Inject
        public <warning descr="Class JavaMultiCtor has multiple @Inject constructors">JavaMultiCtor</warning>(MyKey k) {}
      }
      """);
    myFixture.testHighlighting(true, false, true);

    myFixture.configureByText("KotlinMultiCtor.kt", """
      import com.google.inject.Inject
      class <warning descr="Class KotlinMultiCtor has multiple @Inject constructors">KotlinMultiCtor</warning> @Inject constructor(s: MyService) {
        @Inject
        <warning descr="Class constructor has multiple @Inject constructors">constructor</warning>(k: MyKey) : this(MyServiceImpl())
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }
}

