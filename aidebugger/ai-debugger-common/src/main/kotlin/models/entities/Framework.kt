package com.intellij.aidebugger.common.models.entities

import com.google.gson.annotations.SerializedName

enum class Framework {
    LangChain,
    LangChainEL,
    LangGraph,
    Koog,
}

enum class SerializableFramework {
    @SerializedName("langchain")
    LANGCHAIN,
    @SerializedName("langchain_expression_language")
    LANGCHAIN_EXPRESSION_LANGUAGE,
    @SerializedName("langgraph")
    LANGGRAPH,
    @SerializedName("koog")
    KOOG,
}

fun SerializableFramework.deserialize(): Framework = when (this) {
    SerializableFramework.LANGCHAIN -> Framework.LangChain
    SerializableFramework.LANGCHAIN_EXPRESSION_LANGUAGE -> Framework.LangChainEL
    SerializableFramework.LANGGRAPH -> Framework.LangGraph
    SerializableFramework.KOOG -> Framework.Koog
}