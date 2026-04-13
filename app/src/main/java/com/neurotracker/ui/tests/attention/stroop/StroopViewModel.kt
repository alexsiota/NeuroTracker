package com.neurotracker.ui.tests.attention.stroop

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.eeg.SimulatedEegDataSource
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StroopViewModel(application: Application) : AndroidViewModel(application) {

    enum class TestState { IDLE, RUNNING, FINISHED }

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_ROUNDS = 25
        private const val STIMULUS_MS  = 2500L
    }

    // AMARILLO → NARANJA: mayor contraste y sin confusión con verde en daltonismo
    private val words = listOf("ROJO", "VERDE", "AZUL", "NARANJA")

    private val colorPairs = listOf(
        Pair(Color(0xFFC62828), "Rojo"),    // índice 0 → "ROJO"
        Pair(Color(0xFF2E7D32), "Verde"),   // índice 1 → "VERDE"
        Pair(Color(0xFF1565C0), "Azul"),    // índice 2 → "AZUL"
        Pair(Color(0xFFE65100), "Naranja")  // índice 3 → "NARANJA" (antes amarillo)
    )

    var testState     = mutableStateOf(TestState.IDLE);    private set
    var currentItem   = mutableStateOf<StroopItem?>(null); private set
    var currentRound  = mutableStateOf(0);                 private set
    var hits          = mutableStateOf(0);                 private set
    var errors        = mutableStateOf(0);                 private set
    var omissions     = mutableStateOf(0);                 private set
    var answered      = mutableStateOf(false);             private set
    var avgReactionMs = mutableStateOf(0L);                private set

    private val reactionTimes = mutableListOf<Long>()
    private var shownAt       = 0L
    private var testJob: Job? = null

    fun startTest() {
        hits.value = 0; errors.value = 0; omissions.value = 0
        currentRound.value = 0; reactionTimes.clear()
        testState.value = TestState.RUNNING
        testJob = viewModelScope.launch { runRounds() }
    }

    private suspend fun runRounds() {
        repeat(TOTAL_ROUNDS) { index ->
            currentRound.value = index + 1
            answered.value     = false
            generateStimulus()
            shownAt = System.currentTimeMillis()
            delay(STIMULUS_MS)
            if (!answered.value) omissions.value++
            currentItem.value = null
            delay(300)
        }
        finishTest()
    }

    private fun generateStimulus() {
        val wordIndex  = words.indices.random()
        val colorIndex = colorPairs.indices.random()
        val word       = words[wordIndex]
        val (color, _) = colorPairs[colorIndex]
        val isMatch    = wordIndex == colorIndex
        currentItem.value = StroopItem(word, color, isMatch)
    }

    fun onUserAnswer(userSaysMatch: Boolean) {
        if (testState.value != TestState.RUNNING || answered.value) return
        answered.value = true
        reactionTimes.add(System.currentTimeMillis() - shownAt)
        if (userSaysMatch == currentItem.value?.isCorrect) hits.value++ else errors.value++
    }

    fun precisionPercent(): Int {
        val total = hits.value + errors.value + omissions.value
        return if (total == 0) 0 else ((hits.value.toFloat() / total) * 100).toInt()
    }

    fun resetTest() {
        testJob?.cancel()
        testState.value     = TestState.IDLE
        currentItem.value   = null
        hits.value          = 0; errors.value = 0; omissions.value = 0
        avgReactionMs.value = 0L; currentRound.value = 0
        reactionTimes.clear()
    }

    private fun finishTest() {
        avgReactionMs.value = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 0L
        testState.value = TestState.FINISHED
        saveResult()
    }

    private fun saveResult() {
        val avg = avgReactionMs.value.takeIf { it > 0 } ?: return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(TestResultEntity(
                userEmail    = email, testType = "stroop", score = avg,
                precisionPct = precisionPercent(),
                eegAlpha = eeg.alpha, eegBeta = eeg.beta, eegGamma = eeg.gamma, eegTheta = eeg.theta
            ))
        }
    }
}
