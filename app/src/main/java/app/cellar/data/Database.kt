package app.cellar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** What a bottle holds. */
enum class Kind {
    /** Something you want to buy for yourself: it ages, then you decide. */
    WISH,

    /** An idea for a present: it waits on the gift shelf for its day. */
    GIFT;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: WISH
    }
}

/** Where a bottle stands. */
enum class State {
    /** On the rack, ageing (ripe once its time is up). */
    AGING,

    /** Opened: bought, after it aged (or before, if [Bottle.early]). */
    OPENED,

    /** Poured out: the wish faded, the money went to the barrel. */
    POURED,

    /** A gift bottle that was given. */
    GIVEN;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: AGING
    }
}

/** Why it was wanted right then: the one question asked when a bottle is laid. */
enum class Reason {
    NEED, BORED, SAD, AD, ENVY, SALE;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: NEED
    }
}

/** How an opened bottle turned out, a month later. */
enum class Verdict {
    /** Still a joy. */
    VINTAGE,

    /** Fine, nothing more. */
    SOSO,

    /** A waste. */
    VINEGAR;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s }
    }
}

/**
 * One wish laid down to age. [price] is in whole units of the cellar's currency. [days] is the ageing
 * it was laid for (already doubled if laid in a storm, see [storm]); [extraDays] were added at tastings.
 * [shape] picks the glass and the label. [dreamId] is the barrel its money went into when poured.
 */
@Entity(tableName = "bottles", indices = [Index("state")])
data class Bottle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String = Kind.WISH.name,
    val name: String,
    val price: Long = 0,
    val url: String = "",
    val note: String = "",
    val reason: String = Reason.NEED.name,
    val laidAt: Long = System.currentTimeMillis(),
    val days: Int = 14,
    val extraDays: Int = 0,
    val storm: Boolean = false,
    val state: String = State.AGING.name,
    val endedAt: Long = 0,
    /** Opened before it was ripe. */
    val early: Boolean = false,
    val verdict: String? = null,
    val verdictAt: Long = 0,
    val dreamId: Long = 0,
    /** A picture in the app's photos folder. */
    val photo: String? = null,
    val shape: Long = 0,
    /** Gift bottles: who it's for and the day it's for (0 = no date). */
    val giftFor: String = "",
    val giftAt: Long = 0,
    /** A notification already said it's ripe. */
    val told: Boolean = false,
) {
    val kindOf: Kind get() = Kind.of(kind)
    val stateOf: State get() = State.of(state)
    val reasonOf: Reason get() = Reason.of(reason)
    val verdictOf: Verdict? get() = Verdict.of(verdict)
}

/** A barrel and the dream chalked on it. [target] 0 means no sum was set; [sealedAt] > 0 once it was full and sealed. */
@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val sealedAt: Long = 0,
)

@Dao
interface CellarDao {
    @Query("SELECT * FROM bottles ORDER BY laidAt")
    fun observeBottles(): Flow<List<Bottle>>

    @Query("SELECT * FROM bottles ORDER BY laidAt")
    suspend fun bottles(): List<Bottle>

    @Query("SELECT * FROM bottles WHERE id = :id")
    suspend fun bottle(id: Long): Bottle?

    @Insert
    suspend fun insert(b: Bottle): Long

    @Update
    suspend fun update(b: Bottle)

    @Query("DELETE FROM bottles WHERE id = :id")
    suspend fun deleteBottle(id: Long)

    @Query("SELECT * FROM dreams ORDER BY startedAt")
    fun observeDreams(): Flow<List<Dream>>

    @Query("SELECT * FROM dreams ORDER BY startedAt")
    suspend fun dreams(): List<Dream>

    @Insert
    suspend fun insert(d: Dream): Long

    @Update
    suspend fun update(d: Dream)
}

@Database(entities = [Bottle::class, Dream::class], version = 1, exportSchema = true)
abstract class CellarDatabase : RoomDatabase() {
    abstract fun dao(): CellarDao

    companion object {
        fun create(context: Context): CellarDatabase =
            Room.databaseBuilder(context.applicationContext, CellarDatabase::class.java, "cellar.db").build()
    }
}
