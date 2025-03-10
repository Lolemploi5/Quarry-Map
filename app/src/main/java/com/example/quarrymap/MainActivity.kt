package com.example.quarrymap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quarrymap.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            processFolder(uri)
        } else {
            Toast.makeText(this, "Aucun dossier sélectionné", Toast.LENGTH_SHORT).show()
        }
    }

    private val jsonPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            processJson(uri)
        } else {
            Toast.makeText(this, "Aucun fichier JSON sélectionné", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = CommuneAdapter(emptyList())
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener {
            val dialog = UploadOptionsDialog()
            dialog.show(supportFragmentManager, "UploadOptionsDialog")
        }
    }

    private fun processFolder(uri: Uri) {
        Log.d("MainActivity", "Processing folder: $uri")
        // Add logic to process folder contents
    }

    private fun processJson(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.use { it.readText() }
                val jsonArray = JSONArray(jsonContent)
                val totalFiles = jsonArray.length()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Téléchargement démarré", Toast.LENGTH_SHORT).show()
                }

                for (i in 0 until totalFiles) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val communes = jsonObject.optJSONArray("commune")
                    val imageInfo = jsonObject.optJSONObject("fichier_image")

                    if (communes != null && imageInfo != null) {
                        val communeName = communes.getString(0)
                        val imageUrl = imageInfo.optString("url")
                        val imageFilename = imageInfo.optString("filename")

                        if (communeName.isNotEmpty() && imageUrl.isNotEmpty() && imageFilename.isNotEmpty()) {
                            val communeFolder = File("/storage/emulated/0/Documents/plans_triés/$communeName")
                            if (!communeFolder.exists()) communeFolder.mkdirs()

                            val imageFile = File(communeFolder, imageFilename)
                            if (!imageFile.exists()) {
                                downloadImage(imageUrl, imageFile)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Téléchargement terminé", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun downloadImage(url: String, destination: File) {
        // Simulate image download. Replace this with actual download logic
        Log.d("MainActivity", "Downloading image from $url to ${destination.absolutePath}")
    }
}
