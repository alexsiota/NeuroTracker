package com.neurotracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [TestResultRepository].
 *
 * Usa base de datos Room en memoria para garantizar aislamiento entre tests.
 * Ejercita [TestResultRepository.saveResult] y [TestResultRepository.getResultsForUserFlow]
 * verificando que las operaciones se delegan correctamente al DAO.
 */
@RunWith(AndroidJUnit4::class)
class TestResultRepositoryTest {

    private lateinit var database: NeuroTrackerDatabase
    private lateinit var repository: TestResultRepository

    private val testEmail = "test@repository.com"

    private val baseResult = TestResultEntity(
        userEmail    = testEmail,
        testType     = "reaction_simple",
        score        = 350L,
        precisionPct = 90,
        eegAlpha     = 0.4f,
        eegBeta      = 0.6f,
        eegGamma     = 0.3f,
        eegTheta     = 0.2f
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NeuroTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TestResultRepository(database.testResultDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveResult_andGetFlow_returnsInsertedResult() = runTest {
        repository.saveResult(baseResult)
        val results = repository.getResultsForUserFlow(testEmail).first()
        assertEquals(1, results.size)
        assertEquals(baseResult.testType, results[0].testType)
        assertEquals(baseResult.precisionPct, results[0].precisionPct)
        assertEquals(baseResult.score, results[0].score)
    }

    @Test
    fun getResultsForUserFlow_withNoResults_emitsEmptyList() = runTest {
        val results = repository.getResultsForUserFlow(testEmail).first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun saveResult_multipleResults_allAppearsInFlow() = runTest {
        repository.saveResult(baseResult)
        repository.saveResult(baseResult.copy(testType = "stroop", score = 200L))
        repository.saveResult(baseResult.copy(testType = "n_back", score = 100L))
        val results = repository.getResultsForUserFlow(testEmail).first()
        assertEquals(3, results.size)
    }

    @Test
    fun getResultsForUserFlow_doesNotReturnOtherUsersResults() = runTest {
        repository.saveResult(baseResult)
        repository.saveResult(baseResult.copy(userEmail = "other@repository.com"))
        val results = repository.getResultsForUserFlow(testEmail).first()
        assertEquals(1, results.size)
        assertEquals(testEmail, results[0].userEmail)
    }

    @Test
    fun saveResult_preservesEegData() = runTest {
        repository.saveResult(baseResult)
        val result = repository.getResultsForUserFlow(testEmail).first()[0]
        assertEquals(baseResult.eegAlpha, result.eegAlpha, 0.001f)
        assertEquals(baseResult.eegBeta,  result.eegBeta,  0.001f)
        assertEquals(baseResult.eegGamma, result.eegGamma, 0.001f)
        assertEquals(baseResult.eegTheta, result.eegTheta, 0.001f)
    }

    @Test
    fun saveResult_withDefaultEegValues_storesZeros() = runTest {
        repository.saveResult(baseResult.copy(eegAlpha = 0f, eegBeta = 0f, eegGamma = 0f, eegTheta = 0f))
        val result = repository.getResultsForUserFlow(testEmail).first()[0]
        assertEquals(0f, result.eegAlpha, 0.001f)
        assertEquals(0f, result.eegBeta,  0.001f)
    }
}
