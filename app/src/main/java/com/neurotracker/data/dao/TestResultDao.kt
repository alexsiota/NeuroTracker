package com.neurotracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neurotracker.data.entities.TestResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la entidad [TestResultEntity].
 *
 * ─── Nueva query: updateUserEmail ────────────────────────────────────────────
 *
 * Se añade [updateUserEmail] para soportar el cambio de email del usuario
 * sin pérdida de datos. Cuando el usuario cambia su email en [ProfileViewModel],
 * todos los resultados de tests que apuntan al email antiguo deben actualizarse
 * al nuevo email para que HistoryScreen y StatisticsScreen los sigan encontrando.
 *
 * Sin esta query, cambiar el email hacía que todos los tests del usuario
 * quedaran "huérfanos" (apuntando al email antiguo) y desaparecieran del historial.
 */
@Dao
interface TestResultDao {

    /**
     * Inserta un nuevo resultado de test en la tabla `test_results`.
     *
     * @param result Entidad con los datos del test completado.
     */
    @Insert
    suspend fun insertResult(result: TestResultEntity)

    /**
     * Obtiene un [Flow] reactivo de todos los resultados de un usuario.
     *
     * Room emite automáticamente cada vez que se inserta o elimina un registro.
     *
     * @param email Email del usuario autenticado.
     * @return [Flow] que emite [List]<[TestResultEntity]> ante cambios.
     */
    @Query("SELECT * FROM test_results WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getResultsForUserFlow(email: String): Flow<List<TestResultEntity>>

    /**
     * Elimina un resultado de test por su id.
     *
     * @param id Identificador único del resultado a eliminar.
     */
    @Query("DELETE FROM test_results WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Actualiza el campo [userEmail] en todos los resultados del usuario.
     *
     * Se llama desde [ProfileViewModel.migrateTestResults] cuando el usuario
     * cambia su email, para que los tests sigan siendo visibles en el historial
     * con el nuevo email de la sesión.
     *
     * Sin esta query, cambiar el email hacía que los tests quedaran huérfanos:
     * apuntaban al email antiguo pero la sesión tenía el nuevo, por lo que
     * HistoryScreen filtraba por el nuevo email y no encontraba nada.
     *
     * @param oldEmail Email anterior del usuario.
     * @param newEmail Nuevo email del usuario tras el cambio.
     */
    @Query("UPDATE test_results SET userEmail = :newEmail WHERE userEmail = :oldEmail")
    suspend fun updateUserEmail(oldEmail: String, newEmail: String)
}
