package com.example.quarrymap

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class CommunesFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var searchCard: CardView
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_communes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.searchView)
        searchCard = view.findViewById(R.id.searchCard)
        
        setupRecyclerView()
        setupSearchView()
        loadCommunesList()
        
        searchCard.setOnClickListener {
            // Animation de l'élévation de la carte
            it.animate()
                .translationZ(8f)
                .setDuration(150)
                .withEndAction {
                    searchView.requestFocus()
                    searchView.isIconified = false
                    
                    // Afficher le clavier virtuel
                    val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
                    
                    it.animate()
                        .translationZ(0f)
                        .setStartDelay(200)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CommuneAdapter(emptyList()) { commune ->
            // Lancement de l'activité CommuneActivity avec le nom de la commune
            val intent = Intent(requireContext(), CommuneActivity::class.java).apply {
                putExtra(CommuneActivity.EXTRA_COMMUNE, commune)
                putExtra("EXTRA_BASE_PATH", (requireActivity() as MainActivity).getDownloadPath())
            }
            startActivity(intent)
        }
    }
    
    private fun setupSearchView() {
        // Rendre toute la zone de recherche cliquable, pas juste la carte
        searchView.setOnClickListener {
            searchView.isIconified = false
            searchView.requestFocus()
            
            // Afficher le clavier virtuel
            val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
        }
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCommunes(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCommunes(newText)
                return true
            }
        })
    }
    
    private fun filterCommunes(query: String?) {
        (recyclerView.adapter as? CommuneAdapter)?.filter(query ?: "")
    }
    
    fun refreshCommunes() {
        loadCommunesList()
    }
    
    private fun loadCommunesList() {
        val basePath = (requireActivity() as MainActivity).getDownloadPath()
        val communesDir = File(basePath)
        
        if (communesDir.exists() && communesDir.isDirectory) {
            val communes = communesDir.listFiles()
                ?.filter { it.isDirectory }
                ?.map { it.name }
                ?.sorted() ?: emptyList()
            
            (recyclerView.adapter as? CommuneAdapter)?.updateData(communes)
            
            if (communes.isEmpty()) {
                Toast.makeText(requireContext(), "Aucune commune trouvée. Importez des données d'abord.", Toast.LENGTH_LONG).show()
            }
        } else {
            communesDir.mkdirs()
            Toast.makeText(requireContext(), "Aucune commune trouvée. Importez des données d'abord.", Toast.LENGTH_LONG).show()
        }
    }
}
