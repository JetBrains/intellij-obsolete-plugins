package com.intellij.guice;

import com.intellij.guice.inspections.UninstantiableBindingInspection;

public class UninstantiableBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new UninstantiableBindingInspection());
  }

  /**
   * N6: {@code to(Key.get(T, qualifier))} links to another binding; Guice does not construct {@code T} there.
   */
  public void testLinkToQualifiedKeyIsNotUninstantiable() {
    myFixture.addClass("""
      package test;
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooImpl implements Foo {}
    """);
    myFixture.addClass("""
      package test;
      public interface Bar {}
    """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Key;
      import com.google.inject.name.Names;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Foo.class).to(Key.get(Foo.class, Names.named("prod")));
          bind(Foo.class).annotatedWith(Names.named("prod")).to(FooImpl.class);
          // A link to a class that no binding of the module provides is still reported.
          bind(Bar.class).annotatedWith(Names.named("bad")).to(<warning descr="Class Bar is uninstantiable, and thus can not be bound">Bar</warning>.class);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
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

  /**
   * A target that Guice resolves without a constructor is not uninstantiable: a type with
   * {@code @ImplementedBy} or {@code @ProvidedBy}, or a type that the same module binds to a target.
   */
  public void testTargetsResolvedByJitAnnotationsOrOtherBindingsAreNotReported() {
    myFixture.addClass("package test; public interface Foo {}");
    myFixture.addClass("package test; @com.google.inject.ImplementedBy(BarImpl.class) public interface Bar extends Foo {}");
    myFixture.addClass("package test; public class BarImpl implements Bar {}");
    myFixture.addClass("package test; @com.google.inject.ProvidedBy(BazProvider.class) public interface Baz extends Foo {}");
    myFixture.addClass("""
      package test;
      public class BazProvider implements javax.inject.Provider<Baz> {
        public Baz get() { return null; }
      }
      """);
    myFixture.addClass("package test; public interface Qux extends Foo {}");
    myFixture.addClass("""
      package test;
      public class QuxProvider implements javax.inject.Provider<Qux> {
        public Qux get() { return null; }
      }
      """);
    myFixture.addClass("package test; public interface Plain extends Foo {}");

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Foo.class).to(Bar.class); // Valid: @ImplementedBy
          bind(Foo.class).to(Baz.class); // Valid: @ProvidedBy
          bind(Foo.class).to(Qux.class); // Valid: bound below
          bind(Qux.class).toProvider(QuxProvider.class);
          bind(Foo.class).to(<warning descr="Class Plain is uninstantiable, and thus can not be bound">Plain</warning>.class);
        }
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * Guice uses a private no-argument constructor only when the class itself is private.
   */
  public void testPrivateNoArgConstructorIsUninstantiable() {
    myFixture.addClass("package test; public interface Foo {}");
    myFixture.addClass("""
      package test;
      public class Hidden implements Foo {
        private Hidden() {}
      }
      """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import test.*;
      public class MyModule extends AbstractModule {
        private static class PrivateImpl implements Foo {
          private PrivateImpl() {}
        }
        @Override
        protected void configure() {
          bind(Foo.class).to(<warning descr="Class Hidden is uninstantiable, and thus can not be bound">Hidden</warning>.class);
          bind(Foo.class).to(PrivateImpl.class); // Valid: a private class may use its private constructor
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
        bind<Foo>().to<<warning descr="Class AbstractFoo is uninstantiable, and thus can not be bound">AbstractFoo</warning>>()
        bind<Foo>().to<<warning descr="Class UninstantiableFoo is uninstantiable, and thus can not be bound">UninstantiableFoo</warning>>()
        
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
