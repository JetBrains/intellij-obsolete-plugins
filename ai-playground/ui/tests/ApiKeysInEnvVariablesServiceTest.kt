package com.intellij.aiplayground.ui.env

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.aiplayground.ui.env.ApiKeysInEnvVariablesService.NotificationEvent
import com.intellij.execution.RunManager
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.common.withEnvVars
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

/**
 * Test for ApiKeysInEnvVariablesService to verify notification is shown only once per project.
 *
 * Tests the full project lifecycle: open -> use service -> close -> reopen -> verify persistence
 */
@TestApplication
class ApiKeysInEnvVariablesServiceTest {
  private val tempPath = tempPathFixture()
  private val projectFixture = projectFixture(tempPath, openAfterCreation = true)
  private val moduleFixture = projectFixture.moduleFixture(tempPath, addPathToSourceRoot = true)

  @AfterEach
  fun tearDown() {
    service<PlaygroundSettings>().showStartupNotification = true
  }

  @Test
  fun `should not show startup notification if there is no api keys`() = runTest(timeout = 10.seconds) {
    moduleFixture.get()
    val project = projectFixture.get()
    val service = project.service<ApiKeysInEnvVariablesService>()
    service.notificationEvents.test {
      assertNoNotification()
    }
  }

  @Test
  fun `should not detect api keys from system environment variables`() = runTest(timeout = 10.seconds) {
    withEnvVars("OPENAI_API_KEY" to "test_key") {
      moduleFixture.get()
      val project = projectFixture.get()
      val service = project.service<ApiKeysInEnvVariablesService>()
      service.notificationEvents.test {
        assertNoNotification()
      }
    }
  }

  @Test
  fun `should show startup notification initially for fresh project`() = runTest(timeout = 10.seconds) {
    Files.writeString(tempPath.get().resolve(".env"), "OPENAI_API_KEY=test_key")
    moduleFixture.get()
    val project = projectFixture.get()
    val service = project.service<ApiKeysInEnvVariablesService>()

    service.notificationEvents.test {
      assertNotificationShown()
    }
  }

  @Test
  fun `should not show notification when project reopens after being shown`(
    @TestDisposable disposable: Disposable,
  ) = runTest(timeout = 10.seconds) {
    val projectPath = tempPath.get()
    Files.writeString(projectPath.resolve(".env"), "OPENAI_API_KEY=test_key")

    moduleFixture.get()
    val project1 = projectFixture.get()
    val service1 = project1.service<ApiKeysInEnvVariablesService>()

    service1.notificationEvents.test {
      assertNotificationShown()
      service1.markNotificationAsShown()
    }

    saveAndCloseProject(project1)
    val project2 = openProject(projectPath, disposable)

    assertNotSame(project1, project2, "Should be different project instances")

    val service2 = project2.service<ApiKeysInEnvVariablesService>()
    assertNotSame(service1, service2, "Should be different service instances")
    service2.notificationEvents.test {
      assertNoNotification()
    }
  }

  @Test
  fun `should hide notification permanently when user clicks do not show again`() = runTest(timeout = 10.seconds) {
    Files.writeString(tempPath.get().resolve(".env"), "OPENAI_API_KEY=test_key")
    moduleFixture.get()
    val project = projectFixture.get()
    val service = project.service<ApiKeysInEnvVariablesService>()

    service.notificationEvents.test {
      assertNotificationShown()
      service.doNotShowAgain()
      assertNotificationHidden()
    }
  }

  @Test
  fun `should persist do not show again setting across project reopens`(
    @TestDisposable disposable: Disposable,
  ) = runTest(timeout = 10.seconds) {
    val projectPath = tempPath.get()
    Files.writeString(projectPath.resolve(".env"), "OPENAI_API_KEY=test_key")

    moduleFixture.get()
    val project1 = projectFixture.get()
    val service1 = project1.service<ApiKeysInEnvVariablesService>()

    service1.notificationEvents.test {
      assertNotificationShown()
      service1.doNotShowAgain()
      assertNotificationHidden()
    }

    saveAndCloseProject(project1)
    val project2 = openProject(projectPath, disposable)

    val service2 = project2.service<ApiKeysInEnvVariablesService>()
    service2.notificationEvents.test {
      assertNoNotification()
    }
  }

