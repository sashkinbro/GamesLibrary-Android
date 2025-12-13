package com.sbro.gameslibrary.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import androidx.core.content.edit

data class TestPreset(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val androidVersion: String,
    val deviceModel: String,
    val gpuModel: String,
    val driverVersion: String,
    val ram: String,
    val wrapper: String,
    val performanceMode: String,
    val app: String,
    val appVersion: String,
    val emulatorBuildType: String,
    val accuracy: String,
    val scale: String,
    val asyncShader: Boolean,
    val frameSkip: String
)

class PresetRepository(context: Context) {
    private val prefs = context.getSharedPreferences("user_presets_prefs", Context.MODE_PRIVATE)
    private val KEY_PRESETS = "saved_presets_json"

    fun getAllPresets(): List<TestPreset> {
        val jsonString = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        val list = mutableListOf<TestPreset>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TestPreset(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        androidVersion = obj.optString("androidVersion"),
                        deviceModel = obj.optString("deviceModel"),
                        gpuModel = obj.optString("gpuModel"),
                        driverVersion = obj.optString("driverVersion"),
                        ram = obj.optString("ram"),
                        wrapper = obj.optString("wrapper"),
                        performanceMode = obj.optString("performanceMode"),
                        app = obj.optString("app"),
                        appVersion = obj.optString("appVersion"),
                        emulatorBuildType = obj.optString("emulatorBuildType"),
                        accuracy = obj.optString("accuracy"),
                        scale = obj.optString("scale"),
                        asyncShader = obj.optBoolean("asyncShader"),
                        frameSkip = obj.optString("frameSkip")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.reversed()
    }

    fun savePreset(preset: TestPreset) {
        val currentList = getAllPresets().toMutableList()
        currentList.add(0, preset)
        saveList(currentList)
    }

    fun deletePreset(id: String) {
        val currentList = getAllPresets().toMutableList()
        currentList.removeAll { it.id == id }
        saveList(currentList)
    }

    fun renamePreset(id: String, newName: String) {
        val currentList = getAllPresets().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(name = newName)
            saveList(currentList)
        }
    }

    private fun saveList(list: List<TestPreset>) {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("androidVersion", item.androidVersion)
            obj.put("deviceModel", item.deviceModel)
            obj.put("gpuModel", item.gpuModel)
            obj.put("driverVersion", item.driverVersion)
            obj.put("ram", item.ram)
            obj.put("wrapper", item.wrapper)
            obj.put("performanceMode", item.performanceMode)
            obj.put("app", item.app)
            obj.put("appVersion", item.appVersion)
            obj.put("emulatorBuildType", item.emulatorBuildType)
            obj.put("accuracy", item.accuracy)
            obj.put("scale", item.scale)
            obj.put("asyncShader", item.asyncShader)
            obj.put("frameSkip", item.frameSkip)
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_PRESETS, jsonArray.toString()) }
    }

    fun getNextDefaultName(): String {
        val count = getAllPresets().size
        return "Preset ${count + 1}"
    }
}