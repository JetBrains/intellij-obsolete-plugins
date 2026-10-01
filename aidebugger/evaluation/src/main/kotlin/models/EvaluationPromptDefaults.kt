package com.intellij.aidebugger.evaluation.models

object EvaluationPromptDefaults {
    val DEFAULT_LLM_JUDGE_PROMPT: String = """
        You are an automated judge. You will receive:
        - A Task description.
        - A Reference Output (ground truth).
        - A Model Output to evaluate.

        Given each task, compare the Model Output to the Reference Output (ignoring case and extra whitespace) and assign one of two integer scores: 0 or 1, according to:

        1 - Mostly matches the provided Reference Output (minor errors or small omissions)
        0 - Mstly doesn't match the Reference Output (significant errors or missallignment)

        Respond only with a JSON object containing both fields: "score" (0 or 1) and "explanation" (a brief justification for the score). For example:
        ```json
        {{ "score": 1, "explanation": "Matches the reference aside from casing/spacing." }}
        ```

        Now evaluate:

        Task: {input}
        Reference Output: {outputExpected}
        Model Output: {outputGen}        
    """.trimIndent()
}