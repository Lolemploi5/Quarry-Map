package com.example.quarrymap

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Classe de test pour vérifier le système d'ancrage des planches
 */
class AnchoringSystemTester(private val context: Context) {
    
    companion object {
        private const val TAG = "AnchoringSystemTester"
    }
    
    private val configManager = ConfigurationManager(context)
    
    /**
     * Test de création et sauvegarde d'une configuration
     */
    fun testCreateAndSaveConfiguration(): Boolean {
        return try {
            // Créer une configuration de test
            val testFile = File(context.getExternalFilesDir(null), "test_planche.jpg")
            testFile.createNewFile() // Créer un fichier temporaire pour le test
            
            val configuration = PlancheConfiguration(
                plancheFile = testFile,
                latitude = 46.603354,
                longitude = 1.888334,
                rotation = 45f,
                scale = 1.2f,
                opacity = 0.8f,
                isAnchored = true,
                anchorMode = AnchorMode.CENTER
            )
            
            // Ajouter quelques points d'ancrage
            val anchorPoints = listOf(
                AnchorPoint(
                    name = "Coin supérieur gauche",
                    localX = 0.1f,
                    localY = 0.1f,
                    latitude = 46.604,
                    longitude = 1.887,
                    isActive = true
                ),
                AnchorPoint(
                    name = "Centre",
                    localX = 0.5f,
                    localY = 0.5f,
                    latitude = 46.603354,
                    longitude = 1.888334,
                    isActive = false
                )
            )
            
            val configWithAnchors = configuration.copy(anchorPoints = anchorPoints)
            
            // Sauvegarder la configuration
            val success = configManager.saveConfiguration("Test Configuration", listOf(configWithAnchors))
            
            if (success) {
                Log.d(TAG, "✅ Configuration de test sauvegardée avec succès")
            } else {
                Log.e(TAG, "❌ Échec de la sauvegarde de configuration")
            }
            
            // Nettoyer le fichier de test
            testFile.delete()
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du test de création/sauvegarde", e)
            false
        }
    }
    
