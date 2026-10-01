package com.intellij.dbt.python

import com.intellij.dbt.python.PyDbtUtil.Companion.getAllProfileNames
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel

class PyV3DbtViewModel @RequiresEdt constructor() {
  private val _profilesModel = DefaultComboBoxModel<Profile>()
  val isFullyLoaded: Boolean get() = _profilesModel.size > 0

  @Synchronized
  fun getProfilesModel(): ComboBoxModel<Profile> {
    if (!isFullyLoaded) {
      loadProfiles()
    }
    return _profilesModel
  }

  private fun loadProfiles() {
    ApplicationManager.getApplication().service<PyV3DbtViewModelService>().scope.launch {
      val profileNames = withContext(Dispatchers.IO) { getAllProfileNames() }
      withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
        _profilesModel.addAll(profileNames)
        _profilesModel.selectedItem = _profilesModel.getElementAt(0)
      }
    }
  }
}

@Service
private class PyV3DbtViewModelService(val scope: CoroutineScope)