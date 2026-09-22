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
        <warning descr="Class bind(Foo.class).to(AbstractFoo.class) is uninstantiable, and thus can not be bound">bind(Foo.class).to(AbstractFoo.class)</warning>;
        <warning descr="Class bind(Foo.class).to(UninstantiableFoo.class) is uninstantiable, and thus can not be bound">bind(Foo.class).to(UninstantiableFoo.class)</warning>;
        
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
        bind(Foo::class.java).<warning descr="Class to(AbstractFoo::class.java) is uninstantiable, and thus can not be bound">to(AbstractFoo::class.java)</warning>
        bind(Foo::class.java).<warning descr="Class to(UninstantiableFoo::class.java) is uninstantiable, and thus can not be bound">to(UninstantiableFoo::class.java)</warning>
        
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
}
