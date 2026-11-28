package ru.mtuci.drivenext.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "favorites", primaryKeys = ["owner", "carId"])
data class FavoriteEntity(val owner: String, val carId: Long)

@Dao
interface FavoriteDao {
    @Query("SELECT carId FROM favorites WHERE owner = :owner ORDER BY carId")
    suspend fun ids(owner: String): List<Long>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE owner = :owner AND carId = :carId")
    suspend fun remove(owner: String, carId: Long)
}
@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    companion object {
        @Volatile private var instance: FavoritesDatabase? = null
        fun get(context: Context): FavoritesDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, FavoritesDatabase::class.java, "drivenext_favorites.db").build().also { instance = it }
        }
    }
}
