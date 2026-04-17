package com.floraflow.app.util

import android.content.Context
import android.content.Intent
import com.floraflow.app.data.PlantCollectionWithPlants

object CollectionShareUtil {

    fun share(context: Context, collection: PlantCollectionWithPlants) {
        val plantList = collection.plants.joinToString("\n") { "🌿 ${it.plantName}" }
        val text = buildString {
            append("${collection.collection.emoji} Mon herbier FloraFlow — ${collection.collection.name}\n\n")
            if (plantList.isNotBlank()) append(plantList)
            else append("Collection vide")
            append("\n\n#FloraFlow #Botanique #Nature")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Partager ma collection"))
    }
}
