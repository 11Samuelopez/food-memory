package com.example.foodmemory.data.local

import android.content.Context
import com.example.foodmemory.domain.repository.DishPhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class InternalDishPhotoRepository(context: Context) : DishPhotoRepository {
    private val photoDirectory = File(context.filesDir, "dish-photos")

    override suspend fun cache(sourcePath: String): String = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        require(source.isFile) { "La fotografía ya no está disponible." }
        val destination = File(photoDirectory, "${UUID.randomUUID()}.jpg")
        check(photoDirectory.isDirectory || photoDirectory.mkdirs()) {
            "No se pudo preparar el almacenamiento de fotografías."
        }
        val temporaryFile = File(photoDirectory, "${destination.name}.tmp")
        try {
            source.copyTo(temporaryFile, overwrite = false)
            check(temporaryFile.renameTo(destination)) {
                "No se pudo guardar la fotografía completa."
            }
        } finally {
            temporaryFile.delete()
        }
        destination.absolutePath
    }
}
