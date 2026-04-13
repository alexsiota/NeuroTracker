package com.neurotracker.data.repository

import com.neurotracker.data.dao.TestResultDao
import com.neurotracker.data.entities.TestResultEntity

/**
 * Repositorio para la gestión de resultados de tests cognitivos.
 *
 * Actúa como una capa de abstracción entre el ViewModel y el DAO,
 * facilitando la separación de responsabilidades y el testeo.
 *
 * @param dao Data Access Object para operaciones con la tabla de resultados.
 */
class TestResultRepository(
    private val dao: TestResultDao
) {

    /**
     * Guarda un resultado de test en la base de datos.
     *
     * @param result Entidad [TestResultEntity] con los datos del test a guardar.
     */
    suspend fun saveResult(result: TestResultEntity) {
        dao.insertResult(result)
    }

    /**
     * Obtiene un flujo (Flow) de todos los resultados de un usuario.
     *
     * El Flow emite actualizaciones automáticamente cuando la base de datos cambia,
     * lo que permite a la UI reaccionar en tiempo real.
     *
     * @param email Email del usuario cuyos resultados se desean obtener.
     * @return Flow que emite la lista de resultados ordenada por fecha descendente.
     */
    fun getResultsForUserFlow(email: String) =
        dao.getResultsForUserFlow(email)
}