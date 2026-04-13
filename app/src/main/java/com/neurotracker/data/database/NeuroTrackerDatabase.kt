package com.neurotracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neurotracker.data.dao.TestResultDao
import com.neurotracker.data.dao.UserDao
import com.neurotracker.data.entities.UserEntity
import com.neurotracker.data.entities.TestResultEntity

/**
 * Base de datos Room de NeuroTracker.
 *
 * Define todas las entidades de la aplicación y expone los DAOs
 * para acceder a los datos de forma segura y eficiente.
 *
 * Entidades incluidas:
 *  - [UserEntity]: Almacena los datos de los usuarios registrados.
 *  - [TestResultEntity]: Almacena los resultados de los tests cognitivos.
 *
 * @property userDao DAO para operaciones con la tabla de usuarios.
 * @property testResultDao DAO para operaciones con la tabla de resultados de tests.
 */
@Database(
    entities = [
        UserEntity::class,
        TestResultEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class NeuroTrackerDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun testResultDao(): TestResultDao

    companion object {

        @Volatile
        private var INSTANCE: NeuroTrackerDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (Singleton).
         *
         * Implementa el patrón Singleton con inicialización perezosa (lazy)
         * y sincronización para garantizar una única instancia en toda la app.
         *
         * @param context Contexto de la aplicación.
         * @return Instancia única de [NeuroTrackerDatabase].
         */
        fun getDatabase(context: Context): NeuroTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NeuroTrackerDatabase::class.java,
                    "neurotracker_database"
                )
                    // BORRA y recrea la BD si cambia el esquema
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}