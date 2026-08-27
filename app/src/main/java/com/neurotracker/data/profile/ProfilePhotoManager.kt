package com.neurotracker.data.profile

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.neurotracker.data.profile.ProfilePhotoManager.getPhotoFile
import java.io.File

/**
 * Gestor de fotos de perfil de usuario.
 *
 * Centraliza toda la lógica de almacenamiento, recuperación y eliminación
 * de las fotos de perfil, que son específicas por cuenta de usuario.
 *
 * ─── Almacenamiento ──────────────────────────────────────────────────────────
 *
 * Las fotos se guardan en el directorio de archivos interno de la app
 * ([Context.filesDir]), fuera del directorio de caché para garantizar su
 * persistencia entre sesiones. Android no elimina estos archivos
 * automáticamente a diferencia de los archivos en caché.
 *
 * Ruta: `files_dir/profile_photos/profile_<emailHash>.jpg`
 *
 * ─── Identificación por cuenta ────────────────────────────────────────────────
 *
 * El nombre del archivo se deriva del email del usuario mediante
 * [emailToFileName], que reemplaza los caracteres no seguros en rutas de
 * archivo (`.`, `@`) por guiones bajos. Esto garantiza que cada cuenta
 * tiene su propia foto aunque compartan el mismo dispositivo.
 *
 * Ejemplo: "alex@gmail.com" → "profile_alex_gmail_com.jpg"
 *
 * ─── URI para Coil ───────────────────────────────────────────────────────────
 *
 * [getPhotoUri] devuelve una URI de [FileProvider] en lugar de una URI de
 * archivo directa (`file://`). Esto es necesario porque Android 7+ (API 24+)
 * bloquea el acceso a `file://` URIs fuera de la app por razones de seguridad.
 * [FileProvider] debe estar declarado en el `AndroidManifest.xml` con el
 * proveedor `${packageName}.fileprovider`.
 */
object ProfilePhotoManager {

    /** Subdirectorio dentro de [Context.filesDir] donde se almacenan las fotos. */
    private const val PHOTO_DIR = "profile_photos"

    /**
     * Devuelve el objeto [File] del archivo de foto de perfil del usuario.
     *
     * Crea el directorio padre si no existe. El archivo puede no existir
     * todavía si el usuario no ha establecido ninguna foto.
     *
     * @param context Contexto de la aplicación para acceder a [Context.filesDir].
     * @param email   Email del usuario cuyo archivo de foto se quiere localizar.
     * @return [File] apuntando a la ruta esperada del archivo de foto.
     */
    fun getPhotoFile(context: Context, email: String): File {
        val dir = File(context.filesDir, PHOTO_DIR).also { it.mkdirs() }
        return File(dir, "profile_${emailToFileName(email)}.jpg")
    }

    /**
     * Devuelve la URI de [FileProvider] de la foto de perfil si el archivo existe.
     *
     * Llama a [getPhotoFile] y comprueba si el archivo existe en disco.
     * Si no existe (usuario sin foto), devuelve null para que la UI muestre
     * el avatar por defecto.
     *
     * La URI de [FileProvider] es necesaria para que Coil pueda leer el archivo:
     * desde Android 7+ las URIs `file://` están bloqueadas fuera de la app.
     *
     * @param context Contexto de la aplicación.
     * @param email   Email del usuario cuya URI de foto se quiere obtener.
     * @return URI de [FileProvider] si el archivo existe, null en caso contrario.
     */
    fun getPhotoUri(context: Context, email: String): Uri? {
        val file = getPhotoFile(context, email)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Elimina el archivo de foto de perfil del usuario del almacenamiento local.
     *
     * Si el archivo no existe la operación no tiene efecto y no lanza ninguna
     * excepción. Se llama desde [ProfileViewModel.removeProfilePhoto] cuando
     * el usuario selecciona "Eliminar foto" en el BottomSheet.
     *
     * Tras llamar a esta función, [getPhotoUri] devuelve null para ese email
     * y la UI muestra el avatar por defecto al incrementar [photoVersion].
     *
     * @param context Contexto de la aplicación.
     * @param email   Email del usuario cuya foto se quiere eliminar.
     */
    fun deletePhoto(context: Context, email: String) {
        val file = getPhotoFile(context, email)
        if (file.exists()) file.delete()
    }

    /**
     * Convierte un email en un nombre de archivo seguro para el sistema de archivos.
     *
     * Reemplaza `.` y `@` por `_` para evitar problemas con caracteres reservados
     * en rutas de archivo. Convierte a minúsculas para consistencia.
     *
     * Ejemplos:
     *  - "Alex@Gmail.com" → "alex_gmail_com"
     *  - "user.name@company.org" → "user_name_company_org"
     *
     * @param email Email del usuario a convertir.
     * @return Cadena segura para usar como nombre de archivo (sin extensión).
     */
    private fun emailToFileName(email: String): String =
        email.lowercase().replace(".", "_").replace("@", "_")


    /**
     * Verifica si el usuario tiene una foto de perfil guardada.
     *
     * @param context Contexto de la aplicación.
     * @param email Email del usuario.
     * @return true si la foto existe, false en caso contrario.
     */
    fun exists(context: Context, email: String): Boolean =
        getPhotoFile(context, email).exists()
}


