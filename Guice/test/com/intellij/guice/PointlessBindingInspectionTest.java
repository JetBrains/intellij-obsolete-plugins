package com.intellij.guice;

import com.intellij.guice.inspections.PointlessBindingInspection;

public class PointlessBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new PointlessBindingInspection());
  }

  public void testJavaPointlessBinding() {
    myFixture.addClass("""
      public class Foo {
        public Foo() {}
      }
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          <warning descr="Binding is unnecessary, and can be removed">bind(Foo.class)</warning>;
          bind(MyService.class).to(MyServiceImpl.class); // Valid
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testKotlinPointlessBinding() {
    myFixture.addClass("""
      public class Foo {
        public Foo() {}
      }
    """);

    myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
    
      // Mock Kotlin extensions for Guice
      inline fun <reified T> AbstractModule.bind() {}
      
      class MyModule : AbstractModule() {
        override fun configure() {
          <warning descr="Binding is unnecessary, and can be removed">bind(Foo::class.java)</warning>
          <warning descr="Binding is unnecessary, and can be removed">bind<Foo>()</warning>
          
          bind(MyService::class.java).to(MyServiceImpl::class.java) // Valid
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testJavaPointlessBindingQuickFix() {
    myFixture.addClass("""
      public class Foo {
        public Foo() {}
      }
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Foo.cl<caret>ass);
        }
      }
    """);
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Delete binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
    """);
  }

  public void testKotlinPointlessBindingQuickFixValueArg() {
    myFixture.addClass("""
      public class Foo {
        public Foo() {}
      }
    """);

    myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
      
      inline fun <reified T> AbstractModule.bind() {}
      
      class MyModule : AbstractModule() {
        override fun configure() {
          bind(Foo::cl<caret>ass.java)
        }
      }
    """);
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Delete binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
      import com.google.inject.AbstractModule
      
      inline fun <reified T> AbstractModule.bind() {}
      
      class MyModule : AbstractModule() {
        override fun configure() {
        }
      }
    """);
  }

  public void testKotlinPointlessBindingQuickFixTypeArg() {
    myFixture.addClass("""
      public class Foo {
        public Foo() {}
      }
    """);

    myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
      
      inline fun <reified T> AbstractModule.bind() {}
      
      class MyModule : AbstractModule() {
        override fun configure() {
          bind<Fo<caret>o>()
        }
      }
    """);
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Delete binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
      import com.google.inject.AbstractModule
      
      inline fun <reified T> AbstractModule.bind() {}
      
      class MyModule : AbstractModule() {
        override fun configure() {
        }
      }
    """);
  }
}
