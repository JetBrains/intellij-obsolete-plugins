package com.intellij.aidebugger.evaluation.models.runner

import com.intellij.aidebugger.common.AiDebuggerPlugin
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.models.SessionCountersImpl
import com.intellij.aidebugger.common.models.buildHierarchicalStructure
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.services.network.client.DebuggerClientTransportConfig
import com.intellij.aidebugger.common.services.network.client.DebuggerSimpleClientTransport
import com.intellij.aidebugger.common.services.network.getFreePort
import com.intellij.aidebugger.evaluation.intellij.EnvVarsAggregator
import com.intellij.aidebugger.evaluation.intellij.RunConfigResolver
import com.intellij.aidebugger.python.models.CommonTraceEventsRepository
import com.intellij.aidebugger.python.serialization.asSerializableTraceEvents
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/**
 * Integration point to the existing ai-debugger subsystem.
 *
 * The runner calls this repeatedly for each input (and repeats, if >1).
 *
 * Headless gateway: connects to the ai-debugger Python server via DebuggerSimpleTransport,
 * consumes the real event stream, and returns a list of GroupedEvents representing the run traces.
 */
class AiDebuggerGateway(private val project: Project, private val runConfigName: String) {
    private val log = Logger.getInstance(AiDebuggerGateway::class.java)
    fun runOnce(
        input: String,
        cancelFlag: AtomicBoolean? = null,
    ): HierarchicalTraceEventsState {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val sessionCounters = SessionCountersImpl()
        val workingDir = project.basePath ?: ""

        // Create transport and repository (mirrors LangGraphSession, but without UI wiring)
        val expectedPort = getFreePort()
        val transport = DebuggerSimpleClientTransport(
            DebuggerClientTransportConfig(
                host = "localhost",
                port = expectedPort,
            ),
            scope
        )

        val eventsFlow = transport.incoming
            .receiveAsFlow()
            .asSerializableTraceEvents()

        val repository = CommonTraceEventsRepository(
            scope,
            eventsFlow,
            sessionCounters,
        )

        var pythonExe = RunConfigResolver.resolveInterpreterFromRunConfig(project, runConfigName)
        pythonExe = requireNotNull(pythonExe) { "Python executable not found. Please configure a Python interpreter." }

        val aiProfilerPath = try {
            AiDebuggerPlugin.aiDebuggerPath
        } catch (t: Throwable) {
            "$workingDir/python/ai_profiler.py"
        }
        val cmd = mutableListOf(
            pythonExe,
            aiProfilerPath,
            "--server", expectedPort.toString()
        )

        var finalScript: String? = null
        var finalModule: String? = null
        val rc: Pair<String, String>? = try { RunConfigResolver.resolveTargetFromRunConfig(project, runConfigName) } catch (_: Throwable) { null }
        if (rc != null) {
            if (rc.first == "script") finalScript = rc.second else finalModule = rc.second
        }
        // Ensure we have either a script or module to run
        require(finalScript != null || finalModule != null) {
            "Python target is not specified. Provide either a script path or module name to run."
        }

        when {
            finalScript != null -> {
                cmd.add("--script")
                cmd.add(finalScript)
            }
            finalModule != null -> {
                cmd.add("--module")
                cmd.add(finalModule)
            }
            else -> {
                throw IllegalStateException(
                    "Python target is not specified. Provide one of: " +
                        "JB_PYTHON_SCRIPT or -Djb.python.script, JB_PYTHON_MODULE or -Djb.python.module, " +
                        "JB_RUN_CONFIG_PATH or -Djb.run.config.path, JB_RUN_CONFIG_NAME or -Djb.run.config.name (when .idea/runConfigurations has a matching single entry)."
                )
            }
        }

        cmd.add("--input")
        cmd.add(input)

        // NOTE: (@gas) for compat with how ai_profiler.py handles arguments
        cmd.add("--")

        log.warn("Running command: ${cmd.joinToString(" ")}")

        // Aggregate environment variables from multiple sources (Run Configs, .env)
        val envConfig = EnvVarsAggregator.aggregate(project, runConfigName)

        val pb = ProcessBuilder(cmd).apply {
            redirectErrorStream(true)
            inheritIO()
            redirectInput(ProcessBuilder.Redirect.PIPE)
            directory(File(workingDir))
            envConfig?.let { cfg ->
                val env = environment()
                if (!cfg.passParentEnvs) {
                    env.clear()
                }
                for ((k, v) in cfg.vars) {
                    try {
                        env[k] = RunConfigResolver.expandMacros(v, workingDir)
                    } catch (_: Throwable) {
                        env[k] = v
                    }
                }
            }
            // Always ensure PYTHONPATH includes the project root
            runCatching {
                val env = environment()
                val projectRoot = try { File(workingDir).absolutePath } catch (_: Throwable) { workingDir }
                val existing = env["PYTHONPATH"]?.takeIf { it.isNotBlank() }
                val sep = File.pathSeparator
                env["PYTHONPATH"] = if (existing == null) projectRoot else "$projectRoot$sep$existing"
            }
        }
        var process: Process? = null
        try {
            process = pb.start()

            // Send a default response to any input() calls and close stdin
            try {
                process.outputStream.use { stdin ->
                    stdin.write("\n".toByteArray())
                    stdin.flush()
                }
            } catch (_: Throwable) { }

            val hstate = runBlocking {
                waitForCancellationOrCompletion(cancelFlag, transport, repository, process)
            }
            return hstate
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            runCatching { runBlocking { transport.stop() } }
            process?.let { runCatching { killProcessTree(it) } }
            val msg = t.message?.takeIf { it.isNotBlank() } ?: (t::class.simpleName ?: "Error")
            throw RuntimeException(msg)
        }
    }
}

