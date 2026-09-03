package com.example.housekey

import android.app.Application
import com.example.housekey.data.KeyDatabase
import com.example.housekey.data.KeyRepository
import com.example.housekey.hce.EmulationStore
import com.example.housekey.hce.HceManager

/** Application entry point that wires up the singleton repository (manual DI). */
class HouseKeyApp : Application() {

    lateinit var repository: KeyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = KeyDatabase.get(this)
        repository = KeyRepository(
            dao = db.keyDao(),
            store = EmulationStore(this),
            hce = HceManager(this),
        )
    }
}
