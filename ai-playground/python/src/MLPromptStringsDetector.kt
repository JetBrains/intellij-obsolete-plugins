package com.intellij.aiplayground.python

import com.intellij.aiplayground.python.PromptCandidateFilter.shouldKeep
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.ml.models.PromptStringsModelHolder
import com.jetbrains.mlapi.bundle.ModelPipelineLoader
import com.jetbrains.mlapi.feature.FeatureDeclaration
import com.jetbrains.mlapi.model.pipeline.ModelPipeline
import com.jetbrains.mlapi.model.prediction.ClassesProbabilities
import com.jetbrains.python.psi.PyStringLiteralExpression
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

@Service(Service.Level.APP)
class MLPromptStringsDetector {

  companion object {
    private val LOG = logger<MLPromptStringsDetector>()

    private val placeholderRe = Pattern.compile("""(?<![{$])\{[\p{L}_][\p{L}\p{N}_]*}(?!})""")
    private val sysRe = Pattern.compile(
      listOf(
        "you are\\b", "you have\\b", "you should\\b", "as an ai\\b",
        "act as\\b", "assistant[,:]?\\b", "answer (only|solely)\\b",
        "please\\b", "human[: ]", "system[: ]", "remember\\b", "question\\b",
        "i need\\b", "tell me\\b", "you function\\b", "show me\\b"
      ).joinToString("|"),
      Pattern.CASE_INSENSITIVE
    )
    private val instrRe = Pattern.compile(
      "\\b(" +
      ("write|generate|create|explain|describe|summarize|translate|" +
       "answer|provide|return|output|list|classify|convert|give|determine|identify|" +
       "respond|rate|assess|compare|solve|edit|get|rephrase|replace") +
      ")\\b",
      Pattern.CASE_INSENSITIVE
    )

    private val inputStringFeature = FeatureDeclaration.string("input_string")

    @JvmStatic
    fun getInstance(): MLPromptStringsDetector = service()
  }

  @Volatile
  private var modelPipeline: ModelPipeline<ClassesProbabilities>? = null
  private var modelFuture: CompletableFuture<Void>? = null

  init {
    loadModel()
  }

  private fun loadModel() {
    modelFuture?.cancel(true)

    LOG.info("Loading CatBoost JetEnry prompt strings detection model for AI Playground")
    modelFuture = ModelPipelineLoader.DEFAULT.load(
      PromptStringsModelHolder.getStream(),
      AppExecutorUtil.getAppExecutorService()
    ).thenAccept { model ->
      LOG.info("Successfully loaded CatBoost JetEnry prompt strings detection model for AI Playground")
      this.modelPipeline = model.predictingClasses()
    }.exceptionally { e ->
      LOG.warn("Failed to load CatBoost JetEnry prompt strings detection model for AI Playground", e)
      null
    }
  }

  private fun containsPlaceholder(t: String) = placeholderRe.matcher(t).find()

  data class PlaceholderPosition(
    val start: Int,
    val end: Int,
  )

  fun getPlaceholderPositions(t: String): List<PlaceholderPosition> {
    val matcher = placeholderRe.matcher(t)
    val placeholders = mutableListOf<PlaceholderPosition>()
    while (matcher.find()) {
      placeholders.add(PlaceholderPosition(matcher.start(), matcher.end()))
    }
    return placeholders
  }

  private fun looksLikeBibTex(s: String): Boolean {
    if (s.trimStart().startsWith("@")) return true
    if (s.length > 100 && s.lines().size >= 3) {
      val l = s.lowercase()
      val keys = listOf("title=", "author=", "journal=", "booktitle=", "year=", "doi=")
      return keys.count { it in l } >= 2
    }
    return false
  }

  private fun looksLikeRegex(s: String): Boolean {
    val toks = listOf("\\\\d", "\\\\w", "\\\\s", "[", "]", "(?:", "(?=", "(?!", "(?P<", ".*", ".+", "$", "^", "|", "\\\\b", "\\\\B", "\\\\A", "\\\\Z")
    val l = s.lowercase()
    return toks.count { l.contains(it) } >= 2
  }

  private fun looksLikeSystemPrompt(t: String) = t.lines().any {
    val s = it.trimStart('#', ' ').lowercase()
    sysRe.matcher(s).lookingAt()
  }

  private fun looksLikeInstructionPrompt(t: String): Boolean {
    val s = t.trim()
    val matcher = instrRe.matcher(s)
    var matchesCount = 0
    while (matcher.find()) {
      matchesCount++; if (matchesCount > 3) return true
    }
    return false
  }

  /**
   * This function is a helper the extracts the whole line(s) of code in which candidate prompt is located
   * It is done to replicate input format that was used during training
   */
  private fun getContainingLinesText(literal: PyStringLiteralExpression): String {
    val file = literal.containingFile ?: return literal.text
    val project = file.project
    val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return literal.text

    val startOffset = literal.textRange.startOffset
    val rawEndOffset = literal.textRange.endOffset
    val endOffsetForLine = (rawEndOffset - 1).coerceAtLeast(0).coerceAtMost(doc.textLength - 1)

    val startLine = doc.getLineNumber(startOffset)
    val endLine = doc.getLineNumber(endOffsetForLine)

    val lineStart = doc.getLineStartOffset(startLine)
    val lineEnd = doc.getLineEndOffset(endLine)

    return doc.charsSequence.subSequence(lineStart, lineEnd).toString()
  }

  private fun isPromptHeuristics(text: String): Boolean {
    // Blacklist-only early reject
    return !(looksLikeBibTex(text) || looksLikeRegex(text) || LooksLikeCode.looksLikeCode(text))
  }

  private fun isPromptHeuristicsFallback(text: String): Boolean {
    // if fallback check the whitelist conditions
    val isSystemPrompt = if (Registry.`is`("com.intellij.aiplayground.python.experimental.detect.system.prompts")) {
      looksLikeSystemPrompt(text) || looksLikeInstructionPrompt(text)
    } else false

    return containsPlaceholder(text) || isSystemPrompt
  }

  private fun isPromptML(literal: PyStringLiteralExpression): Boolean {
    val content = literal.stringValue
    val m = modelPipeline
    // if the model is not available, use the algorithmic steps
    if (m == null) {
      return isPromptHeuristicsFallback(content)
    }
    // Use whole containing line(s) as the model input to match training setup
    val modelInput = getContainingLinesText(literal)
    val logits = m.predict(listOf(inputStringFeature with modelInput))
    return logits.probabilities.getValue("text") > 0.5
  }

  fun detectPrompts(literal: PyStringLiteralExpression): Boolean {
    val text = literal.stringValue
    // Step 1: heuristic quick reject
    if (!isPromptHeuristics(text)) return false
    // Step 2: stronger filter
    if (!shouldKeep(literal)) return false
    // Step 3: ML when available; otherwise algorithmic fallback
    return isPromptML(literal)
  }
}