suspend fun waitForCancellationOrCompletion(
    cancelFlag: AtomicBoolean?,
    transport: DebuggerSimpleClientTransport,
    repository: CommonTraceEventsRepository,
    process: Process? = null
): HierarchicalTraceEventsState = try {
    coroutineScope {
        // Launch cancellation monitoring as a separate coroutine
        val cancellationJob = async {
            while (true) {
                if (cancelFlag?.get() == true) {
                    try { transport.stop() } catch (_: Throwable) {}
                    // Ensure we terminate the entire process tree if process is provided
                    process?.let {
                        try { killProcessTree(it) } catch (_: Throwable) {}
                    }
                    throw CancellationException("Canceled")
                }
                delay(5) // Use shorter delay for more responsive cancellation
            }
        }

        // Launch completion monitoring as a separate coroutine
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        val completionJob = async {
            withTimeoutOrNull(60_000) {
                repository.finished.first { it }
                repository.state
                    .filter { it.events.isNotEmpty() }
                    .debounce(100.milliseconds)
                    .first()
            }

            val state = repository.state.value
            for ((_, event) in state.events) {
                if (event.type == EventType.Exception) {
                    throw RuntimeException(event.name)
                }
            }
            val hstate = buildHierarchicalStructure(state)
            require(hstate.rootEvents.isNotEmpty()) { "No events received from ai-debugger transport" }
            return@async hstate
        }

        // Race between cancellation and completion
        select<HierarchicalTraceEventsState> {
            cancellationJob.onAwait {
                // This will throw CancellationException, so we never reach here
                HierarchicalTraceEventsState(rootEvents = emptyList())
            }
            completionJob.onAwait { result ->
                cancellationJob.cancel() // Cancel the monitoring job
                result
            }
        }
    }
} finally {
    // Clean up resources regardless of how the function exits
    try { transport.stop() } catch (_: Throwable) {}
    // Best-effort final termination of the entire process tree
    process?.let {
        try { killProcessTree(it) } catch (_: Throwable) {}
    }
}

// Ensure a process and all of its descendants are terminated
private fun killProcessTree(process: Process) {
    try {
        val handle = try { process.toHandle() } catch (_: Throwable) { null }
        if (handle != null) {
            // First try to gracefully destroy children
            try {
                handle.descendants().forEach { child ->
                    runCatching { child.destroy() }
                }
                // Give a short grace period
                try { Thread.sleep(100) } catch (_: Throwable) { }
                // Forcibly kill any remaining children
                handle.descendants().forEach { child ->
                    if (runCatching { child.isAlive }.getOrDefault(false)) {
                        runCatching { child.destroyForcibly() }
                    }
                }
            } catch (_: Throwable) { }
            // Now terminate the root
            runCatching { handle.destroy() }
            try { Thread.sleep(100) } catch (_: Throwable) { }
            if (runCatching { handle.isAlive }.getOrDefault(false)) {
                runCatching { handle.destroyForcibly() }
            }
        } else {
            // Fallback without ProcessHandle support
            if (process.isAlive) runCatching { process.destroy() }
            try { Thread.sleep(100) } catch (_: Throwable) { }
            if (process.isAlive) runCatching { process.destroyForcibly() }
        }
    } catch (_: Throwable) {
        // Best effort only
        runCatching { if (process.isAlive) process.destroyForcibly() }
    }
}
