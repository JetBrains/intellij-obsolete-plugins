/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.groovy.grails.action

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import org.jetbrains.plugins.groovy.grails.GrailsTestCase

class DomainFieldIntentionsTest : GrailsTestCase() {

  fun testInnerClass() {
    val file = addDomain("""
class City {

  String name;

  private class InnerClass {
    String nam<caret>e;
  }
}
""")

    runIntention(file, "Make property nullable", false)
  }

  fun testTransientsField() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static transients = ["name"]
}
""")

    runIntention(file, "Make property nullable", false)
  }

  fun testConstraintsNotExists1() {
    val file = addDomain("""
class City {

    String nam<caret>e;
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

    String name;

    static constraints = {
        name(nullable: true)
    }
}
""")
  }

  fun testConstraintsNotExists2() {
    val file = addDomain("""
class City {

    String nam<caret>e;

}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

    String name;

    static constraints = {
        name(nullable: true)
    }
}
""")
  }

  fun testConstraintsWithoutInitializer() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
      name(nullable: true)
  }
}
""")
  }

  fun testConstraintsNull() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = null
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
      name(nullable: true)
  }
}
""")
  }

  fun testConstraintsWithInvalidInitializer() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = "!!!!!!"
}
""")

    runIntention(file, "Make property nullable", false)
  }

  fun testConstraintsEmpty() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = { }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
      name(nullable: true)
  }
}
""")
  }

  private fun checkResult(text: String) {
    ApplicationManager.getApplication().runWriteAction {
      PostprocessReformattingAspect.getInstance(project).doPostponedFormatting()
    }

    myFixture.checkResult(text)
  }

  fun testConstraintsHasFieldDescription() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name(nullable: true)
  }
}
""")
  }

  fun testNullableAlreadyExists() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: true)
  }
}
""")

    runIntention(file, "Make property nullable", false)
  }

  fun testNonNullable() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: false)
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name(nullable: true)
  }
}
""")
  }

  fun testInvalidValue() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: 1 + 2)
  }
}
""")

    runIntention(file, "Make property nullable", false)
  }

  fun testMakeNullableOnPrimitiveField() {
    val file = addDomain("""
class City {

  int siz<caret>e;

}
""")

    runIntention(file, "Make property nullable", false)
  }

  fun testAppStatement1() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name nullable: false
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name nullable: true
  }
}
""")
  }

  fun testAppStatement2() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name asdasdasd: 4
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name nullable: true, asdasdasd: 4
  }
}
""")
  }

  fun testCreation1() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name(nullable:, size: 4)
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name(nullable: true, size: 4)
  }
}
""")
  }

  fun testCreation2() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: false, size: 4)
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name(nullable: true, size: 4)
  }
}
""")
  }

  fun testCreation3() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name(size: 4, nullable: )
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name(size: 4, nullable: true)
  }
}
""")
  }

  fun testCreation4() {
    val file = addDomain("""
class City {

  String nam<caret>e;
  String description;

  static constraints = {
      description(nullable: false)
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class City {

  String name;
  String description;

  static constraints = {
      description(nullable: false)
      name(nullable: true)
  }
}
""")
  }

  fun testMakeUnique() {
    val file = addDomain("""
class City {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""")

    runIntention(file, "Make property unique", true)

    checkResult("""
class City {

  String name;

  static constraints = {
    name(unique: true)
  }
}
""")
  }

  fun testMakeUniqueInCommand() {
    val file = addController("""
class CccController {
  def index = { ZzzCommand com ->
  }
}

class ZzzCommand {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""")

    runIntention(file, "Make property unique", false)
  }

  fun testMakeNullableInCommand() {
    val file = addController("""
class CccController {
  def index = { ZzzCommand com ->
  }
}

class ZzzCommand {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""")

    runIntention(file, "Make property nullable", true)

    checkResult("""
class CccController {
  def index = { ZzzCommand com ->
  }
}

class ZzzCommand {

  String name;

  static constraints = {
    name(nullable: true)
  }
}
""")
  }

  fun testUniqueIsNotApplicable() {
    val file = addDomain("""
class City {

  Set stree<caret>t;

}
""")

    runIntention(file, "Make property unique", false)
  }

}