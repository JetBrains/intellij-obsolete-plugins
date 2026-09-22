package com.intellij.guice;

import com.intellij.guice.inspections.RedundantToBindingInspection;

public class RedundantToBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new RedundantToBindingInspection());
    
    // Add ImplementedBy and ProvidedBy annotations
    myFixture.addClass("""
      package com.google.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.TYPE)
      public @interface ImplementedBy {
        Class<?> value();
      }
      """);
    myFixture.addClass("""
      package com.google.inject;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      import java.lang.annotation.Target;
      import java.lang.annotation.ElementType;
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.TYPE)
      public @interface ProvidedBy {
        Class<? extends javax.inject.Provider<?>> value();
      }
      """);
  }

  public void testJavaRedundantToBinding() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ImplementedBy(FooImpl.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooImpl implements Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);
    myFixture.addClass("""
      package test;
      public class SpecialFoo implements Foo {}
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          <warning descr="Binding to class bind(Bar.class).to(Bar.class) is redundant">bind(Bar.class).to(Bar.class)</warning>;
          <warning descr="Binding to class bind(Foo.class).to(FooImpl.class) is redundant">bind(Foo.class).to(FooImpl.class)</warning>;
          
          bind(Foo.class).to(SpecialFoo.class); // Valid and not redundant
          bind(MyService.class).to(MyServiceImpl.class); // Valid
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testKotlinRedundantToBinding() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ImplementedBy(FooImpl.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooImpl implements Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);

    myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
      import com.google.inject.binder.LinkedBindingBuilder
      import test.*
    
      // Mock Kotlin extensions
      inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
      inline fun <reified T> LinkedBindingBuilder<in T>.to(): Unit = TODO()
    
      class MyModule : AbstractModule() {
        override fun configure() {
          // Class literal
          bind(Bar::class.java).<warning descr="Binding to class to(Bar::class.java) is redundant">to(Bar::class.java)</warning>
          bind(Foo::class.java).<warning descr="Binding to class to(FooImpl::class.java) is redundant">to(FooImpl::class.java)</warning>

          // Reified generics extension
          bind<Bar>().<warning descr="Binding to class to<Bar>() is redundant">to<Bar>()</warning>
          bind<Foo>().<warning descr="Binding to class to<FooImpl>() is redundant">to<FooImpl>()</warning>

          bind(MyService::class.java).to(MyServiceImpl::class.java) // Valid
          bind<MyService>().to<MyServiceImpl>() // Valid
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testJavaRedundantToBindingQuickFix() {
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);

    myFixture.configureByText("MyModule.java", """
import com.google.inject.AbstractModule;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Bar.class).t<caret>o(Bar.class);
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Delete binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
import com.google.inject.AbstractModule;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Bar.class);
    }
}
""");
  }

  public void testKotlinRedundantToBindingQuickFixValueArg() {
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind(Bar::class.java).t<caret>o(Bar::class.java)
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Delete binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind(Bar::class.java)
    }
}
""");
  }

  public void testKotlinRedundantToBindingQuickFixTypeArg() {
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
inline fun <reified T> LinkedBindingBuilder<in T>.to(): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind<Bar>().t<caret>o<Bar>()
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Delete binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
inline fun <reified T> LinkedBindingBuilder<in T>.to(): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind<Bar>()
    }
}
""");
  }
}
