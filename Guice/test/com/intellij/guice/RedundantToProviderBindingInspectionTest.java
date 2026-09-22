package com.intellij.guice;

import com.intellij.guice.inspections.RedundantToProviderBindingInspection;

public class RedundantToProviderBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new RedundantToProviderBindingInspection());
    
    // Add ProvidedBy annotation
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

  public void testJavaRedundantToProviderBinding() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ProvidedBy(FooProvider.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);
    myFixture.addClass("""
      package test;
      public class BarProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);
    myFixture.configureByText("MyModule.java", """
import com.google.inject.AbstractModule;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        <warning descr="Binding to class bind(Foo.class).toProvider(FooProvider.class) is redundant">bind(Foo.class).toProvider(FooProvider.class)</warning>;
        
        bind(Foo.class).toProvider(BarProvider.class); // Valid (different provider)
        bind(MyService.class).toProvider(MyServiceProvider.class); // Valid
    }
}
""");
    myFixture.testHighlighting(true, false, true);
  }

  public void testKotlinRedundantToProviderBinding() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ProvidedBy(FooProvider.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);
    myFixture.addClass("""
      package test;
      public class BarProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);
    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

// Mock Kotlin extensions
inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
inline fun <reified P : javax.inject.Provider<*>> LinkedBindingBuilder<*>.toProvider(): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        // Class literal
        bind(Foo::class.java).<warning descr="Binding to class toProvider(FooProvider::class.java) is redundant">toProvider(FooProvider::class.java)</warning>
        
        // Reified generics extension
        bind<Foo>().<warning descr="Binding to class toProvider<FooProvider>() is redundant">toProvider<FooProvider>()</warning>
        
        bind(Foo::class.java).toProvider(BarProvider::class.java) // Valid (different provider)
        bind<Foo>().toProvider<BarProvider>() // Valid (different provider)
    }
}
""");
    myFixture.testHighlighting(true, false, true);
  }

  public void testJavaRedundantToProviderBindingQuickFix() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ProvidedBy(FooProvider.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);

    myFixture.configureByText("MyModule.java", """
import com.google.inject.AbstractModule;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Foo.class).toProv<caret>ider(FooProvider.class);
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
        bind(Foo.class);
    }
}
""");
  }

  public void testKotlinRedundantToProviderBindingQuickFixValueArg() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ProvidedBy(FooProvider.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind(Foo::class.java).toProv<caret>ider(FooProvider::class.java)
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
        bind(Foo::class.java)
    }
}
""");
  }

  public void testKotlinRedundantToProviderBindingQuickFixTypeArg() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ProvidedBy(FooProvider.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
inline fun <reified P : javax.inject.Provider<*>> LinkedBindingBuilder<*>.toProvider(): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind<Foo>().toProv<caret>ider<FooProvider>()
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
inline fun <reified P : javax.inject.Provider<*>> LinkedBindingBuilder<*>.toProvider(): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind<Foo>()
    }
}
""");
  }
}
