package com.intellij.aiplayground.tests.gherkin

import com.intellij.aiplayground.tests.annotations.GherkinTest
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import org.junit.runners.ParentRunner
import java.io.File
import java.net.JarURLConnection

class GherkinTestRunner(testClass: Class<*>) : ParentRunner<String>(testClass) {

  override fun getChildren(): List<String>? {
    return testClass.getAnnotation(GherkinTest::class.java)?.let { annotation ->
      listOf(annotation.value)
    }
  }

  override fun describeChild(child: String): Description? {
    return Description.createTestDescription(testClass.javaClass, child)
  }

  override fun runChild(featureFile: String, notifier: RunNotifier) {
    val testInstance = testClass.onlyConstructor.newInstance() as GherkinTestBase
    val featureDescription = describeChild(featureFile)

    notifier.fireTestStarted(featureDescription)
    try {
      testInstance.setUp()
      testInstance.executeFeature(featureFile)
      notifier.fireTestFinished(featureDescription)
    }
    catch (e: Throwable) {
      when (e) {
        // Handle different types of failures appropriately
        is AssertionError -> {
          // Test assertion failed - report as test failure
          notifier.fireTestFailure(Failure(featureDescription, e))
        }
        is IllegalStateException -> {
          // Configuration or setup error - report as test error
          notifier.fireTestFailure(Failure(featureDescription, e))
        }
        else -> {
          // Unexpected error - report as test error
          val error = AssertionError("Unexpected error executing feature: $featureFile", e)
          notifier.fireTestFailure(Failure(featureDescription, error))
        }
      }
      notifier.fireTestFinished(featureDescription)
    }
    finally {
      // Report overall test suite completion
      notifier.fireTestSuiteFinished(featureDescription)
      testInstance.tearDown()
    }
  }

  private fun findFeatureFiles(): List<String> {
    val featureFiles = mutableSetOf<String>()
    val classLoader = testClass.javaClass.classLoader

    // Search paths to look for feature files
    val searchPaths = listOf(
      "spec/features",
      "tests/spec/features",
      "features"
    )

    // First try to load from classpath
    for (path in searchPaths) {
      try {
        println("Searching in classpath path: $path")
        val resources = classLoader.getResources(path)
        while (resources.hasMoreElements()) {
          val url = resources.nextElement()

          println("Found resource URL: $url")
          when (url.protocol) {
            "file" -> {
              // Handle directory-based resources
              val directory = File(url.toURI())
              println("Checking directory: ${directory.absolutePath} (exists: ${directory.exists()}, isDir: ${directory.isDirectory})")
              if (directory.exists() && directory.isDirectory) {
                directory.walk()
                  .filter { it.isFile && it.name.endsWith(".feature") }
                  .forEach {
                    println("Found feature file: ${it.absolutePath}")
                    featureFiles.add(it.absolutePath)
                  }
              }
            }
            "jar" -> {
              // Handle JAR-based resources
              println("Checking JAR URL: $url")
              val connection = url.openConnection() as JarURLConnection
              val jar = connection.jarFile
              jar.entries().asSequence()
                .filter { it.name.endsWith(".feature") }
                .forEach {
                  println("Found feature file in JAR: ${it.name}")
                  featureFiles.add(it.name)
                }
            }
          }
        }

        // Also try direct resource lookup for feature files
        val featurePattern = "$path/*.feature"
        println("Trying direct resource lookup with pattern: $featurePattern")
        val directResources = classLoader.getResources(featurePattern)
        while (directResources.hasMoreElements()) {
          val url = directResources.nextElement()
          println("Found direct feature file: $url")
          featureFiles.add(url.toString())
        }
      }
      catch (e: Exception) {
        // Log the error but continue searching
        println("Error searching path $path: ${e.message}")
        e.printStackTrace()
      }
    }

    println("Found feature files: ${featureFiles.joinToString(", ")}")
    return featureFiles.toList()
  }
}