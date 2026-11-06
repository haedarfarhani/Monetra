package ir.heydar.monetra.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "icon")
    val icon: String = "",
    @ColumnInfo(name = "color")
    val color: String = "#FF9800",
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false
)

