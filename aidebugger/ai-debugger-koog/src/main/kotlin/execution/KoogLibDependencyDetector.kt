package com.intellij.aidebugger.koog.execution

import com.intellij.externalSystem.ImportedLibraryProperties
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.text.SemVer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Service(Service.Level.PROJECT)
class KoogLibDependencyDetector(private val project: Project, private val coroutineScope: CoroutineScope) {

    companion object {

        private val logger = thisLogger()

        fun getInstance(project: Project): KoogLibDependencyDetector = project.service()

        private const val KOOG_LIB_NAME = "ai.koog:agents-core"

        private val koogLibRequiredMinVersion = SemVer("0.5.2", 0, 5, 2)
    }

    data class LibraryData(val library: Library, val version: String?, val module: Module)

    private val _isValidateKoogLibrary = MutableStateFlow(false)

    val isValidKoogLibrary: StateFlow<Boolean> = _isValidateKoogLibrary

    fun isIncludeKoogLibrary(): Boolean {
        return getKoogLibraries().isNotEmpty()
    }

    fun isIncludeApplicableKoogLibrary(): Boolean {
        val koogLibrariesPerModule = getKoogLibraries()

        val latestVersionKoogLibrary = koogLibrariesPerModule.map { libraryData ->
            val version = SemVer.parseFromText(libraryData.version)
            if (version == null) {
                logger.warn("Koog Runner. Found invalid Koog library version: ${libraryData.version}")
                return@map SemVer(libraryData.version ?: "", 0, 0, 0)
            }
            version
        }.maxOrNull()

        val isApplicable = latestVersionKoogLibrary != null &&
                latestVersionKoogLibrary >= koogLibRequiredMinVersion

        logger.info("Is Koog library (name: '$KOOG_LIB_NAME', version: '$latestVersionKoogLibrary') applicable: $isApplicable")
        _isValidateKoogLibrary.value = isApplicable
        return isApplicable
    }

    /**
     * Checks whether the Koog library dependency is present in the project's module dependencies.
     *
     * This method iterates through all the modules of the project and inspects their dependencies
     * to determine if any library's name contains the specified Koog library identifier.
     *
     * @return true if the Koog library is included in the project's dependencies, false otherwise.
     */
    fun getKoogLibraries(): List<LibraryData> {
        return ReadAction.nonBlocking<List<LibraryData>> {
            doGetKoogLibraries()
        }.executeSynchronously()
    }

    private fun doGetKoogLibraries(): List<LibraryData> {
        val koogLibraries = mutableListOf<LibraryData>()

        ModuleManager.getInstance(project).modules.forEach checkModule@{ module ->
            val koogModuleLibraries = ModuleRootManager.getInstance(module).orderEntries.mapNotNull checkEntry@{ entry ->
                (entry as? LibraryOrderEntry)?.library?.takeIf { library ->
                    library.name?.contains(KOOG_LIB_NAME) == true
                }
            }

            logger.debug {
                "Koog Runner. Found <${koogModuleLibraries.size}> Koog library '$KOOG_LIB_NAME' dependency in module: ${module.name}"
            }

            koogModuleLibraries.forEach eachLib@{ library ->
                val libraryProperties = (library as? LibraryEx)?.properties ?: return@eachLib
                val importedProperties = libraryProperties as? ImportedLibraryProperties ?: return@eachLib
                val version = importedProperties.mavenCoordinates?.version ?: return@eachLib

                koogLibraries.add(LibraryData(library, version, module))
            }
        }

        logger.debug {
            val builder = StringBuilder().appendLine("Found Koog '$KOOG_LIB_NAME' libraries:")
            koogLibraries.groupBy { it.module }.forEach { (module, data) ->
                builder.appendLine("  Module: ${module.name}")
                data.forEach { libraryData ->
                    builder.appendLine("    Library: ${libraryData.library.presentableName} (${libraryData.version})")
                }
            }
            builder.toString()
        }

        return koogLibraries
    }
}
