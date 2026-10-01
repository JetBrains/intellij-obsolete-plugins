package com.intellij.aidebugger.common.utility.debug

import com.intellij.openapi.diagnostic.thisLogger
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * GC Watcher utility for debugging object creation and destruction.
 *
 * Usage:
 * ```kotlin
 * val watcher = GCWatcher.getInstance()
 *
 * // Track an object
 * val myObject = MyClass()
 * watcher.watch(myObject, "MyObject instance")
 *
 * // Get statistics
 * watcher.printStats()
 *
 * // Stop watching
 * watcher.stop()
 * ```
 */
class GCWatcher private constructor() {

    companion object {
        private val logger = thisLogger()
        private val instance = GCWatcher()

        fun getInstance(): GCWatcher = instance
    }

    private val referenceQueue = ReferenceQueue<Any>()
    private val trackedObjects = ConcurrentHashMap<TrackedReference, ObjectInfo>()
    private val isRunning = AtomicBoolean(false)
    private val processingThread: Thread
    private val objectIdCounter = AtomicLong(0)

    // Statistics
    private val createdCount = AtomicLong(0)
    private val destroyedCount = AtomicLong(0)
    private val objectTypeStats = ConcurrentHashMap<String, TypeStats>()

    init {
        processingThread = Thread({
            while (isRunning.get() || trackedObjects.isNotEmpty()) {
                try {
                    val ref = referenceQueue.remove(1000) as? TrackedReference
                    if (ref != null) {
                        handleObjectDestroyed(ref)
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    logger.error("GCWatcher: Error processing reference queue", e)
                }
            }
        }, "GCWatcher-ReferenceQueue-Processor").apply {
            isDaemon = true
        }
    }

    /**
     * Start watching for GC events
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            processingThread.start()
            logger.info("GCWatcher: Started")
        }
    }

    /**
     * Stop watching for GC events
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            processingThread.interrupt()
            logger.info("GCWatcher: Stopped")
        }
    }

    /**
     * Watch an object for GC events
     * @param obj The object to watch
     * @param description Optional description for the object
     * @return A unique object ID
     */
    fun watch(obj: Any, description: String = ""): Long {
        if (!isRunning.get()) {
            start()
        }

        val objectId = objectIdCounter.incrementAndGet()
        val className = obj::class.java.name
        val objectInfo = ObjectInfo(
            objectId = objectId,
            className = className,
            description = description,
            createdAt = System.currentTimeMillis(),
            createdAtNano = System.nanoTime()
        )

        val ref = TrackedReference(obj, referenceQueue, objectId)
        trackedObjects[ref] = objectInfo
        createdCount.incrementAndGet()

        // Update type statistics
        objectTypeStats.compute(className) { _, stats ->
            val current = stats ?: TypeStats(className)
            current.created.incrementAndGet()
            current.alive.incrementAndGet()
            current
        }

        logger.debug("GCWatcher: Watching object #$objectId [${className.substringAfterLast('.')}] ${if (description.isNotEmpty()) "\"$description\"" else ""}")

        return objectId
    }

    /**
     * Unwatch an object (stop tracking it)
     */
    fun unwatch(objectId: Long) {
        val entry = trackedObjects.entries.find { it.value.objectId == objectId }
        if (entry != null) {
            trackedObjects.remove(entry.key)
            logger.debug("GCWatcher: Stopped watching object #$objectId")
        }
    }

    /**
     * Clear all tracked objects and reset statistics
     */
    fun clear() {
        trackedObjects.clear()
        objectTypeStats.clear()
        createdCount.set(0)
        destroyedCount.set(0)
        logger.info("GCWatcher: Cleared all tracked objects and statistics")
    }

    /**
     * Get current statistics
     */
    fun getStats(): Stats {
        return Stats(
            created = createdCount.get(),
            destroyed = destroyedCount.get(),
            alive = trackedObjects.size.toLong(),
            typeStats = objectTypeStats.values.map { it.copy() }
        )
    }

    /**
     * Print statistics to log
     */
    fun printStats() {
        val stats = getStats()
        val report = buildString {
            appendLine("\n========== GC Watcher Statistics ==========")
            appendLine("Total Created:   ${stats.created}")
            appendLine("Total Destroyed: ${stats.destroyed}")
            appendLine("Currently Alive: ${stats.alive}")
            appendLine("\nBy Type:")

            stats.typeStats
                .sortedByDescending { it.alive.get() }
                .forEach { typeStats ->
                    val shortName = typeStats.className.substringAfterLast('.')
                    appendLine("  ├─ $shortName")
                    appendLine("  │  Created: ${typeStats.created.get()}, Destroyed: ${typeStats.destroyed.get()}, Alive: ${typeStats.alive.get()}")
                }
            appendLine("==========================================")
        }

        logger.info(report)
        println(report)
    }

    /**
     * Get list of currently alive objects
     */
    fun getAliveObjects(): List<ObjectInfo> {
        return trackedObjects.values.toList()
    }

    /**
     * Print list of currently alive objects
     */
    fun printAliveObjects() {
        val alive = getAliveObjects()
        val report = buildString {
            appendLine("\n========== Alive Objects (${alive.size}) ==========")
            alive.sortedBy { it.objectId }.forEach { info ->
                val age = System.currentTimeMillis() - info.createdAt
                val shortName = info.className.substringAfterLast('.')
                appendLine("  #${info.objectId} [$shortName] age: ${age}ms ${if (info.description.isNotEmpty()) "\"${info.description}\"" else ""}")
            }
            appendLine("==========================================")
        }

        logger.info(report)
        println(report)
    }

    private fun handleObjectDestroyed(ref: TrackedReference) {
        val info = trackedObjects.remove(ref)
        if (info != null) {
            destroyedCount.incrementAndGet()

            val lifetime = System.nanoTime() - info.createdAtNano
            val lifetimeMs = lifetime / 1_000_000.0

            // Update type statistics
            objectTypeStats.computeIfPresent(info.className) { _, stats ->
                stats.destroyed.incrementAndGet()
                stats.alive.decrementAndGet()
                stats.totalLifetimeNanos.addAndGet(lifetime)
                stats
            }

            logger.debug("GCWatcher: Object #${info.objectId} [${info.className.substringAfterLast('.')}] destroyed after ${lifetimeMs}ms ${if (info.description.isNotEmpty()) "\"${info.description}\"" else ""}")
        }
    }

    private class TrackedReference(
        referent: Any,
        queue: ReferenceQueue<Any>,
        val objectId: Long
    ) : PhantomReference<Any>(referent, queue)

    data class ObjectInfo(
        val objectId: Long,
        val className: String,
        val description: String,
        val createdAt: Long,
        val createdAtNano: Long
    )

    data class Stats(
        val created: Long,
        val destroyed: Long,
        val alive: Long,
        val typeStats: List<TypeStats>
    )

    data class TypeStats(
        val className: String,
        val created: AtomicLong = AtomicLong(0),
        val destroyed: AtomicLong = AtomicLong(0),
        val alive: AtomicLong = AtomicLong(0),
        val totalLifetimeNanos: AtomicLong = AtomicLong(0)
    ) {
        fun copy(): TypeStats = TypeStats(
            className = className,
            created = AtomicLong(created.get()),
            destroyed = AtomicLong(destroyed.get()),
            alive = AtomicLong(alive.get()),
            totalLifetimeNanos = AtomicLong(totalLifetimeNanos.get())
        )

        fun getAverageLifetimeMs(): Double {
            val destroyed = this.destroyed.get()
            return if (destroyed > 0) {
                (totalLifetimeNanos.get() / destroyed) / 1_000_000.0
            } else {
                0.0
            }
        }
    }
}