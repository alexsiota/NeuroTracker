package com.neurotracker.data.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tests instrumentados del [ProfilePhotoManager].
 *
 * Verifica la gestión del ciclo de vida de las fotos de perfil en el
 * sistema de archivos local:
 *  - Obtención del archivo de foto por email.
 *  - [getPhotoUri] devuelve null cuando el archivo no existe.
 *  - [getPhotoUri] devuelve una URI válida cuando el archivo existe.
 *  - [deletePhoto] elimina el archivo correctamente.
 *  - [exists] refleja el estado real del archivo.
 *  - Aislamiento entre emails distintos.
 *
 * Se ejecuta en dispositivo o emulador porque [ProfilePhotoManager]
 * accede al sistema de archivos del dispositivo mediante [Context.filesDir].
 */
@RunWith(AndroidJUnit4::class)
class ProfilePhotoManagerTest {

    private lateinit var context: Context
    private val testEmail = "test@neurotracker.com"
    private val otherEmail = "otro@neurotracker.com"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Limpiar archivos de tests anteriores
        ProfilePhotoManager.deletePhoto(context, testEmail)
        ProfilePhotoManager.deletePhoto(context, otherEmail)
    }

    @After
    fun tearDown() {
        ProfilePhotoManager.deletePhoto(context, testEmail)
        ProfilePhotoManager.deletePhoto(context, otherEmail)
    }

    /**
     * Verifica que [ProfilePhotoManager.getPhotoFile] devuelve un [File]
     * con una ruta que incluye el email convertido a nombre de archivo seguro.
     */
    @Test
    fun getPhotoFile_returnsFileWithCorrectPath() {
        val file = ProfilePhotoManager.getPhotoFile(context, testEmail)
        assertTrue(file.path.contains("profile_photos"))
        assertTrue(file.name.contains("test_neurotracker_com"))
        assertTrue(file.name.endsWith(".jpg"))
    }

    /**
     * Verifica que [ProfilePhotoManager.getPhotoUri] devuelve null
     * cuando no existe ningún archivo de foto para ese email.
     */
    @Test
    fun getPhotoUri_whenFileDoesNotExist_returnsNull() {
        assertNull(ProfilePhotoManager.getPhotoUri(context, testEmail))
    }

    /**
     * Verifica que [ProfilePhotoManager.getPhotoUri] devuelve una URI válida
     * cuando el archivo de foto existe en disco.
     */
    @Test
    fun getPhotoUri_whenFileExists_returnsNonNullUri() {
        val file = ProfilePhotoManager.getPhotoFile(context, testEmail)
        file.writeBytes(ByteArray(10))
        val uri = ProfilePhotoManager.getPhotoUri(context, testEmail)
        assertNotNull(uri)
    }

    /**
     * Verifica que [ProfilePhotoManager.exists] devuelve false
     * cuando no existe ningún archivo de foto para ese email.
     */
    @Test
    fun exists_whenFileDoesNotExist_returnsFalse() {
        assertFalse(ProfilePhotoManager.exists(context, testEmail))
    }

    /**
     * Verifica que [ProfilePhotoManager.exists] devuelve true
     * cuando el archivo de foto existe en disco.
     */
    @Test
    fun exists_whenFileExists_returnsTrue() {
        val file = ProfilePhotoManager.getPhotoFile(context, testEmail)
        file.writeBytes(ByteArray(10))
        assertTrue(ProfilePhotoManager.exists(context, testEmail))
    }

    /**
     * Verifica que [ProfilePhotoManager.deletePhoto] elimina el archivo
     * correctamente y [exists] devuelve false tras el borrado.
     */
    @Test
    fun deletePhoto_removesFileFromDisk() {
        val file = ProfilePhotoManager.getPhotoFile(context, testEmail)
        file.writeBytes(ByteArray(10))
        assertTrue(ProfilePhotoManager.exists(context, testEmail))
        ProfilePhotoManager.deletePhoto(context, testEmail)
        assertFalse(ProfilePhotoManager.exists(context, testEmail))
    }

    /**
     * Verifica que [ProfilePhotoManager.deletePhoto] no lanza ninguna excepción
     * cuando el archivo no existe previamente.
     */
    @Test
    fun deletePhoto_whenFileDoesNotExist_doesNotThrow() {
        ProfilePhotoManager.deletePhoto(context, testEmail)
    }

    /**
     * Verifica que archivos de emails distintos están completamente aislados:
     * eliminar la foto de un usuario no afecta a la foto de otro.
     */
    @Test
    fun photoFiles_areisolatedByEmail() {
        val file1 = ProfilePhotoManager.getPhotoFile(context, testEmail)
        val file2 = ProfilePhotoManager.getPhotoFile(context, otherEmail)
        file1.writeBytes(ByteArray(10))
        file2.writeBytes(ByteArray(10))
        ProfilePhotoManager.deletePhoto(context, testEmail)
        assertFalse(ProfilePhotoManager.exists(context, testEmail))
        assertTrue(ProfilePhotoManager.exists(context, otherEmail))
    }

    /**
     * Verifica que el directorio de fotos se crea automáticamente
     * si no existía previamente.
     */
    @Test
    fun getPhotoFile_createsDirectoryIfNotExists() {
        val photoDir = File(context.filesDir, "profile_photos")
        photoDir.deleteRecursively()
        val file = ProfilePhotoManager.getPhotoFile(context, testEmail)
        assertTrue(file.parentFile?.exists() == true)
    }
}
