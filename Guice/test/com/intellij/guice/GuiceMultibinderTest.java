package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.psi.PsiFile;
import java.util.Set;

public class GuiceMultibinderTest extends GuiceTestBase {

  public void testJavaMultibinderChained() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.Multibinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), MyService.class).addBinding().to(MyServiceImpl.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    // Because they share the same outermost qualified source expression,
    // BindToDescriptor gets deduplicated in the Set by SetMultibindDescriptor
    // due to equals() checking only the outermostSource SmartPsiElementPointer.
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(SetMultibindDescriptor.class);
    assertEquals("MyService", ((SetMultibindDescriptor) binding).getElementType().getQualifiedName());
  }

  public void testJavaMultibinderLocalVar() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.Multibinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder<MyService> binder = Multibinder.newSetBinder(binder(), MyService.class);
          binder.addBinding().to(MyServiceImpl.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    // Because they are in separate statements, they are NOT deduplicated.
    assertEquals(2, bindings.size());
    
    boolean foundMultibinder = false;
    boolean foundBinding = false;
    for (BindDescriptor b : bindings) {
      if (b instanceof SetMultibindDescriptor) {
        foundMultibinder = true;
        assertEquals("MyService", ((SetMultibindDescriptor) b).getElementType().getQualifiedName());
      } else if (b instanceof BindToDescriptor) {
        foundBinding = true;
        // BUT because it was defined using a local variable,
        // it fails to walk up the receiver chain and resolve the bound class!
        assertNull(b.getBoundClass());
        assertEquals("MyServiceImpl", ((BindToDescriptor) b).getBindingClass().getQualifiedName());
      }
    }
    assertTrue(foundMultibinder);
    assertTrue(foundBinding);
  }

  public void testJavaOptionalBinder() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.OptionalBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          OptionalBinder.newOptionalBinder(binder(), MyService.class).setDefault().to(MyServiceImpl.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(OptionalBindDescriptor.class);
    assertEquals("MyService", ((OptionalBindDescriptor) binding).getBoundClass().getQualifiedName());
  }

  public void testJavaMapBinder() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.MapBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          MapBinder.newMapBinder(binder(), MyKey.class, MyService.class).addBinding(null).to(MyServiceImpl.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(MapMultibindDescriptor.class);
    assertEquals("MyKey", ((MapMultibindDescriptor) binding).getKeyType().getQualifiedName());
    assertEquals("MyService", ((MapMultibindDescriptor) binding).getValueType().getQualifiedName());
  }
}
