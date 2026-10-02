package com.intellij.guice;

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

    // The multibinder and its .to() tail share the call expression. Both descriptors stay.
    SetMultibindDescriptor multibinder = findDescriptor(file, SetMultibindDescriptor.class);
    assertEquals("MyService", multibinder.getElementType().getQualifiedName());
    assertEquals("MyServiceImpl", findDescriptor(file, BindToDescriptor.class).getBindingClass().getQualifiedName());
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
      } else {
        foundBinding = true;
        assertEquals("MyService", b.getBoundClass().getQualifiedName());
        assertEquals("MyServiceImpl", b.getBindingClass().getQualifiedName());
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

    assertEquals("MyService", findDescriptor(file, OptionalBindDescriptor.class).getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", findDescriptor(file, BindToDescriptor.class).getBindingClass().getQualifiedName());
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

    MapMultibindDescriptor mapBinder = findDescriptor(file, MapMultibindDescriptor.class);
    assertEquals("MyKey", mapBinder.getKeyType().getQualifiedName());
    assertEquals("MyService", mapBinder.getValueType().getQualifiedName());
    assertEquals("MyServiceImpl", findDescriptor(file, BindToDescriptor.class).getBindingClass().getQualifiedName());
  }

  private static <T extends BindDescriptor> T findDescriptor(PsiFile file, Class<T> descriptorClass) {
    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(2, bindings.size());
    for (BindDescriptor binding : bindings) {
      if (descriptorClass.isInstance(binding)) return descriptorClass.cast(binding);
    }
    throw new AssertionError("No " + descriptorClass.getSimpleName() + " in " + bindings);
  }
}