    /**
     * Test de chargement d'une configuration
     */
    fun testLoadConfiguration(): Boolean {
        return try {
            val configurations = configManager.listConfigurations()
            
            if (configurations.isNotEmpty()) {
                val firstConfig = configurations.first()
                val result = configManager.loadConfiguration(firstConfig.fileName)
                
                if (result != null) {
                    val (name, configs) = result
                    Log.d(TAG, "✅ Configuration '$name' chargée avec ${configs.size} planches")
                    
                    // Vérifier les données chargées
                    configs.forEach { config ->
                        Log.d(TAG, "   - Planche: ${config.plancheFile.name}")
                        Log.d(TAG, "     Position: ${config.latitude}, ${config.longitude}")
                        Log.d(TAG, "     Ancrée: ${config.isAnchored}")
                        Log.d(TAG, "     Points d'ancrage: ${config.anchorPoints.size}")
                    }
                    
                    true
                } else {
                    Log.e(TAG, "❌ Échec du chargement de configuration")
                    false
                }
            } else {
                Log.w(TAG, "⚠️ Aucune configuration disponible pour le test")
                true // Ce n'est pas une erreur
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du test de chargement", e)
            false
        }
    }
    
    /**
     * Test de sérialisation/désérialisation JSON
     */
    fun testJsonSerialization(): Boolean {
        return try {
            val testFile = File(context.getExternalFilesDir(null), "test_json_planche.jpg")
            testFile.createNewFile()
            
            // Créer une configuration complexe
            val originalConfig = PlancheConfiguration(
                plancheFile = testFile,
                latitude = 48.8566,
                longitude = 2.3522,
                rotation = 90f,
                scale = 0.8f,
                opacity = 0.6f,
                isAnchored = true,
                anchorMode = AnchorMode.MULTI_POINT,
                anchorPoints = listOf(
                    AnchorPoint(
                        name = "Point A",
                        localX = 0.2f,
                        localY = 0.3f,
                        latitude = 48.857,
                        longitude = 2.351,
                        isActive = true
                    ),
                    AnchorPoint(
                        name = "Point B",
                        localX = 0.8f,
                        localY = 0.7f,
                        latitude = 48.856,
                        longitude = 2.353,
                        isActive = false
                    )
                )
            )
            
            // Sérialiser vers JSON
            val json = originalConfig.toJson()
            Log.d(TAG, "JSON généré: ${json.toString(2)}")
            
            // Désérialiser depuis JSON
            val deserializedConfig = PlancheConfiguration.fromJson(json)
            
            if (deserializedConfig != null) {
                // Vérifier que les données sont correctes
                val isDataIntact = deserializedConfig.latitude == originalConfig.latitude &&
                        deserializedConfig.longitude == originalConfig.longitude &&
                        deserializedConfig.rotation == originalConfig.rotation &&
                        deserializedConfig.scale == originalConfig.scale &&
                        deserializedConfig.opacity == originalConfig.opacity &&
                        deserializedConfig.isAnchored == originalConfig.isAnchored &&
                        deserializedConfig.anchorMode == originalConfig.anchorMode &&
                        deserializedConfig.anchorPoints.size == originalConfig.anchorPoints.size
                
                if (isDataIntact) {
                    Log.d(TAG, "✅ Sérialisation/désérialisation JSON réussie")
                    testFile.delete()
                    return true
                } else {
                    Log.e(TAG, "❌ Données corrompues après désérialisation")
                }
            } else {
                Log.e(TAG, "❌ Échec de la désérialisation JSON")
            }
            
            testFile.delete()
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du test de sérialisation JSON", e)
            false
        }
    }
    
    /**
     * Test de performance du système d'ancrage
     */
    fun testPerformance(): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Créer et sauvegarder 50 configurations
            repeat(50) { i ->
                val testFile = File(context.getExternalFilesDir(null), "perf_test_$i.jpg")
                testFile.createNewFile()
                
                val config = PlancheConfiguration(
                    plancheFile = testFile,
                    latitude = 46.0 + (i * 0.001),
                    longitude = 1.0 + (i * 0.001),
                    rotation = (i * 7.2f) % 360f,
                    scale = 0.5f + (i * 0.01f),
                    opacity = 0.3f + (i * 0.01f),
                    isAnchored = i % 2 == 0,
                    anchorMode = if (i % 3 == 0) AnchorMode.CENTER else AnchorMode.CORNER
                )
                
                configManager.saveConfiguration("Performance Test $i", listOf(config))
                testFile.delete()
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            Log.d(TAG, "✅ Test de performance: 50 configurations en ${duration}ms")
            
            // Nettoyer les configurations de test
            val configs = configManager.listConfigurations()
            configs.filter { it.name.startsWith("Performance Test") }
                .forEach { configManager.deleteConfiguration(it.fileName) }
            
            duration < 5000 // Considérer comme réussi si < 5 secondes
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du test de performance", e)
            false
        }
    }
    
    /**
     * Exécuter tous les tests
     */
    fun runAllTests(): TestResults {
        Log.d(TAG, "🚀 Démarrage des tests du système d'ancrage")
        
        val results = TestResults()
        
        results.createSaveTest = testCreateAndSaveConfiguration()
        results.loadTest = testLoadConfiguration()
        results.jsonSerializationTest = testJsonSerialization()
        results.performanceTest = testPerformance()
        
        val passedTests = listOf(
            results.createSaveTest,
            results.loadTest,
            results.jsonSerializationTest,
            results.performanceTest
        ).count { it }
        
        Log.d(TAG, "🏁 Tests terminés: $passedTests/4 réussis")
        
        if (passedTests == 4) {
            Log.d(TAG, "🎉 Tous les tests du système d'ancrage sont réussis!")
        } else {
            Log.w(TAG, "⚠️ Certains tests ont échoué. Vérifiez les logs pour plus de détails.")
        }
        
        return results
    }
    
    data class TestResults(
        var createSaveTest: Boolean = false,
        var loadTest: Boolean = false,
        var jsonSerializationTest: Boolean = false,
        var performanceTest: Boolean = false
    ) {
        fun allPassed(): Boolean = createSaveTest && loadTest && jsonSerializationTest && performanceTest
        fun passedCount(): Int = listOf(createSaveTest, loadTest, jsonSerializationTest, performanceTest).count { it }
    }
}