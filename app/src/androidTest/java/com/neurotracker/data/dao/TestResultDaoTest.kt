package com.neurotracker.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [TestResultDao].
 *
 * Usa una base de datos Room en memoria para garantizar aislamiento entre tests.
 * Cada test parte de un estado limpio sin datos previos.
 *
 * Cubre las operaciones del ciclo de vida de un resultado de test:
 *  - Inserción y recuperación reactiva mediante Flow.
 *  - Eliminación por id.
 *  - Migración de resultados al cambiar el email del usuario.
 *  - Orden descendente por timestamp.
 *  - Aislamiento entre usuarios distintos.
 *
 * Se ejecuta en el dispositivo o emulador porque Room requiere contexto Android.
 */
@RunWith(AndroidJUnit4::class)
class TestResultDaoTest {

    private lateinit var database: NeuroTrackerDatabase
    private lateinit var testResultDao: TestResultDao
    private lateinit var userDao: UserDao

    private val userEmail   = "alex@neurotracker.com"
    private val otherEmail  = "otro@neurotracker.com"

    private val baseResult = TestResultEntity(
        userEmail    = userEmail,
        testType     = "reaction_simple",
        score        = 320L,
        precisionPct = 85,
        eegAlpha     = 1.2f,
        eegBeta      = 0.8f,
        eegGamma     = 0.5f,
        eegTheta     = 0.3f
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NeuroTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        testResultDao = database.testResultDao()
        userDao       = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Verifica que un resultado insertado aparece en el Flow del usuario correspondiente.
     */
    @Test
    fun insertResult_appearsInUserFlow() = runTest {
        testResultDao.insertResult(baseResult)
        val results = testResultDao.getResultsForUserFlow(userEmail).first()
        assertEquals(1, results.size)
        assertEquals(baseResult.testType, results[0].testType)
        assertEquals(baseResult.precisionPct, results[0].precisionPct)
    }

    /**
     * Verifica que el Flow de un usuario no contiene resultados de otro usuario.
     */
    @Test
    fun getResultsForUserFlow_doesNotReturnOtherUsersResults() = runTest {
        testResultDao.insertResult(baseResult)
        testResultDao.insertResult(baseResult.copy(userEmail = otherEmail))
        val results = testResultDao.getResultsForUserFlow(userEmail).first()
        assertEquals(1, results.size)
        assertTrue(results.all { it.userEmail == userEmail })
    }

    /**
     * Verifica que los resultados se devuelven ordenados por timestamp descendente,
     * es decir, el más reciente primero.
     */
    @Test
    fun getResultsForUserFlow_returnsResultsOrderedByTimestampDesc() = runTest {
        val old   = baseResult.copy(timestamp = 1000L)
        val recent = baseResult.copy(testType = "stroop", timestamp = 9000L)
        testResultDao.insertResult(old)
        testResultDao.insertResult(recent)
        val results = testResultDao.getResultsForUserFlow(userEmail).first()
        assertEquals(2, results.size)
        assertTrue(results[0].timestamp > results[1].timestamp)
    }

    /**
     * Verifica que [TestResultDao.deleteById] elimina únicamente el resultado
     * con el id especificado y deja intactos los demás.
     */
    @Test
    fun deleteById_removesOnlyTargetResult() = runTest {
        testResultDao.insertResult(baseResult)
        testResultDao.insertResult(baseResult.copy(testType = "stroop"))
        val all    = testResultDao.getResultsForUserFlow(userEmail).first()
        val target = all.first { it.testType == "reaction_simple" }
        testResultDao.deleteById(target.id)
        val remaining = testResultDao.getResultsForUserFlow(userEmail).first()
        assertEquals(1, remaining.size)
        assertEquals("stroop", remaining[0].testType)
    }

    /**
     * Verifica que [TestResultDao.updateUserEmail] migra todos los resultados
     * del email antiguo al nuevo, y que el email antiguo deja de tener resultados.
     */
    @Test
    fun updateUserEmail_migratesAllResultsToNewEmail() = runTest {
        testResultDao.insertResult(baseResult)
        testResultDao.insertResult(baseResult.copy(testType = "stroop"))
        val newEmail = "nuevo@neurotracker.com"
        testResultDao.updateUserEmail(userEmail, newEmail)
        val oldResults = testResultDao.getResultsForUserFlow(userEmail).first()
        val newResults = testResultDao.getResultsForUserFlow(newEmail).first()
        assertTrue(oldResults.isEmpty())
        assertEquals(2, newResults.size)
        assertTrue(newResults.all { it.userEmail == newEmail })
    }

    /**
     * Verifica que el Flow de un usuario sin resultados emite una lista vacía.
     */
    @Test
    fun getResultsForUserFlow_withNoResults_emitsEmptyList() = runTest {
        val results = testResultDao.getResultsForUserFlow(userEmail).first()
        assertTrue(results.isEmpty())
    }

    /**
     * Verifica que se pueden insertar resultados de múltiples tipos de test
     * para el mismo usuario y todos son recuperables.
     */
    @Test
    fun insertMultipleTestTypes_allRetrievableForSameUser() = runTest {
        val types = listOf("reaction_simple", "stroop", "n_back", "digit_span", "global_cognitive")
        types.forEach { type ->
            testResultDao.insertResult(baseResult.copy(testType = type))
        }
        val results = testResultDao.getResultsForUserFlow(userEmail).first()
        assertEquals(types.size, results.size)
        val resultTypes = results.map { it.testType }.toSet()
        assertTrue(resultTypes.containsAll(types))
    }
}
