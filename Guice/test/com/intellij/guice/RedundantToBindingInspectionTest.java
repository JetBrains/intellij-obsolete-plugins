package com.intellij.guice;

import com.intellij.guice.inspections.RedundantToBindingInspection;

public class RedundantToBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new RedundantToBindingInspection());
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
          bind(Bar.class).to(<warning descr="Binding to class Bar is redundant">Bar</warning>.class);
          bind(Foo.class).to(<warning descr="Binding to class FooImpl is redundant">FooImpl</warning>.class);
          
          bind(Foo.class).to(SpecialFoo.class); // Valid and not redundant
          bind(MyService.class).to(MyServiceImpl.class); // Valid
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * {@code to()} on a multibinder or optional-binder element binds no key of its own,
   * so it is never redundant, even when the element type has {@code @ImplementedBy}.
   */
  /**
   * N6: {@code to(Key.get(T, qualifier))} links to another binding of the same type. It is not a self-binding,
   * and the "Remove binding target" fix would break the injector.
   */
  public void testLinkToQualifiedKeyOfSameTypeIsNotRedundant() {
    myFixture.addClass("""
      package test;
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooImpl implements Foo {}
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
          bind(FooImpl.class).to(<warning descr="Binding to class FooImpl is redundant">FooImpl</warning>.class);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testBinderElementBindingIsNotRedundant() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ImplementedBy(FooImpl.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooImpl implements Foo {}
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.Multibinder;
      import com.google.inject.multibindings.OptionalBinder;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), Foo.class).addBinding().to(FooImpl.class); // Valid
          OptionalBinder.newOptionalBinder(binder(), Foo.class).setDefault().to(FooImpl.class); // Valid
          OptionalBinder.newOptionalBinder(binder(), Foo.class).setBinding().to(FooImpl.class); // Valid
          bind(Foo.class).to(<warning descr="Binding to class FooImpl is redundant">FooImpl</warning>.class);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * A qualified key is a different key: linking it to the unqualified type or to the
   * {@code @ImplementedBy} class is a real binding.
   */
  public void testQualifiedKeyBindingIsNotRedundant() {
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

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Key;
      import com.google.inject.name.Names;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Key.get(Foo.class, Names.named("x"))).to(FooImpl.class); // Valid
          bind(Key.get(Bar.class, Names.named("x"))).to(Bar.class); // Valid
          bind(Key.get(Bar.class)).to(<warning descr="Binding to class Bar is redundant">Bar</warning>.class);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * Guice reads {@code @ImplementedBy} on the bound type only, not on its supertypes.
   */
  public void testImplementedByOnSupertypeIsNotInherited() {
    myFixture.addClass("""
      package test;
      @com.google.inject.ImplementedBy(FooImpl.class)
      public interface Foo {}
    """);
    myFixture.addClass("""
      package test;
      public interface SubFoo extends Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class FooImpl implements SubFoo {}
    """);
    myFixture.addClass("""
      package test;
      @com.google.inject.ImplementedBy(BaseImpl.class)
      public abstract class Base {}
    """);
    myFixture.addClass("""
      package test;
      public class BaseImpl extends Base {}
    """);
    myFixture.addClass("""
      package test;
      public class Derived extends Base {}
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(SubFoo.class).to(FooImpl.class); // Valid: SubFoo has no @ImplementedBy of its own
          bind(Derived.class).to(<warning descr="Binding to class Derived is redundant">Derived</warning>.class); // Self-binding
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
          bind(Bar::class.java).to(<warning descr="Binding to class Bar is redundant">Bar</warning>::class.java)
          bind(Foo::class.java).to(<warning descr="Binding to class FooImpl is redundant">FooImpl</warning>::class.java)

          // Reified generics extension
          bind<Bar>().to<<warning descr="Binding to class Bar is redundant">Bar</warning>>()
          bind<Foo>().to<<warning descr="Binding to class FooImpl is redundant">FooImpl</warning>>()

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
        bind(Bar.class).to(B<caret>ar.class);
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Remove binding target");
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
        bind(Bar::class.java).to(B<caret>ar::class.java)
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Remove binding target");
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
        bind<Bar>().to<B<caret>ar>()
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Remove binding target");
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