  @Test
  fun `should ignore attempts to re-enable notification after user clicked do not show again`(
    @TestDisposable disposable: Disposable,
  ) = runTest(timeout = 10.seconds) {
    val projectPath = tempPath.get()
    Files.writeString(projectPath.resolve(".env"), "OPENAI_API_KEY=test_key")

    moduleFixture.get()
    val project1 = projectFixture.get()
    val service1 = project1.service<ApiKeysInEnvVariablesService>()

    service1.doNotShowAgain()

    saveAndCloseProject(project1)
    val project2 = openProject(projectPath, disposable)

    val service2 = project2.service<ApiKeysInEnvVariablesService>()
    service2.notificationEvents.test {
      assertNoNotification()
    }
  }

  @Test
  fun `should hide startup notification when user cancels it`() = runTest(timeout = 10.seconds) {
    Files.writeString(tempPath.get().resolve(".env"), "OPENAI_API_KEY=test_key")
    moduleFixture.get()
    val project = projectFixture.get()
    val service = project.service<ApiKeysInEnvVariablesService>()

    service.notificationEvents.test {
      assertNotificationShown()
      service.cancelStartupNotification()
      assertNotificationHidden()
    }
  }

  @Test
  fun `should persist startup notification cancellation across sessions`(
    @TestDisposable disposable: Disposable,
  ) = runTest(timeout = 10.seconds) {
    val projectPath = tempPath.get()
    Files.writeString(projectPath.resolve(".env"), "OPENAI_API_KEY=test_key")

    moduleFixture.get()
    val project1 = projectFixture.get()
    val service1 = project1.service<ApiKeysInEnvVariablesService>()

    service1.cancelStartupNotification()

    saveAndCloseProject(project1)
    val project2 = openProject(projectPath, disposable)

    val service2 = project2.service<ApiKeysInEnvVariablesService>()
    service2.notificationEvents.test {
      assertNoNotification()
    }
  }

  @Test
  fun `should hide startup notification when user clicks do not show again`() = runTest(timeout = 10.seconds) {
    Files.writeString(tempPath.get().resolve(".env"), "OPENAI_API_KEY=test_key")
    moduleFixture.get()
    val project = projectFixture.get()
    val service = project.service<ApiKeysInEnvVariablesService>()
    service.notificationEvents.test {
      assertNotificationShown()
      service.doNotShowAgain()
      assertNotificationHidden()
    }
  }

  @Test
  fun `should persist do not show again for startup notification across sessions`(
    @TestDisposable disposable: Disposable,
  ) = runTest(timeout = 10.seconds) {
    val projectPath = tempPath.get()
    Files.writeString(projectPath.resolve(".env"), "OPENAI_API_KEY=test_key")

    moduleFixture.get()
    val project1 = projectFixture.get()
    val service1 = project1.service<ApiKeysInEnvVariablesService>()

    service1.doNotShowAgain()

    saveAndCloseProject(project1)
    val project2 = openProject(projectPath, disposable)

    val service2 = project2.service<ApiKeysInEnvVariablesService>()
    service2.notificationEvents.test {
      assertNoNotification()
    }
  }

  private suspend fun saveAndCloseProject(project1: Project) {
    withContext(Dispatchers.EDT) {
      ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(project1, save = true)
    }
  }

  private suspend fun openProject(projectPath: Path, disposable: Disposable): Project {
    val project = withContext(Dispatchers.EDT) {
      ProjectManagerEx.getInstanceEx().openProject(projectPath, OpenProjectTask())!!
    }

    RunManager.getInstanceAsync(project)
    Disposer.register(disposable) {
      runBlocking(Dispatchers.EDT) {
        ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(project, save = false)
      }
    }
    return project
  }

  private suspend fun TurbineTestContext<*>.assertNotificationShown() {
    awaitItem() shouldBe NotificationEvent.Show
    awaitItem() shouldBe NotificationEvent.ProcessingComplete
    expectNoEvents()
  }

  private suspend fun TurbineTestContext<*>.assertNoNotification() {
    awaitItem() shouldBe NotificationEvent.ProcessingComplete
    expectNoEvents()
  }

  private suspend fun TurbineTestContext<*>.assertNotificationHidden() {
    awaitItem() shouldBe NotificationEvent.Hide
    cancelAndIgnoreRemainingEvents()
  }
}
