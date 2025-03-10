package com.example.quarrymap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class CommuneActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private val images = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commune)

        val communeName = intent.getStringExtra("COMMUNE") ?: return
        title = communeName

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // Charger les images de la commune
        val baseFolder = File("/storage/emulated/0/Documents/plans_triés/$communeName")
        if (baseFolder.exists()) {
            images.addAll(baseFolder.listFiles()!!.filter { it.extension in listOf("jpg", "png") }.map { it.absolutePath })
        }

        adapter = ImageAdapter(images) { imagePath ->
            ImageViewerActivity.start(this, imagePath)
        }
        recyclerView.adapter = adapter
    }
}
