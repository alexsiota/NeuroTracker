package com.neurotracker.data.repository

import com.neurotracker.data.dao.TestResultDao
import com.neurotracker.data.entities.TestResultEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios puros del [TestResultRepository].
 *
 * Usa MockK para simular el [TestResultDao] y verificar que el repositorio
 * delega correctamente las operaciones de inserción y consulta de resultados.
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android ni base de datos real.
 */
class TestResultRepositoryTest {

    private lateinit var dao:        TestResultDao
    private lateinit var repository: TestResultRepository

    private val userEmail  = "alex@neurotracker.com"
    private val baseResult = TestResultEntity(
        userEmail    = userEmail,
        testType     = "reaction_simple",
        score        = 320L,
        precisionPct = 85
    )

    @Before
    fun setUp() {
        dao        = mockk()
        repository = TestResultRepository(dao)
    }

    /**
     * Verifica que [TestResultRepository.saveResult] delega la llamada
     * a [TestResultDao.insertResult] exactamente una vez con el resultado correcto.
     */
    @Test
    fun saveResult_delegatesToDao() = runTest {
        coEvery { dao.insertResult(baseResult) } returns Unit
        repository.saveResult(baseResult)
        coVerify(exactly = 1) { dao.insertResult(baseResult) }
    }

    /**
     * Verifica que [TestResultRepository.getResultsForUserFlow] devuelve
     * el Flow del DAO sin modificarlo.
     */
    @Test
    fun getResultsForUserFlow_returnsFlowFromDao() = runTest {
        val expected = listOf(baseResult)
        coEvery { dao.getResultsForUserFlow(userEmail) } returns flowOf(expected)
        val result = repository.getResultsForUserFlow(userEmail).first()
        assertEquals(expected, result)
    }

    /**
     * Verifica que [TestResultRepository.getResultsForUserFlow] devuelve
     * una lista vacía cuando el DAO no tiene resultados para ese email.
     */
    @Test
    fun getResultsForUserFlow_withNoResults_returnsEmptyList() = runTest {
        coEvery { dao.getResultsForUserFlow(userEmail) } returns flowOf(emptyList())
        val result = repository.getResultsForUserFlow(userEmail).first()
        assertTrue(result.isEmpty())
    }

    /**
     * Verifica que [TestResultRepository.getResultsForUserFlow] devuelve
     * todos los resultados cuando hay múltiples entradas para el mismo usuario.
     */
    @Test
    fun getResultsForUserFlow_withMultipleResults_returnsAll() = runTest {
        val results = listOf(
            baseResult,
            baseResult.copy(testType = "stroop",   precisionPct = 70),
            baseResult.copy(testType = "n_back",    precisionPct = 90),
            baseResult.copy(testType = "digit_span", precisionPct = 60)
        )
        coEvery { dao.getResultsForUserFlow(userEmail) } returns flowOf(results)
        val emitted = repository.getResultsForUserFlow(userEmail).first()
        assertEquals(4, emitted.size)
    }

    /**
     * Verifica que [TestResultRepository.saveResult] puede ser llamado
     * múltiples veces con resultados distintos y cada llamada llega al DAO.
     */
    @Test
    fun saveResult_multipleResults_eachDelegatesToDao() = runTest {
        val second = baseResult.copy(testType = "stroop")
        coEvery { dao.insertResult(any()) } returns Unit
        repository.saveResult(baseResult)
        repository.saveResult(second)
        coVerify(exactly = 1) { dao.insertResult(baseResult) }
        coVerify(exactly = 1) { dao.insertResult(second) }
    }
}
