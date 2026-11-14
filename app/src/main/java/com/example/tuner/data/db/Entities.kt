package com.example.tuner.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tuner.system.GameMode

enum class GraphicsMode { HIGH, MEDIUM, LOW, ULTRA_LOW }

// Skema acuan: replica/schema.sql. Room tidak membuat CHECK constraint, jadi nilai divalidasi di kode.

@Entity(
    tableName = "game_entry",
    indices = [Index(value = ["is_installed", "is_pinned", "label"], name = "idx_game_entry_list")],
)
data class GameEntryEntity(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    val label: String,
    @ColumnInfo(name = "is_manual") val isManual: Boolean = false,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "is_installed") val isInstalled: Boolean = true,
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "game_profile",
    foreignKeys = [
        ForeignKey(
            entity = GameEntryEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tune_enabled"], name = "idx_game_profile_tuned")],
)
data class GameProfileEntity(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "tune_enabled") val tuneEnabled: Boolean = false,
    @ColumnInfo(name = "game_mode") val gameMode: GameMode = GameMode.STANDARD,
    @ColumnInfo(name = "graphics_mode") val graphicsMode: GraphicsMode = GraphicsMode.HIGH,
    @ColumnInfo(name = "downscale_ratio") val downscaleRatio: Float? = null,
    @ColumnInfo(name = "fps_cap") val fpsCap: Int? = null,
    @ColumnInfo(name = "cpu_boost") val cpuBoost: Boolean = false,
    @ColumnInfo(name = "last_applied_at") val lastAppliedAt: Long? = null,
    @ColumnInfo(name = "last_apply_status") val lastApplyStatus: String? = null, // OK | PARTIAL | FAILED
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "apply_log",
    foreignKeys = [
        ForeignKey(
            entity = GameEntryEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["package_name", "created_at"], name = "idx_apply_log_pkg_time"),
        Index(value = ["created_at"], name = "idx_apply_log_time"),
    ],
)
data class ApplyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scope: String, // GAME | SYSTEM | MODULE
    @ColumnInfo(name = "package_name") val packageName: String?,
    val action: String,
    val command: String,
    val backend: String, // SHIZUKU | ROOT
    @ColumnInfo(name = "exit_code") val exitCode: Int?,
    val output: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Baris untuk daftar S03: entry + ringkasan profil (kolom profil null kalau belum pernah di-tune). */
data class GameRow(
    val packageName: String,
    val label: String,
    val isPinned: Boolean,
    val tuneEnabled: Boolean?,
    val gameMode: GameMode?,
    val lastApplyStatus: String?,
)
