package com.intellij.guice;

import com.intellij.guice.inspections.UninstantiableBindingInspection;

public class UninstantiableBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new UninstantiableBindingInspection());
  }

  public void testJavaUninstantiableBinding() {
    myFixture.addClass("""
      package test;
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public abstract class AbstractFoo implements Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class ConcreteFoo implements Foo {
        public ConcreteFoo() {}
      }
    """);
    myFixture.addClass("""
      package test;
      public class UninstantiableFoo implements Foo {
        public UninstantiableFoo(String name) {}
      }
    """);

    myFixture.configureByText("MyModule.java", """
import com.google.inject.AbstractModule;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Foo.class).to(<warning descr="Class AbstractFoo is uninstantiable, and thus can not be bound">AbstractFoo</warning>.class);
        bind(Foo.class).to(<warning descr="Class UninstantiableFoo is uninstantiable, and thus can not be bound">UninstantiableFoo</warning>.class);
        
        bind(Foo.class).to(ConcreteFoo.class); // Valid
    }
}
""");
    myFixture.testHighlighting(true, false, true);
  }

  public void testKotlinUninstantiableBinding() {
    myFixture.addClass("""
      package test;
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public abstract class AbstractFoo implements Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class ConcreteFoo implements Foo {
        public ConcreteFoo() {}
      }
    """);
    myFixture.addClass("""
      package test;
      public class UninstantiableFoo implements Foo {
        public UninstantiableFoo(String name) {}
      }
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
        bind(Foo::class.java).to(<warning descr="Class AbstractFoo is uninstantiable, and thus can not be bound">AbstractFoo</warning>::class.java)
        bind(Foo::class.java).to(<warning descr="Class UninstantiableFoo is uninstantiable, and thus can not be bound">UninstantiableFoo</warning>::class.java)
        
        // Reified generics extension
        bind<Foo>().<warning descr="Class to<AbstractFoo>() is uninstantiable, and thus can not be bound">to<AbstractFoo>()</warning>
        bind<Foo>().<warning descr="Class to<UninstantiableFoo>() is uninstantiable, and thus can not be bound">to<UninstantiableFoo>()</warning>
        
        bind(Foo::class.java).to(ConcreteFoo::class.java) // Valid
        bind<Foo>().to<ConcreteFoo>() // Valid
    }
}
""");
    myFixture.testHighlighting(true, false, true);
  }

  public void testCheckedProvidesAndProvidesIntoOptional() {
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      public interface CheckedProvider<T> {}
      """);
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      import java.lang.annotation.*;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface CheckedProvides {
        Class<? extends CheckedProvider> value();
      }
      """);
    myFixture.addClass("package test; public interface Foo {}");
    myFixture.addClass("package test; public abstract class AbstractChecked implements Foo {}");
    myFixture.addClass("package test; public abstract class AbstractOptional implements Foo {}");
    myFixture.addClass("""
      package test;
      public interface TestCheckedProvider extends com.google.inject.throwingproviders.CheckedProvider<AbstractChecked> {}
      """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.ProvidesIntoOptional;
      import com.google.inject.throwingproviders.CheckedProvides;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Foo.class).to(<warning descr="Class AbstractChecked is uninstantiable, and thus can not be bound">AbstractChecked</warning>.class);
          bind(Foo.class).to(AbstractOptional.class);
        }

        @CheckedProvides(TestCheckedProvider.class)
        AbstractChecked provideChecked() { return null; }

        @ProvidesIntoOptional(ProvidesIntoOptional.Type.DEFAULT)
        AbstractOptional provideOptional() { return null; }
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }
}
