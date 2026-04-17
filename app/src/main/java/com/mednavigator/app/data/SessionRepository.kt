package com.mednavigator.app.data

import android.content.Context
import androidx.room.Room
import com.mednavigator.app.data.models.AppDatabase
import com.mednavigator.app.data.models.SessionEntity
import com.mednavigator.app.utils.Constants

class SessionRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        Constants.APP_DB_NAME
    ).build()

    private val dao = db.sessionDao()

    suspend fun saveSession(session: SessionEntity): Long {
        return dao.insertSession(session)
    }

    suspend fun getRecentSessions(): List<SessionEntity> {
        return dao.getRecentSessions()
    }

    suspend fun pruneOldSessions(keepDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        dao.deleteOldSessions(cutoff)
    }
}
