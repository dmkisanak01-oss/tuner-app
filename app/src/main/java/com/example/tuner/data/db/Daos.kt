package com.example.tuner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameEntryDao {
    @Query(
        """
        SELECT e.package_name AS packageName, e.label AS label, e.is_pinned AS isPinned,
               p.tune_enabled AS tuneEnabled, p.game_mode AS gameMode, p.graphics_mode AS graphicsMode,
               p.fps_cap AS fpsCap, p.last_apply_status AS lastApplyStatus
        FROM game_entry e LEFT JOIN game_profile p ON p.package_name = e.package_name
        WHERE e.is_installed = 1
        ORDER BY e.is_pinned DESC, e.label COLLATE NOCASE
        """,
    )
    fun observeRows(): Flow<List<GameRow>>

    @Query("UPDATE game_entry SET is_pinned = :pinned, updated_at = :now WHERE package_name = :pkg")
    suspend fun setPinned(pkg: String, pinned: Boolean, now: Long)

    @Query("SELECT label FROM game_entry WHERE package_name = :pkg")
    fun observeLabel(pkg: String): Flow<String?>

    @Query("SELECT EXISTS(SELECT 1 FROM game_entry WHERE package_name = :pkg)")
    suspend fun exists(pkg: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entries: List<GameEntryEntity>)

    @Query("UPDATE game_entry SET label = :label, is_installed = 1, updated_at = :now WHERE package_name = :pkg")
    suspend fun refresh(pkg: String, label: String, now: Long)

    @Query("UPDATE game_entry SET is_installed = 0, updated_at = :now WHERE is_manual = 0 AND package_name NOT IN (:present)")
    suspend fun markMissing(present: List<String>, now: Long)
}

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profile WHERE package_name = :pkg")
    suspend fun get(pkg: String): GameProfileEntity?

    @Query("SELECT * FROM game_profile WHERE package_name = :pkg")
    fun observe(pkg: String): Flow<GameProfileEntity?>

    @Upsert
    suspend fun upsert(profile: GameProfileEntity)
}

@Dao
interface ApplyLogDao {
    @Insert
    suspend fun insert(log: ApplyLogEntity)

    @Query("SELECT * FROM apply_log WHERE package_name = :pkg ORDER BY created_at DESC, id DESC LIMIT 1")
    fun observeLast(pkg: String): Flow<ApplyLogEntity?>

    @Query("DELETE FROM apply_log WHERE id NOT IN (SELECT id FROM apply_log ORDER BY created_at DESC, id DESC LIMIT :keep)")
    suspend fun prune(keep: Int)
}
