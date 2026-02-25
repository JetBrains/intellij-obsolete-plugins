// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.validation;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsValidatedObjectTest extends GrailsTestCase {
  public void testCompletionDynamicMethodsCommand() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                       com.<caret>
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                   
                                     String name;
                                   }
                                   """);

    checkCompletion(file, "validate", "hasErrors", "name");
  }

  private void addValidateableAnnotationClass() {
    addSimpleGroovyFile("""
                          package org.codehaus.groovy.grails.validation
                          @interface Validateable {}
                          """);
  }

  private void addValidateableAnnotationClass2() {
    addSimpleGroovyFile("""
                          package grails.validation
                          @interface Validateable {}
                          """);
  }

  public void testCompletionDynamicMethodsValidateable() {
    addValidateableAnnotationClass();

    PsiFile file = addSimpleGroovyFile("""
                                         @org.codehaus.groovy.grails.validation.Validateable
                                         class Ccc {
                                           String name;
                                         
                                           static {
                                             new Ccc().<caret>
                                           }
                                         }
                                         """);

    checkCompletion(file, "validate", "hasErrors", "name");
  }

  public void testCompletionDynamicMethodsValidateable2() {
    addValidateableAnnotationClass2();

    PsiFile file = addSimpleGroovyFile("""
                                         import grails.validation.Validateable
                                         
                                         @Validateable
                                         class Ccc {
                                           String name;
                                         
                                           static {
                                             new Ccc().<caret>
                                           }
                                         }
                                         """);

    checkCompletion(file, "validate", "hasErrors", "name");
  }

  public void testCompletionAttr1Command() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                   
                                     int name;
                                   
                                     static constraints = {
                                       name(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "max", "inList", "range", "notEqual");
    checkNonExistingCompletionVariants("unique", "nullable", "email", "scale");
  }

  public void testCompletionAttr1Validateable() {
    addValidateableAnnotationClass();

    PsiFile file = addSimpleGroovyFile("""
                                         @org.codehaus.groovy.grails.validation.Validateable
                                         class Ccc {
                                           int name;
                                         
                                           static constraints = {
                                             name(<caret>)
                                           }
                                         }
                                         """);

    checkCompletion(file, "max", "inList", "range", "notEqual");
    checkNonExistingCompletionVariants("unique", "nullable", "email", "scale");
  }

  public void testCompletionAttr2() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                   
                                     String sss
                                   
                                     static constraints = {
                                       sss(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "max", "inList", "range", "nullable", "email", "notEqual");
    checkNonExistingCompletionVariants("unique", "scale");
  }

  public void testCompletionAttr3() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                   
                                     Object sss
                                   
                                     static constraints = {
                                       sss(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "inList", "nullable", "notEqual");
    checkNonExistingCompletionVariants("unique", "scale", "max", "range", "email");
  }

  public void testCompletionAttr4() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                   
                                     boolean sss
                                   
                                     static constraints = {
                                       sss(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "inList", "notEqual");
    checkNonExistingCompletionVariants("unique", "scale", "max", "range", "email");
  }

  public void testCompletionFields() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                   
                                     String name;
                                     String description;
                                     int iii;
                                   
                                     static constraints = {
                                       <caret>
                                     }
                                   }
                                   """);

    checkCompletion(file, "name", "description", "iii");
  }

  public void testValidatorValueType() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = { ZzzCommand com ->
                                     }
                                   }
                                   
                                   class ZzzCommand {
                                     int iii;
                                   
                                     static constraints = {
                                       iii(validator: {val ->
                                         val.<caret>
                                       })
                                     }
                                   }
                                   """);

    checkCompletion(file, "intValue", "byteValue", "toString");
  }
}
