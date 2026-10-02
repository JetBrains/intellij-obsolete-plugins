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
    var action = myFixture.findSingleIntention("Delete binding");
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
    var action = myFixture.findSingleIntention("Delete binding");
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
    var action = myFixture.findSingleIntention("Delete binding");
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
