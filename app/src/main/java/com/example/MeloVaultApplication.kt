package com.example

import android.app.Application
import com.example.data.database.MeloVaultDatabase
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager

class MeloVaultApplication : Application() {

    lateinit var database: MeloVaultDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var playerManager: PlayerManager
        private set

    companion object {
        lateinit var instance: MeloVaultApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = MeloVaultDatabase.getDatabase(this)
        repository = MusicRepository(this, database)
        playerManager = PlayerManager.getInstance(this, repository)
    }
}
