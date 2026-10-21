package com.mamafua.app.data.local

import android.content.Context
import androidx.core.content.edit

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("mamafua_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit {
            putString("jwt_token", token)
        }
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun clearToken() {
        prefs.edit {
            remove("jwt_token")
        }
    }

    fun saveAppliedJob(jobId: Int) {
        val jobs = getAppliedJobs().map { it.toString() }.toMutableSet()
        jobs.add(jobId.toString())
        prefs.edit {
            putStringSet("applied_jobs", jobs)
        }
    }

    fun getAppliedJobs(): Set<Int> {
        val jobs = prefs.getStringSet("applied_jobs", emptySet()) ?: emptySet()
        return jobs.mapNotNull { it.toIntOrNull() }.toSet()
    }
}
