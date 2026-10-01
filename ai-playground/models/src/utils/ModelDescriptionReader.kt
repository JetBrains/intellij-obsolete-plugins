package com.intellij.aiplayground.models.utils

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.openapi.diagnostic.thisLogger
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Model information read from the models description XML file
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String
)

/**
 * Provider information read from the models description XML file
 */
data class ProviderInfo(
    val id: LlmProvider,
    val name: String,
    val website: String,
    val models: List<ModelInfo>
)

/**
 * Helper for reading available model descriptions from providers
 */
class ModelDescriptionReader() {
  private var providers: List<ProviderInfo>? = null
    
    /**
     * Gets all provider information
     */
    fun getProviders(): List<ProviderInfo> {
        if (providers == null) {
            loadModelDescriptions()
        }
        return providers ?: emptyList()
    }
    
    /**
     * Gets provider info for the specified provider
     */
    fun getProviderInfo(provider: LlmProvider): ProviderInfo? {
        return getProviders().find { it.id.id == provider.id }
    }
    
    /**
     * Gets all available models for a specific provider
     */
    fun getModelsForProvider(provider: LlmProvider): List<ModelInfo> {
        return getProviderInfo(provider)?.models ?: emptyList()
    }
    
    /**
     * Gets information about a specific model
     */
    fun getModelInfo(provider: LlmProvider, modelId: String): ModelInfo? {
        return getModelsForProvider(provider).find { it.id == modelId }
    }
    
    /**
     * Loads model descriptions from the XML file
     */
    private fun loadModelDescriptions() {
        try {
            val inputStream = ModelDescriptionReader::class.java.classLoader
                .getResourceAsStream("META-INF/models-description.xml")
                
            if (inputStream != null) {
                providers = parseXml(inputStream)
              thisLogger().info("Loaded ${providers?.size} providers with " +
                                "${providers?.sumOf { it.models.size } ?: 0} models")
            } else {
              thisLogger().error("Could not find models-description.xml")
            }
        } catch (e: Exception) {
          thisLogger().error("Failed to load model descriptions", e)
            providers = emptyList()
        }
    }
    
    /**
     * Parses the XML file content
     */
    private fun parseXml(inputStream: InputStream): List<ProviderInfo> {
        val result = mutableListOf<ProviderInfo>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputStream)
            
            val providerElements = document.getElementsByTagName("provider")
            for (i in 0 until providerElements.length) {
                val providerElement = providerElements.item(i) as Element

              val providerId = LlmProviderId(providerElement.getAttribute("id"))
                val providerName = providerElement.getAttribute("name")
                val website = providerElement.getAttribute("website")
                
                try {
                    val providerType = LlmServiceExtension.getProviderById(providerId)
                        ?: throw IllegalArgumentException("Unknown provider ID: $providerId")
                        
                    val models = parseModels(providerElement.getElementsByTagName("model"))
                    
                    result.add(ProviderInfo(
                        id = providerType,
                        name = providerName,
                        website = website,
                        models = models
                    ))
                } catch (e: IllegalArgumentException) {
                  thisLogger().warn("Unknown provider ID: $providerId")
                }
            }
        } catch (e: Exception) {
          thisLogger().error("Error parsing model descriptions XML", e)
        }
        
        return result
    }
    
    /**
     * Parses model elements from the XML
     */
    private fun parseModels(modelElements: NodeList): List<ModelInfo> {
        val models = mutableListOf<ModelInfo>()
        
        for (i in 0 until modelElements.length) {
            val modelElement = modelElements.item(i) as Element
            
            val modelId = modelElement.getAttribute("id")
            val modelName = modelElement.getAttribute("name")
            val description = modelElement.getAttribute("description")
            
            models.add(ModelInfo(
                id = modelId,
                name = modelName,
                description = description
            ))
        }
        
        return models
    }
}