package com.jetbrains.spark.submit.util

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.CommonClassNames
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.IOException

class SparkJobClassScanner {
  fun scanClasses(file: VirtualFile, indicator: ProgressIndicator?): List<String> {
    val jarFileSystem = JarFileSystem.getInstance()

    val jarRoot = if (file.isInLocalFileSystem)
      jarFileSystem.getJarRootForLocalFile(file)
    else
      file

    jarRoot ?: return emptyList()

    val mainClassSearcher = MainFunSearcher(jarRoot, indicator)
    VfsUtilCore.visitChildrenRecursively(jarRoot, mainClassSearcher, RuntimeException::class.java)
    return mainClassSearcher.results

  }

  private inner class MainFunSearcher(val file: VirtualFile, private val indicator: ProgressIndicator?) : VirtualFileVisitor<Any?>() {
    val packageBuilder = StringBuilder()
    var results = mutableListOf<String>()

    override fun visitFile(child: VirtualFile): Boolean {
      indicator?.text2 = SparkMessagesBundle.message("fun.search.process.text", child.path)

      when {
        indicator?.isCanceled == true -> return false
        child.parent == null -> return true
        child.isDirectory -> {
          val name = child.name
          if (!StringUtil.isJavaIdentifier(name)) return false
          packageBuilder.append("$name.")
          return true
        }
        child.extension == "class" -> {
          val className = packageBuilder.toString() + child.nameWithoutExtension
          val isFound = searchMainFunctionsInClass(className.replace('.', '/'))
          if (isFound) {
            results.add(className)
          }
          return false
        }
        else -> return false
      }
    }

    private fun searchMainFunctionsInClass(className: String): Boolean {
      val classResource = className + CommonClassNames.CLASS_FILE_EXTENSION
      val inputStream = getResourceAsStream(classResource, file) ?: return false
      val reader = ClassReader(inputStream)
      val classVisitor = MainFunctionClassVisitor()
      reader.accept(classVisitor, ClassReader.SKIP_DEBUG)
      return classVisitor.isMainClassFound
    }

    override fun afterChildrenVisited(file: VirtualFile) {
      if (packageBuilder.isNotEmpty()) {
        packageBuilder.setLength(0.coerceAtLeast(packageBuilder.lastIndexOf(".", packageBuilder.length - 2) + 1))
      }
    }
  }

  companion object {
    private fun getResourceAsStream(url: String, file: VirtualFile): ByteArray? {
      val root: VirtualFile? = when {
        file.isDirectory -> {
          file
        }
        file.fileType is ArchiveFileType -> {
          JarFileSystem.getInstance().getJarRootForLocalFile(file)
        }
        else -> {
          null
        }
      }
      if (root == null) return null
      val child = root.findFileByRelativePath(url) ?: return null
      try {
        return child.contentsToByteArray()
      }
      catch (e: IOException) {
        // nothing
      }
      return null
    }
  }
}

private class MainFunctionClassVisitor : ClassVisitor(Opcodes.API_VERSION) {
  var isMainClassFound = false
  override fun visitMethod(access: Int,
                           name: String?,
                           descriptor: String?,
                           signature: String?,
                           exceptions: Array<out String>?): MethodVisitor? {
    if (name?.contains("main") == true) {
      isMainClassFound = true
    }
    return super.visitMethod(access, name, descriptor, signature, exceptions)
  }
}