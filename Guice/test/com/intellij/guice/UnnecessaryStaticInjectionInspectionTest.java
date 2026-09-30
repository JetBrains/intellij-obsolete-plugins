package com.intellij.guice;

import com.intellij.guice.inspections.UnnecessaryStaticInjectionInspection;

public class UnnecessaryStaticInjectionInspectionTest extends GuiceTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new UnnecessaryStaticInjectionInspection());
    myFixture.addClass("""
      public class NoStatics {
        @com.google.inject.Inject String name;
      }
      """);
    myFixture.addClass("""
      public class WithStatics {
        @com.google.inject.Inject static String name;
      }
      """);
  }

  public void testHighlighting() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          requestStaticInjection(<warning descr="Class NoStatics has no static @Inject members">NoStatics</warning>.class, WithStatics.class);
        }
      }
      """);
    myFixture.testHighlighting(true, false, true);
  }

  public void testQuickFixDeletesOnlyTheReportedArgument() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          requestStaticInjection(NoStat<caret>ics.class, WithStatics.class);
        }
      }
      """);
    myFixture.launchAction(myFixture.findSingleIntention("Delete binding"));
    myFixture.checkResult("""
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          requestStaticInjection(WithStatics.class);
        }
      }
      """);
  }

  public void testQuickFixDeletesTheLastArgument() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          requestStaticInjection(WithStatics.class, NoStat<caret>ics.class);
        }
      }
      """);
    myFixture.launchAction(myFixture.findSingleIntention("Delete binding"));
    myFixture.checkResult("""
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          requestStaticInjection(WithStatics.class);
        }
      }
      """);
  }

  public void testQuickFixDeletesTheStatementForTheOnlyArgument() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          requestStaticInjection(NoStat<caret>ics.class);
        }
      }
      """);
    myFixture.launchAction(myFixture.findSingleIntention("Delete binding"));
    myFixture.checkResult("""
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
      """);
  }

  public void testKotlinQuickFixDeletesOnlyTheReportedArgument() {
    myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
      class MyModule : AbstractModule() {
        override fun configure() {
          requestStaticInjection(NoStat<caret>ics::class.java, WithStatics::class.java)
        }
      }
      """);
    myFixture.launchAction(myFixture.findSingleIntention("Delete binding"));
    myFixture.checkResult("""
      import com.google.inject.AbstractModule
      class MyModule : AbstractModule() {
        override fun configure() {
          requestStaticInjection(WithStatics::class.java)
        }
      }
      """);
  }
}
