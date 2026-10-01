package com.intellij.aidebugger.common.viewModels

import com.intellij.aidebugger.common.models.HierarchicalTraceEvent
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddToDatasetViewModelTest : BasePlatformTestCase() {

    private lateinit var datasetsRepo: TracesDatasetsRepository
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var viewModel: AddToDatasetViewModel
    private val collectorJobs = mutableListOf<Job>()

    override fun setUp() {
        super.setUp()
        datasetsRepo = project.getService(TracesDatasetsRepository::class.java)

        // Use Dispatchers.Default with exception handler to suppress registry key exceptions
        val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            // Suppress MissingResourceException for registry keys not available in tests
            if (throwable !is java.util.MissingResourceException) {
                throw throwable
            }
        }
        coroutineScope = CoroutineScope(Dispatchers.Default + Job() + exceptionHandler)
        viewModel = AddToDatasetViewModel(project, datasetsRepo, coroutineScope)

        // Launch collectors to keep StateFlows active
        // Note: We don't collect isVisible because it depends on registry key that's not available in tests
        collectorJobs.add(coroutineScope.launch { viewModel.datasets.collect {} })
        collectorJobs.add(coroutineScope.launch { viewModel.canAddToDataset.collect {} })
        collectorJobs.add(coroutineScope.launch { viewModel.currentCheckboxStates.collect {} })

        // Give the StateFlows time to initialize and start collecting
        Thread.sleep(200)
    }

    override fun tearDown() {
        collectorJobs.forEach { it.cancel() }
        super.tearDown()
    }

    /**
     * Wait for a StateFlow to reach an expected value, with timeout
     */
    private fun <T> waitForValue(flow: StateFlow<T>, expected: T, timeoutMs: Long = 3000): Boolean {
        // Give a moment for flows to start processing
        Thread.sleep(50)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (flow.value == expected) {
                return true
            }
            Thread.sleep(20)
        }
        return false
    }

    /**
     * Wait for a condition on a StateFlow value, with timeout
     */
    private fun <T> waitForCondition(flow: StateFlow<T>, condition: (T) -> Boolean, timeoutMs: Long = 3000): Boolean {
        // Give a moment for flows to start processing
        Thread.sleep(50)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition(flow.value)) {
                return true
            }
            Thread.sleep(20)
        }
        return false
    }

    fun `test initial state is correct`() {
        assertFalse(viewModel.showPopup.value)
        assertFalse(viewModel.isCreatingNew.value)
        assertEquals("", viewModel.newDatasetName.value)
        // datasets can be non-empty if .jbeval/datasets directory exists from previous runs
        assertNotNull(viewModel.datasets.value)
        assertFalse(viewModel.canAddToDataset.value)
    }

    fun `test togglePopup opens and closes popup`() {
        assertFalse(viewModel.showPopup.value)

        viewModel.togglePopup()
        assertTrue(viewModel.showPopup.value)

        viewModel.togglePopup()
        assertFalse(viewModel.showPopup.value)
    }

    fun `test closePopup closes the popup`() {
        viewModel.togglePopup()
        assertTrue(viewModel.showPopup.value)

        viewModel.closePopup()
        assertFalse(viewModel.showPopup.value)
    }

    fun `test startCreatingDataset sets creating mode`() {
        viewModel.startCreatingDataset()

        assertTrue(viewModel.isCreatingNew.value)
        assertEquals("", viewModel.newDatasetName.value)
    }

    fun `test setNewDatasetName updates name`() {
        viewModel.startCreatingDataset()
        viewModel.setNewDatasetName("my-dataset")

        assertEquals("my-dataset", viewModel.newDatasetName.value)
    }

    fun `test commitNewDataset ignores empty names`() {
        val initialSize = datasetsRepo.listDatasets().size

        viewModel.startCreatingDataset()
        viewModel.setNewDatasetName("   ")
        viewModel.commitNewDataset()

        assertTrue(viewModel.isCreatingNew.value)
        assertEquals(initialSize, datasetsRepo.listDatasets().size)
    }

    fun `test cancelNewDataset exits creating mode`() {
        viewModel.startCreatingDataset()
        viewModel.setNewDatasetName("test")
        viewModel.cancelNewDataset()

        assertFalse(viewModel.isCreatingNew.value)
        assertEquals("", viewModel.newDatasetName.value)
    }

    fun `test setSelectedThread with running thread prevents adding to dataset`() {
        val thread = createSessionThreadVM("thread1", "Thread 1")
        val hstate = createHierarchicalState()

        viewModel.setSelectedThread(thread, true, hstate)

        assertFalse(viewModel.canAddToDataset.value)
    }

    fun `test setSelectedThread closes popup when thread is running`() {
        val thread = createSessionThreadVM("thread1", "Thread 1")

        viewModel.togglePopup()
        assertTrue(viewModel.showPopup.value)

        viewModel.setSelectedThread(thread, true, null)
        assertFalse(viewModel.showPopup.value)
    }

    fun `test setSelectedThread with null thread disables adding`() {
        viewModel.setSelectedThread(null, false, null)

        assertFalse(viewModel.canAddToDataset.value)
        assertTrue(viewModel.currentCheckboxStates.value.isEmpty())
    }

    fun `test setSelectedThread preserves checkbox states per thread`() {
        val thread1 = createSessionThreadVM("thread1", "Thread 1")
        val thread2 = createSessionThreadVM("thread2", "Thread 2")
        datasetsRepo.createDataset("dataset1")

        viewModel.setSelectedThread(thread1, false, createHierarchicalState())

        var checkboxStates = viewModel.currentCheckboxStates.value
        assertFalse(checkboxStates.containsKey("dataset1"))

        viewModel.setSelectedThread(thread2, false, createHierarchicalState())
        checkboxStates = viewModel.currentCheckboxStates.value
        assertFalse(checkboxStates.containsKey("dataset1"))

        viewModel.setSelectedThread(thread1, false, createHierarchicalState())
        checkboxStates = viewModel.currentCheckboxStates.value
        assertFalse(checkboxStates.containsKey("dataset1"))
    }

    fun `test addToDataset fails when state is null`() {
        var resultReceived = false
        var result = true

        viewModel.addToDataset("dataset1") {
            resultReceived = true
            result = it
        }

        assertTrue(resultReceived)
        assertFalse(result)
    }

    fun `test addToDataset fails when state has no root events`() {
        val thread = createSessionThreadVM("thread1", "Thread 1")
        val emptyState = HierarchicalTraceEventsState(emptyList())
        viewModel.setSelectedThread(thread, false, emptyState)

        var resultReceived = false
        var result = true

        viewModel.addToDataset("dataset1") {
            resultReceived = true
            result = it
        }

        assertTrue(resultReceived)
        assertFalse(result)
    }

    private fun createSessionThreadVM(threadId: String, title: String): SessionThreadVM {
        return object : SessionThreadVM {
            override val threadId = threadId
            override val title = title
            override val isActive = false
        }
    }

    private fun createHierarchicalState(): HierarchicalTraceEventsState {
        val event = HierarchicalTraceEvent(
            id = "event1",
            name = "test_event",
            type = EventType.LlmCall,
            framework = Framework.LangChain,
            timestampStartMs = 1000L,
            timestampEndMs = 2000L,
            finished = true,
            payload = mapOf("input" to "test input", "output" to "test output")
        )
        return HierarchicalTraceEventsState(listOf(event))
    }
}
