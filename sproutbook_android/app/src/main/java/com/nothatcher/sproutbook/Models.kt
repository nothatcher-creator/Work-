package com.nothatcher.sproutbook

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
enum class ChildStage { Pregnancy, Baby, Toddler, Kid, Teen }

@Serializable
data class Memory(
    val id: String = uid("mem"),
    val title: String,
    val notes: String = "",
    val date: String = LocalDate.now().toString(),
    val kind: String = "memory",
    val anchor: Int = -1
)

@Serializable
data class FeedEntry(
    val id: String = uid("feed"),
    val kind: String,
    val amount: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SolidFood(
    val id: String = uid("food"),
    val food: String,
    val reaction: String = "No reaction",
    val date: String = LocalDate.now().toString()
)

@Serializable
data class SleepEntry(
    val id: String = uid("sleep"),
    val minutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Contraction(
    val id: String = uid("con"),
    val startedAt: Long,
    val durationSeconds: Int
)

@Serializable
data class PregnancyData(
    val kickCount: Int = 0,
    val contractions: List<Contraction> = emptyList()
)

@Serializable
data class ChildProfile(
    val id: String = uid("child"),
    val name: String = "My Child",
    val stage: ChildStage = ChildStage.Baby,
    val birthday: String = "",
    val dueDate: String = "",
    val memories: List<Memory> = emptyList(),
    val feeds: List<FeedEntry> = emptyList(),
    val solids: List<SolidFood> = emptyList(),
    val mealPlan: Map<String, String> = emptyMap(),
    val teeth: Map<String, String> = emptyMap(),
    val milestones: Map<String, Boolean> = emptyMap(),
    val sleep: List<SleepEntry> = emptyList(),
    val pregnancy: PregnancyData = PregnancyData()
)

@Serializable
data class Appointment(
    val id: String = uid("appt"),
    val title: String,
    val date: String,
    val time: String = "",
    val location: String = "",
    val notes: String = "",
    val childId: String? = null
)

@Serializable
data class InventoryItem(
    val id: String = uid("inv"),
    val name: String,
    val quantity: Int = 1,
    val unit: String = "item",
    val lowAt: Int = 1
)

@Serializable
data class EmergencyCard(
    val allergies: String = "",
    val medications: String = "",
    val conditions: String = "",
    val doctor: String = "",
    val doctorPhone: String = "",
    val contacts: String = ""
)

@Serializable
data class AppSettings(
    val grandparentMode: Boolean = false,
    val reduceMotion: Boolean = false
)

@Serializable
data class SproutState(
    val children: List<ChildProfile> = listOf(ChildProfile()),
    val selectedChildId: String = "",
    val appointments: List<Appointment> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val emergency: EmergencyCard = EmergencyCard(),
    val settings: AppSettings = AppSettings()
) {
    fun normalized(): SproutState {
        val safeChildren = if (children.isEmpty()) listOf(ChildProfile()) else children
        val selected = selectedChildId.takeIf { id -> safeChildren.any { it.id == id } }
            ?: safeChildren.first().id
        return copy(children = safeChildren, selectedChildId = selected)
    }

    fun selectedChild(): ChildProfile =
        children.firstOrNull { it.id == selectedChildId } ?: children.first()
}

fun uid(prefix: String): String = "${prefix}-${UUID.randomUUID()}"

fun SproutState.updateChild(child: ChildProfile): SproutState =
    copy(children = children.map { if (it.id == child.id) child else it })
