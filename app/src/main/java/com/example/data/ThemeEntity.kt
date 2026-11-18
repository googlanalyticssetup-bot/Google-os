package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class ThemeEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String,
  val isBuiltIn: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val payloadJson: String,
)
