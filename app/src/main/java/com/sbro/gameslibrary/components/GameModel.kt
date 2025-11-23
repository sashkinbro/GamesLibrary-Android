package com.sbro.gameslibrary.components

import java.io.Serializable

enum class WorkStatus {
    WORKING,
    UNTESTED,
    NOT_WORKING
}

data class GameTestResult(
    val status: WorkStatus = WorkStatus.UNTESTED,
    val testedDevice: String = "",
    val testedGpuDriver: String = "",
    val testedApp: String = "",
    val testedAppVersion: String = "",
    val testedDateFormatted: String = "",
    val issueNote: String = "",
    val updatedAtMillis: Long = 0L
) : Serializable

data class Game(
    val id: String,
    val title: String,
    val year: String,
    val genre: String,
    val rating: String,
    val imageUrl: String,
    val telegramLink: String = "",
    val description: String,
    val platform: String,

    val testResults: List<GameTestResult> = emptyList(),
    val isFavorite: Boolean = false
) : Serializable {

    fun latestTestOrNull(): GameTestResult? {
        return testResults.maxByOrNull { it.updatedAtMillis }
    }


    fun overallStatus(): WorkStatus {
        if (testResults.isEmpty()) return WorkStatus.UNTESTED

        val counts = testResults.groupingBy { it.status }.eachCount()

        val workingCount = counts[WorkStatus.WORKING] ?: 0
        val notWorkingCount = counts[WorkStatus.NOT_WORKING] ?: 0
        val untestedCount = counts[WorkStatus.UNTESTED] ?: 0

        val maxCount = maxOf(workingCount, notWorkingCount, untestedCount)

        val candidates = buildList {
            if (workingCount == maxCount) add(WorkStatus.WORKING)
            if (notWorkingCount == maxCount) add(WorkStatus.NOT_WORKING)
            if (untestedCount == maxCount) add(WorkStatus.UNTESTED)
        }

        if (candidates.size == 1) return candidates.first()

        val newestByStatus = candidates.associateWith { st ->
            testResults
                .filter { it.status == st }
                .maxOfOrNull { it.updatedAtMillis } ?: 0L
        }

        return newestByStatus.maxByOrNull { it.value }!!.key
    }
}
