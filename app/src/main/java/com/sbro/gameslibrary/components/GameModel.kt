package com.sbro.gameslibrary.components

import java.io.Serializable

enum class WorkStatus {
    WORKING,
    UNTESTED,
    NOT_WORKING
}

enum class IssueType(val firestoreValue: String) {
    CRASH("Crash"),
    BLACK_SCREEN("Black screen"),
    SOFTLOCK("Softlock"),
    GRAPHICS_GLITCHES("Graphics glitches"),
    AUDIO_ISSUES("Audio issues"),
    CONTROLS_NOT_WORKING("Controls not working"),
    SLOW_PERFORMANCE("Slow performance");

    companion object {
        fun fromFirestore(value: String?): IssueType {
            return entries.firstOrNull { it.firestoreValue == value } ?: CRASH
        }
    }
}

enum class Reproducibility(val firestoreValue: String) {
    ALWAYS("Always"),
    OFTEN("Often"),
    RARE("Rare"),
    ONCE("Once");

    companion object {
        fun fromFirestore(value: String?): Reproducibility {
            return entries.firstOrNull { it.firestoreValue == value } ?: ALWAYS
        }
    }
}

enum class EmulatorBuildType(val firestoreValue: String) {
    STABLE("Stable"),
    CANARY("Canary"),
    GIT_HASH("Git hash");

    companion object {
        fun fromFirestore(value: String?): EmulatorBuildType {
            return entries.firstOrNull { it.firestoreValue == value } ?: STABLE
        }
    }
}

data class GameTestResult(
    val status: WorkStatus = WorkStatus.UNTESTED,

    val testedAndroidVersion: String = "",
    val testedDeviceModel: String = "",


    val testedGpuModel: String = "",
    val testedRam: String = "",
    val testedWrapper: String = "",
    val testedPerformanceMode: String = "",


    val testedApp: String = "",
    val testedAppVersion: String = "",
    val testedGameVersionOrBuild: String = "",


    val issueType: IssueType = IssueType.CRASH,
    val reproducibility: Reproducibility = Reproducibility.ALWAYS,
    val workaround: String = "",
    val issueNote: String = "",


    val emulatorBuildType: EmulatorBuildType = EmulatorBuildType.STABLE,
    val accuracyLevel: String = "",
    val resolutionScale: String = "",
    val asyncShaderEnabled: Boolean = false,
    val frameSkip: String = "",


    val resolutionWidth: String = "",
    val resolutionHeight: String = "",
    val fpsMin: String = "",
    val fpsMax: String = "",


    val mediaLink: String = "",


    val testedDateFormatted: String = "",
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
