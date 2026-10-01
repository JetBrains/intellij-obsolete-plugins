package com.intellij.aidebugger.evaluation.models.storage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object JsonFileStorage {
    private val mapper = jacksonObjectMapper()

    fun ensureDir(p: Path) {
        if (!Files.exists(p)) Files.createDirectories(p)
    }

    fun writeJson(path: Path, obj: Any) {
        ensureDir(path.parent)
        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(obj)
        Files.write(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    }

    fun <T> readJson(path: Path, clazz: Class<T>): T {
        return mapper.readValue(Files.readAllBytes(path), clazz)
    }

    fun readDataPoints(path: Path): List<DataPoint> {
        return mapper.readValue(Files.readAllBytes(path))
    }

    fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { p ->
                runCatching { Files.deleteIfExists(p) }
            }
        }
    }

    fun copyRecursively(src: Path, dst: Path) {
        Files.walk(src).use { stream ->
            stream.forEach { p ->
                val rel = src.relativize(p)
                val dest = dst.resolve(rel.toString())
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest)
                } else {
                    try { Files.createDirectories(dest.parent) } catch (_: Throwable) {}
                    Files.copy(p, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
