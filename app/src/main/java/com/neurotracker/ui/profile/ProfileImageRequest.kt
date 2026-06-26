package com.neurotracker.ui.profile

import android.content.Context
import android.net.Uri
import coil.request.ImageRequest
import com.neurotracker.data.profile.ProfilePhotoManager

/**
 * Construye el [ImageRequest] de Coil para la foto de perfil.
 *
 * La cache key se deriva del estado real del archivo en disco: combina [email],
 * [java.io.File.lastModified] y [java.io.File.length]. Esto garantiza que Coil
 * invalida su caché en memoria y en disco automáticamente en cuanto el archivo
 * cambia, sin depender de ningún contador en memoria que pueda resetearse al
 * recrear el ViewModel o reentrar al composable.
 *
 * @param context Contexto para construir el [ImageRequest].
 * @param uri     URI de FileProvider de la foto actual, o null si no hay foto.
 * @param email   Email del usuario, usado para localizar el archivo en disco.
 */
fun buildProfileImageRequest(context: Context, uri: Uri?, email: String): ImageRequest {
    val file = ProfilePhotoManager.getPhotoFile(context, email)
    val cacheKey = if (uri != null && file.exists()) {
        "profile_${email}_${file.lastModified()}_${file.length()}"
    } else {
        "profile_${email}_none"
    }
    return ImageRequest.Builder(context)
        .data(uri)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .build()
}
