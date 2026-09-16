package com.intellij.bigdatatools.zeppelin.components.service

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.utils.ZeppelinProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class ZeppelinInstanceFileCloser(val project: Project?, val connection: ZeppelinInstanceCachedConnection): Disposable {
  private val config = connection.config
  private var noteInfos: Set<NotebookInfo> = connection.cachedNotesInfo?.toSet() ?: emptySet()

  private val listener = object: ZeppelinConnectionListener {
    override fun updateNotebookList(notebooks: List<NotebookInfo>) {
      val newNotes = notebooks.toSet()
      val removedNotes = noteInfos - newNotes
      val movedToTrashes = (newNotes - noteInfos).filter { it.isInTrash }.toSet()
      noteInfos = newNotes

      closeForAllProjects(removedNotes)
      closeForAllProjects(movedToTrashes)
    }
  }

  init {
    connection.addListener(listener)
  }

  override fun dispose() {
    connection.removeListener(listener)
    closeForAllProjects(null)
  }

  private fun closeForAllProjects(removedNoteInfos: Set<NotebookInfo>?) {
    val removedNoteIds = removedNoteInfos?.map { it.id }?.toSet()
    val projects = ZeppelinProjectUtil.getProjectList(project)
    projects.forEach {
      closeConnectionForProject(it, removedNoteIds)
    }
  }

  private fun closeConnectionForProject(project: Project, removedNoteIds: Set<String>?) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    val remoteFiles = fileEditorManager.openFiles.filter {
      it.fileType is ZeppelinFileType &&
      NotebookFileUtil.isRemote(it) &&
      NotebookFileUtil.getConfigId(it) == config.innerId
    }
    val filesForClose = if (removedNoteIds == null)
      remoteFiles
    else
      remoteFiles.filter { NotebookFileUtil.getNotebookId(it) in removedNoteIds }
    
    invokeLater {
      if (project.isDisposed)
        return@invokeLater
      filesForClose.forEach {
        fileEditorManager.closeFile(it)
      }
    }
  }
}