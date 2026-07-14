package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.codeInsight.daemon.GutterMark;
import java.util.List;
import java.util.Objects;

public class GuiceLineMarkerTest extends GuiceTestBase {

  public void testKotlinConstructorGutter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
      """);

    var clientFile = myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      class Client @Inject constructor(private val service: MyService)
      """);

    myFixture.configureByText("AnotherClass.java", """
      import com.google.inject.Inject;
      public class AnotherClass {
        @Inject private Client client;
      }
      """);

    myFixture.openFileInEditor(clientFile.getVirtualFile());

    myFixture.doHighlighting();
    var gutters = myFixture.findAllGutters();

    List<String> tooltips = gutters.stream()
        .map(GutterMark::getTooltipText)
        .filter(Objects::nonNull)
        .toList();

    assertThat(tooltips).contains("Navigate to injection points");
  }

  public void testJavaConstructorGutter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
      """);

    var clientFile = myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        private MyService service;
        @Inject
        public Client(MyService service) {
          this.service = service;
        }
      }
      """);

    myFixture.configureByText("AnotherClass.java", """
      import com.google.inject.Inject;
      public class AnotherClass {
        @Inject private Client client;
      }
      """);

    myFixture.openFileInEditor(clientFile.getVirtualFile());
    myFixture.doHighlighting();
    var gutters = myFixture.findAllGutters();

    List<String> tooltips = gutters.stream()
        .map(GutterMark::getTooltipText)
        .filter(Objects::nonNull)
        .toList();

    assertThat(tooltips).contains("Navigate to injection points");
  }
}
