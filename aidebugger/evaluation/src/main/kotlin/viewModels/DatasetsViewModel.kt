package com.intellij.aidebugger.evaluation.viewModels

import com.intellij.aidebugger.common.models.DatasetRow
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path

private const val RUNNER_ROWS_KEY = "com.intellij.aidebugger.datasetsView.rows.v1"
private const val RUNNER_SELECTED_DATASET_KEY = "com.intellij.aidebugger.datasetsView.selectedDataset.v1"

@Service(Service.Level.PROJECT)
class DatasetsViewModel(private val project: Project) {
    private val repo: TracesDatasetsRepository = project.service<TracesDatasetsRepository>()
    private val props = PropertiesComponent.getInstance(project)
    private val scope = CoroutineScope(SupervisorJob())

    val datasetNames: StateFlow<List<@NlsSafe String>> = repo.datasets
        .map { list -> list.map { it.name } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _selectedName = MutableStateFlow<@NlsSafe String?>(null)
    val selectedName: StateFlow<@NlsSafe String?> = _selectedName

    private val _rows = MutableStateFlow<List<DatasetRow>>(emptyList())
    val rows: StateFlow<List<DatasetRow>> = _rows

    init {
        val savedSel = props.getValue(RUNNER_SELECTED_DATASET_KEY)
        scope.launch {
            datasetNames.collect { names ->
                val current = _selectedName.value
                when {
                    savedSel != null && names.contains(savedSel) && current == null -> selectDataset(savedSel)
                    current != null && !names.contains(current) -> {
                        selectDataset(names.firstOrNull())
                    }
                    current == null && names.isNotEmpty() -> selectDataset(names.first())
                    current == null -> _rows.value = loadSavedRows()
                }
            }
        }
        scope.launch {
            repo.datasetContentChanged.collect { changedDatasetName ->
                if (changedDatasetName != null) {
                    repo.refresh()
                    if (changedDatasetName == _selectedName.value) {
                        val info = repo.listDatasets().firstOrNull { it.name == changedDatasetName }
                        if (info != null) {
                            val datasetRows = repo.loadDataset(info, inputJsonPath = null, outputJsonPath = null)
                            _rows.value = datasetRows
                        }
                    }
                }
            }
        }
        if (datasetNames.value.isEmpty()) {
            _rows.value = loadSavedRows()
        }
    }

    fun selectDataset(name: String?) {
        _selectedName.value = name
        props.setValue(RUNNER_SELECTED_DATASET_KEY, name ?: "")
        if (name.isNullOrBlank()) {
            _rows.value = loadSavedRows()
        } else {
            val info = repo.listDatasets().firstOrNull { it.name == name }
            val rows = info
                ?.let { repo.loadDataset(it, inputJsonPath = null, outputJsonPath = null) }
                ?: emptyList()
            _rows.value = rows
        }
    }

    fun createDataset() {
        val info = repo.createDataset("")
        selectDataset(info.name)
        setRows(emptyList())
    }

    fun deleteSelected() {
        val name = _selectedName.value ?: return
        repo.deleteDataset(name)
        if (props.getValue(RUNNER_SELECTED_DATASET_KEY) == name) {
            props.setValue(RUNNER_SELECTED_DATASET_KEY, "")
        }
        if (datasetNames.value.isEmpty()) setRows(emptyList())
    }

    fun renameSelected(newName: String): String? {
        val cur = _selectedName.value ?: return null
        if (!isValidDatasetName(newName)) {
            return null
        }
        val renamed = repo.renameDataset(cur, newName)
        val effective = renamed?.name ?: newName
        props.setValue(RUNNER_SELECTED_DATASET_KEY, effective)
        selectDataset(effective)
        return effective
    }

    private fun isValidDatasetName(name: String): Boolean {
        if (name.isBlank()) return false
        val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\n', '\r', '\t')
        if (name.any { it in invalidChars }) {
            return false
        }
        return true
    }

    fun duplicateSelected() {
        val cur = _selectedName.value ?: return
        val info = repo.listDatasets().firstOrNull { it.name == cur } ?: return
        val datasetRows = repo.loadDataset(info, inputJsonPath = null, outputJsonPath = null)
        val newName = "${cur}_copy"
        val newInfo = repo.createDataset(newName)
        repo.saveDataset(newInfo, datasetRows)
        selectDataset(newInfo.name)
    }

    fun setRows(rows: List<DatasetRow>) {
        val normalized = rows
        _rows.value = normalized
        saveRows(normalized)

        val sel = _selectedName.value
        if (sel.isNullOrBlank()) {
            val hasContent = normalized.any {
                it.input.orEmpty().isNotBlank() || it.output.orEmpty().isNotBlank()
            }
            if (hasContent) {
                val info = repo.createDataset("")
                repo.saveDataset(info, normalized)
                selectDataset(info.name)
            }
        } else {
            val info = repo.listDatasets().firstOrNull { it.name == sel }
            if (info != null) {
                repo.saveDataset(info, normalized)
            }
        }
    }

    fun addRowLikeLast() {
        val cur = _rows.value
        val last = cur.lastOrNull() ?: DatasetRow("", "", null)
        setRows(cur + last)
    }

    fun downloadSelected(target: Path) {
        val sel = _selectedName.value ?: return
        val info = repo.listDatasets().firstOrNull { it.name == sel } ?: return
        repo.export(info, target)
    }

    private fun loadSavedRows(): List<DatasetRow> {
        return try {
            val raw = props.getValue(RUNNER_ROWS_KEY) ?: return emptyList()
            decodeRows(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveRows(rows: List<DatasetRow>) {
        try {
            props.setValue(RUNNER_ROWS_KEY, encodeRows(rows))
        } catch (_: Throwable) { }
    }

    private fun encodeRows(rows: List<DatasetRow>): String {
        val cs = StandardCharsets.UTF_8.name()
        return rows.joinToString("\n") { row ->
            val first = row.input.orEmpty()
            val second = row.output.orEmpty()
            "${URLEncoder.encode(first, cs)}\t${URLEncoder.encode(second, cs)}"
        }
    }

    private fun decodeRows(data: String): List<DatasetRow> {
        val cs = StandardCharsets.UTF_8.name()
        if (data.isBlank()) return emptyList()
        return data.lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split('\t')
            val first = URLDecoder.decode(parts.getOrNull(0) ?: "", cs)
            val second = URLDecoder.decode(parts.getOrNull(1) ?: "", cs)
            DatasetRow(first, second, null)
        }
    }
}