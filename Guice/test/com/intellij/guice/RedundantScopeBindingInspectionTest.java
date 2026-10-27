package com.intellij.guice;

import com.intellij.guice.inspections.RedundantScopeBindingInspection;

public class RedundantScopeBindingInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new RedundantScopeBindingInspection());
  }

  public void testJavaRedundantScopeBinding() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);

    myFixture.configureByText("MyModule.java", """
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Foo.class).in(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>);
        
        bind(Bar.class).in(Scopes.SINGLETON); // Valid (Bar has no @Singleton)
    }
}
""");
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * Guice applies a scope annotation of the class it constructs. For a linked binding that is the
   * implementation class, not the bound type; a scope annotation on an abstract bound type is never applied.
   */
  public void testLinkedBindingScopeIsCheckedOnImplementation() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public interface AnnotatedIface {}
    """);
    myFixture.addClass("""
      package test;
      public class UnscopedImpl implements AnnotatedIface {}
    """);
    myFixture.addClass("""
      package test;
      public interface PlainIface {}
    """);
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class ScopedImpl implements PlainIface {}
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Scopes;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(AnnotatedIface.class).to(UnscopedImpl.class).in(Scopes.SINGLETON); // Valid: the impl has no scope
          bind(PlainIface.class).to(ScopedImpl.class).in(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * The scope of a provider binding scopes the provided instances. Neither a scope annotation on the
   * bound type nor one on the provider class makes {@code in()} redundant.
   */
  public void testProviderBindingScopeIsNotRedundant() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Foo {}
    """);
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class FooProvider implements javax.inject.Provider<Foo> {
        public Foo get() { return null; }
      }
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Scopes;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Foo.class).toProvider(FooProvider.class).in(Scopes.SINGLETON); // Valid
          bind(Foo.class).in(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  /**
   * Scope annotations are not inherited: {@code @Singleton} on a superclass does not scope the subclass.
   */
  public void testInheritedScopeAnnotationIsNotRedundant() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Base {}
    """);
    myFixture.addClass("""
      package test;
      public class Sub extends Base {}
    """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Scopes;
      import test.*;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Sub.class).in(Scopes.SINGLETON); // Valid
          bind(Base.class).in(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>);
        }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testKotlinLinkedBindingScopeIsCheckedOnImplementation() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public interface AnnotatedIface {}
    """);
    myFixture.addClass("""
      package test;
      public class UnscopedImpl implements AnnotatedIface {}
    """);
    myFixture.addClass("""
      package test;
      public interface PlainIface {}
    """);
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class ScopedImpl implements PlainIface {}
    """);

    myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
      import com.google.inject.Scopes
      import test.*

      class MyModule : AbstractModule() {
          override fun configure() {
              bind(AnnotatedIface::class.java).to(UnscopedImpl::class.java).`in`(Scopes.SINGLETON) // Valid
              bind(PlainIface::class.java).to(ScopedImpl::class.java).`in`(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>)
          }
      }
    """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testKotlinRedundantScopeBinding() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Foo {}
    """);
    myFixture.addClass("""
      package test;
      public class Bar {}
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.Scopes
import test.*

// Mock Kotlin extensions
inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
fun LinkedBindingBuilder<*>.`in`(scope: com.google.inject.Scope): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind(Foo::class.java).`in`(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>)
        
        bind<Foo>().`in`(<warning descr="Binding to scope Scopes.SINGLETON is redundant">Scopes.SINGLETON</warning>)
        
        bind(Bar::class.java).`in`(Scopes.SINGLETON) // Valid
        bind<Bar>().`in`(Scopes.SINGLETON) // Valid
    }
}
""");
    myFixture.testHighlighting(true, false, true);
  }

  public void testJavaRedundantScopeBindingQuickFix() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Foo {}
    """);

    myFixture.configureByText("MyModule.java", """
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Foo.class).in(Scopes.SING<caret>LETON);
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Remove scope binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import test.*;
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Foo.class);
    }
}
""");
  }

  public void testKotlinRedundantScopeBindingQuickFixValueArg() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Foo {}
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.Scopes
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind(Foo::class.java).`in`(Scopes.SING<caret>LETON)
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Remove scope binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.Scopes
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind(Foo::class.java)
    }
}
""");
  }

  public void testKotlinRedundantScopeBindingQuickFixTypeArg() {
    myFixture.addClass("""
      package test;
      @com.google.inject.Singleton
      public class Foo {}
    """);

    myFixture.configureByText("MyModule.kt", """
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.Scopes
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
inline fun LinkedBindingBuilder<*>.`in`(scope: com.google.inject.Scope): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind<Foo>().`in`(Scopes.SING<caret>LETON)
    }
}
""");
    myFixture.doHighlighting();
    var action = myFixture.findSingleIntention("Remove scope binding");
    myFixture.launchAction(action);
    myFixture.checkResult("""
import com.google.inject.AbstractModule
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.Scopes
import test.*

inline fun <reified T> AbstractModule.bind(): LinkedBindingBuilder<T> = TODO()
inline fun LinkedBindingBuilder<*>.`in`(scope: com.google.inject.Scope): Unit = TODO()

class MyModule : AbstractModule() {
    override fun configure() {
        bind<Foo>()
    }
}
""");
  }
}
