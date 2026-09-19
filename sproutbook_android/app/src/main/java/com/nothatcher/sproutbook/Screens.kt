package com.nothatcher.sproutbook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CrisisAlert
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PregnantWoman
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = SproutSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = SproutMuted, style = MaterialTheme.typography.bodyMedium)
            }
            content()
        }
    }
}

@Composable
private fun ToolHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = SproutMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = SproutPrimary, contentColor = Color(0xFF251B10))
    ) { Text(text) }
}

@Composable
private fun ToolTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (accent) Color(0xFF2B2A1E) else SproutSurface
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (accent) SproutPrimary else SproutGreen)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(subtitle, color = SproutMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun TodayScreen(
    state: SproutState,
    onUpdate: (SproutState) -> Unit,
    onOpenTool: (Tool) -> Unit
) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode
    var arranging by remember { mutableStateOf(false) }
    var addMemory by remember { mutableStateOf(false) }
    var openMemory by remember { mutableStateOf<Memory?>(null) }

    ScreenColumn {
        Text(
            "Today with " + child.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(child.stage.name + " mode", color = SproutPrimary)

        SectionCard("Quick log", "The things you need most are one tap away.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Feeding", { onOpenTool(Tool.Feeding) }, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onOpenTool(Tool.SleepSound) }, modifier = Modifier.weight(1f)) {
                    Text("Sleep + sound")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (!readOnly) addMemory = true }, modifier = Modifier.weight(1f), enabled = !readOnly) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Memory")
                }
                OutlinedButton(onClick = { onOpenTool(Tool.HelpNow) }, modifier = Modifier.weight(1f)) {
                    Text("Help now")
                }
            }
        }

        SectionCard(
            "Memory Tree",
            if (arranging) "Tap a leaf, then tap a glowing branch spot — or long-press and drag the leaf." else "Tap a leaf to open the memory. Each new memory grows a leaf."
        ) {
            MemoryTree(
                memories = child.memories,
                arrangeMode = arranging,
                onLeafTap = { openMemory = it },
                onMoveMemory = { id, anchor ->
                    if (!readOnly) {
                        val updated = child.copy(
                            memories = child.memories.map { if (it.id == id) it.copy(anchor = anchor) else it }
                        )
                        onUpdate(state.updateChild(updated))
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = "+ Add memory",
                    onClick = { addMemory = true },
                    enabled = !readOnly,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { arranging = !arranging },
                    modifier = Modifier.weight(1f),
                    enabled = child.memories.isNotEmpty()
                ) {
                    Text(if (arranging) "Done" else "Arrange")
                }
            }
            Text(
                child.memories.size.toString() + if (child.memories.size == 1) " leaf" else " leaves",
                color = SproutMuted
            )
        }

        val next = state.appointments
            .filter { runCatching { LocalDate.parse(it.date) >= LocalDate.now() }.getOrDefault(false) }
            .sortedBy { it.date + it.time }
            .firstOrNull()
        SectionCard("Next up") {
            if (next == null) {
                Text("No upcoming appointments.", color = SproutMuted)
            } else {
                Text(next.title, fontWeight = FontWeight.SemiBold)
                Text(next.date + if (next.time.isNotBlank()) " · " + next.time else "", color = SproutMuted)
                if (next.location.isNotBlank()) Text(next.location, color = SproutMuted)
            }
        }

        if (child.memories.isNotEmpty()) {
            SectionCard("Recent memories") {
                child.memories.takeLast(3).reversed().forEach { memory ->
                    OutlinedButton(onClick = { openMemory = memory }, modifier = Modifier.fillMaxWidth()) {
                        Text(memory.title + " · " + memory.date, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    }
                }
            }
        }
    }

    if (addMemory) {
        MemoryEditorDialog(
            onDismiss = { addMemory = false },
            onSave = { title, notes, kind ->
                val anchor = child.memories.size % 24
                val updated = child.copy(memories = child.memories + Memory(title = title, notes = notes, kind = kind, anchor = anchor))
                onUpdate(state.updateChild(updated))
                addMemory = false
            }
        )
    }

    openMemory?.let { memory ->
        AlertDialog(
            onDismissRequest = { openMemory = null },
            title = { Text(memory.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(memory.date + " · " + memory.kind.replaceFirstChar { it.uppercase() }, color = SproutPrimary)
                    Text(memory.notes.ifBlank { "No notes." })
                }
            },
            confirmButton = {
                Button(onClick = { openMemory = null }) { Text("Close") }
            },
            dismissButton = if (!readOnly) {
                {
                    OutlinedButton(onClick = {
                        val updated = child.copy(memories = child.memories.filterNot { it.id == memory.id })
                        onUpdate(state.updateChild(updated))
                        openMemory = null
                    }) { Text("Delete") }
                }
            } else null
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("memory") }
    var kindsOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { kindsOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(kind.replaceFirstChar { it.uppercase() })
                    }
                    DropdownMenu(kindsOpen, { kindsOpen = false }) {
                        listOf("memory","first","milestone","funny","health","photo").forEach {
                            DropdownMenuItem(
                                text = { Text(it.replaceFirstChar { c -> c.uppercase() }) },
                                onClick = { kind = it; kindsOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    notes,
                    { notes = it },
                    label = { Text("What happened?") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title.trim(), notes.trim(), kind) }, enabled = title.isNotBlank()) {
                Text("Grow leaf")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(state: SproutState, onUpdate: (SproutState) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var appointmentDate by remember { mutableStateOf<LocalDate?>(null) }
    val readOnly = state.settings.grandparentMode

    ScreenColumn {
        Text("Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        SectionCard("Calendar", "Tap a day to inspect it. Long-press a day to create an appointment.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                Text(
                    month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + month.year,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("S","M","T","W","T","F","S").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = SproutMuted)
                }
            }

            val first = month.atDay(1)
            val leading = first.dayOfWeek.value % 7
            val cells = leading + month.lengthOfMonth()
            val rows = (cells + 6) / 7

            repeat(rows) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val cell = row * 7 + col
                        val day = cell - leading + 1
                        if (day !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).height(62.dp))
                        } else {
                            val date = month.atDay(day)
                            val appointmentCount = state.appointments.count { it.date == date.toString() }
                            val birthdayCount = state.children.count { birthdayMatches(it.birthday, date) }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .height(62.dp)
                                    .combinedClickable(
                                        onClick = { selectedDate = date },
                                        onLongClick = { if (!readOnly) appointmentDate = date }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (date == selectedDate) Color(0xFF33442F) else SproutSurface2
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    Modifier.fillMaxSize().padding(6.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(day.toString(), fontWeight = if (date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal)
                                    if (appointmentCount + birthdayCount > 0) {
                                        Text(
                                            "•".repeat(min(3, appointmentCount + birthdayCount)),
                                            color = if (birthdayCount > 0) SproutPrimary else SproutGreen
                                        )
                                    } else {
                                        Text(" ", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SectionCard("Selected day · " + selectedDate.toString()) {
            val birthdays = state.children.filter { birthdayMatches(it.birthday, selectedDate) }
            val appts = state.appointments.filter { it.date == selectedDate.toString() }.sortedBy { it.time }
            if (birthdays.isEmpty() && appts.isEmpty()) {
                Text("Nothing scheduled.", color = SproutMuted)
            }
            birthdays.forEach { Text("🎂 " + it.name + "'s birthday", color = SproutPrimary, fontWeight = FontWeight.SemiBold) }
            appts.forEach { appt ->
                Column {
                    Text(appt.title, fontWeight = FontWeight.SemiBold)
                    Text((appt.time + " " + appt.location).trim(), color = SproutMuted)
                }
                HorizontalDivider(color = Color(0x2253705C))
            }
            PrimaryButton(
                "Add appointment",
                { appointmentDate = selectedDate },
                enabled = !readOnly,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard("Upcoming appointments") {
            val upcoming = state.appointments
                .filter { runCatching { LocalDate.parse(it.date) >= LocalDate.now() }.getOrDefault(false) }
                .sortedBy { it.date + it.time }
                .take(12)
            if (upcoming.isEmpty()) Text("No upcoming appointments.", color = SproutMuted)
            upcoming.forEach { appt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(appt.title, fontWeight = FontWeight.SemiBold)
                        Text(appt.date + if (appt.time.isBlank()) "" else " · " + appt.time, color = SproutMuted)
                    }
                    if (!readOnly) {
                        OutlinedButton(onClick = { onUpdate(state.copy(appointments = state.appointments.filterNot { it.id == appt.id })) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    appointmentDate?.let { date ->
        AppointmentDialog(
            date = date,
            children = state.children,
            onDismiss = { appointmentDate = null },
            onSave = { appointment ->
                onUpdate(state.copy(appointments = state.appointments + appointment))
                appointmentDate = null
                selectedDate = date
            }
        )
    }
}

private fun birthdayMatches(birthday: String, date: LocalDate): Boolean {
    return runCatching {
        val b = LocalDate.parse(birthday)
        b.monthValue == date.monthValue && b.dayOfMonth == date.dayOfMonth
    }.getOrDefault(false)
}

@Composable
private fun AppointmentDialog(
    date: LocalDate,
    children: List<ChildProfile>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var childId by remember { mutableStateOf<String?>(null) }
    var childMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appointment · " + date.toString()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Time (e.g. 2:30 PM)") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2)
                Box {
                    OutlinedButton(onClick = { childMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(children.firstOrNull { it.id == childId }?.name ?: "All children / family")
                    }
                    DropdownMenu(childMenu, { childMenu = false }) {
                        DropdownMenuItem(text = { Text("All children / family") }, onClick = { childId = null; childMenu = false })
                        children.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { childId = it.id; childMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Appointment(
                            title = title.trim(),
                            date = date.toString(),
                            time = time.trim(),
                            location = location.trim(),
                            notes = notes.trim(),
                            childId = childId
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CareScreen(child: ChildProfile, onOpenTool: (Tool) -> Unit) {
    ScreenColumn {
        Text("Care", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Tools for " + child.name + " · " + child.stage.name, color = SproutMuted)

        ToolTile("Feeding", "Bottle, breast and pumping logs", Icons.Rounded.LocalDrink, { onOpenTool(Tool.Feeding) })
        ToolTile("Sleep + sound", "Sleep log plus native white/brown noise", Icons.Rounded.Nightlight, { onOpenTool(Tool.SleepSound) })
        ToolTile("Solids", "Age guide, food tracker and meal planner", Icons.Rounded.DinnerDining, { onOpenTool(Tool.Solids) })
        ToolTile("What do I do right now?", "Pick a problem and get three calm first steps", Icons.Rounded.CrisisAlert, { onOpenTool(Tool.HelpNow) }, true)
        ToolTile("Teeth", "Long-press teeth to cycle their stage", Icons.Rounded.HealthAndSafety, { onOpenTool(Tool.Teeth) })
        ToolTile("Milestones", "Stage-based checklist and notes", Icons.Rounded.ChildCare, { onOpenTool(Tool.Milestones) })
        ToolTile("Mom advice", "Breastfeeding, latching, calming and feeding support", Icons.Rounded.Favorite, { onOpenTool(Tool.MomAdvice) })
        ToolTile("Dad advice", "Calming, feeding support and survival tips", Icons.Rounded.Spa, { onOpenTool(Tool.DadAdvice) })
        ToolTile("Pregnancy", "Kick counter and contraction log", Icons.Rounded.PregnantWoman, { onOpenTool(Tool.Pregnancy) })
    }
}

@Composable
fun MoreScreen(
    state: SproutState,
    onUpdate: (SproutState) -> Unit,
    onOpenTool: (Tool) -> Unit
) {
    ScreenColumn {
        Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        ToolTile("Inventory", "Diapers, wipes, formula, medicine and household supplies", Icons.Rounded.Inventory2, { onOpenTool(Tool.Inventory) })
        ToolTile("Emergency card", "Allergies, medications, doctor and emergency contacts", Icons.Rounded.Emergency, { onOpenTool(Tool.Emergency) }, true)
        ToolTile("Profiles + settings", "Children, stages, backup and app settings", Icons.Rounded.Settings, { onOpenTool(Tool.ProfileBackup) })

        SectionCard("Grandparent mode", "Read-only mode prevents accidental edits while keeping information visible.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Read-only", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.settings.grandparentMode,
                    onCheckedChange = {
                        onUpdate(state.copy(settings = state.settings.copy(grandparentMode = it)))
                    }
                )
            }
        }
    }
}

@Composable
fun FeedingScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode
    var type by remember { mutableStateOf("Bottle") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ScreenColumn {
        ToolHeader("Feeding", onBack, child.name)
        SectionCard("Quick log") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Bottle","Breast","Pump").forEach { item ->
                    FilterChip(selected = type == item, onClick = { type = item }, label = { Text(item) })
                }
            }
            OutlinedTextField(
                amount,
                { amount = it },
                label = { Text(if (type == "Bottle" || type == "Pump") "Amount (mL / oz)" else "Minutes / side") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(
                "Save feeding",
                {
                    val updated = child.copy(
                        feeds = child.feeds + FeedEntry(kind = type, amount = amount.trim(), note = note.trim())
                    )
                    onUpdate(state.updateChild(updated))
                    amount = ""
                    note = ""
                },
                enabled = !readOnly && amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard("Recent feedings") {
            if (child.feeds.isEmpty()) Text("Nothing logged yet.", color = SproutMuted)
            child.feeds.takeLast(12).reversed().forEach {
                Text(it.kind + " · " + it.amount, fontWeight = FontWeight.SemiBold)
                Text(formatTimestamp(it.timestamp) + if (it.note.isBlank()) "" else " · " + it.note, color = SproutMuted)
                HorizontalDivider(color = Color(0x2253705C))
            }
        }
    }
}

@Composable
fun SolidsScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode
    var food by remember { mutableStateOf("") }
    var reaction by remember { mutableStateOf("No reaction") }
    var reactionOpen by remember { mutableStateOf(false) }
    val plan = remember(child.id, child.mealPlan) { mutableStateMapOf<String, String>().apply { putAll(child.mealPlan) } }

    ScreenColumn {
        ToolHeader("Solids", onBack, child.name)
        SectionCard("When to start", "Most babies are ready around 6 months when they can sit with support, control their head/neck and bring food to their mouth. Follow your clinician's advice for your baby.") {
            Text("Around 6 months: iron-rich purées/mashed foods, soft vegetables, fruit, oatmeal, egg, yogurt and very soft meats.")
            Text("6–9 months: thicker mash, soft finger foods, toast strips, ripe fruit, shredded tender meat.")
            Text("9–12 months: more family foods cut into safe shapes and textures.")
            Text("Avoid choking hazards such as whole nuts, popcorn, whole grapes, hard raw chunks, and spoonfuls of nut butter. Honey waits until after 12 months.", color = SproutPrimary)
        }

        SectionCard("Food tracker") {
            OutlinedTextField(food, { food = it }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedButton(onClick = { reactionOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(reaction) }
                DropdownMenu(reactionOpen, { reactionOpen = false }) {
                    listOf("No reaction","Liked it","Didn't like it","Possible reaction").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { reaction = it; reactionOpen = false })
                    }
                }
            }
            PrimaryButton(
                "Add food",
                {
                    val updated = child.copy(solids = child.solids + SolidFood(food = food.trim(), reaction = reaction))
                    onUpdate(state.updateChild(updated))
                    food = ""
                },
                enabled = !readOnly && food.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            child.solids.takeLast(10).reversed().forEach {
                Text(it.food + " · " + it.reaction + " · " + it.date)
            }
        }

        SectionCard("7-day meal planner", "Use this as a simple family plan, not a nutrition prescription.") {
            listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday").forEach { day ->
                OutlinedTextField(
                    value = plan[day] ?: "",
                    onValueChange = { plan[day] = it },
                    label = { Text(day + " · meals/snacks") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !readOnly
                )
            }
            PrimaryButton(
                "Save meal plan",
                { onUpdate(state.updateChild(child.copy(mealPlan = plan.toMap()))) },
                enabled = !readOnly,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SleepSoundScreen(
    state: SproutState,
    onUpdate: (SproutState) -> Unit,
    soundMachine: SoundMachine,
    onBack: () -> Unit
) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode
    var minutes by remember { mutableStateOf("60") }
    var playing by remember { mutableStateOf<String?>(null) }

    ScreenColumn {
        ToolHeader("Sleep + sound", onBack, child.name)
        SectionCard("Sound machine", "Generated on-device — no streaming or internet required.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("White noise","Brown noise","Fan","Rain").forEach { sound ->
                    FilterChip(
                        selected = playing == sound,
                        onClick = {
                            if (playing == sound) {
                                soundMachine.stop()
                                playing = null
                            } else {
                                soundMachine.play(sound)
                                playing = sound
                            }
                        },
                        label = { Text(sound) },
                        leadingIcon = { Icon(Icons.Rounded.VolumeUp, null, Modifier.size(18.dp)) }
                    )
                }
            }
            if (playing != null) {
                OutlinedButton(
                    onClick = { soundMachine.stop(); playing = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop sound") }
            }
        }

        SectionCard("Sleep log") {
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Minutes slept") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(
                "Save sleep",
                {
                    val mins = minutes.toIntOrNull() ?: 0
                    if (mins > 0) onUpdate(state.updateChild(child.copy(sleep = child.sleep + SleepEntry(minutes = mins))))
                },
                enabled = !readOnly && (minutes.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth()
            )
            child.sleep.takeLast(10).reversed().forEach {
                Text(it.minutes.toString() + " min · " + formatTimestamp(it.timestamp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeethScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode

    fun cycle(index: Int) {
        if (readOnly) return
        val key = index.toString()
        val current = child.teeth[key] ?: "none"
        val next = when (current) {
            "none" -> "seen"
            "seen" -> "erupted"
            else -> "none"
        }
        onUpdate(state.updateChild(child.copy(teeth = child.teeth + (key to next))))
    }

    ScreenColumn {
        ToolHeader("Teeth", onBack, "Long-press a tooth to cycle its stage.")
        SectionCard("Upper teeth") {
            repeat(2) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(5) { col ->
                        val index = rowIndex * 5 + col
                        ToothCell(index, child.teeth[index.toString()] ?: "none", { cycle(index) }, Modifier.weight(1f))
                    }
                }
            }
        }
        SectionCard("Lower teeth") {
            repeat(2) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(5) { col ->
                        val index = 10 + rowIndex * 5 + col
                        ToothCell(index, child.teeth[index.toString()] ?: "none", { cycle(index) }, Modifier.weight(1f))
                    }
                }
            }
        }
        Text("Stages: not seen → observed → erupted. Long-press is intentional so scrolling never changes a tooth by accident.", color = SproutMuted)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToothCell(index: Int, status: String, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val color = when (status) {
        "seen" -> Color(0xFF395A4D)
        "erupted" -> Color(0xFFE7DFC7)
        else -> SproutSurface2
    }
    val textColor = if (status == "erupted") Color(0xFF352C20) else SproutText
    Card(
        modifier = modifier
            .height(76.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🦷", fontSize = 24.sp)
            Text((index + 1).toString(), color = textColor, fontSize = 12.sp)
            Text(if (status == "none") "—" else if (status == "seen") "Seen" else "Up", color = textColor, fontSize = 10.sp)
        }
    }
}

@Composable
fun MilestonesScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode
    val items = milestoneList(child.stage)

    ScreenColumn {
        ToolHeader("Milestones", onBack, child.stage.name + " checklist")
        SectionCard("Development tracker", "A memory/organization tool — not a pass/fail test. Children develop at different rates.") {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = child.milestones[item] == true,
                        onCheckedChange = if (readOnly) null else { checked ->
                            onUpdate(state.updateChild(child.copy(milestones = child.milestones + (item to checked))))
                        }
                    )
                    Text(item, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun milestoneList(stage: ChildStage): List<String> = when (stage) {
    ChildStage.Pregnancy -> listOf("Prenatal appointment", "Movement noticed", "Sleep space ready", "Hospital bag ready")
    ChildStage.Baby -> listOf("Smiles socially", "Holds head steadier", "Rolls over", "Reaches for toys", "Sits with support", "Babbles", "Moves objects hand to hand", "Pulls to stand")
    ChildStage.Toddler -> listOf("Walks independently", "Uses several words", "Points to show interest", "Stacks blocks", "Uses spoon with help", "Runs", "Combines two words", "Pretend play")
    ChildStage.Kid -> listOf("Dresses with little help", "Follows multi-step directions", "Tells stories", "Writes or draws intentionally", "Takes turns", "Helps with simple chores")
    ChildStage.Teen -> listOf("Manages more responsibilities", "Develops independent interests", "Plans longer projects", "Builds stronger peer relationships", "Practices self-advocacy")
}

@Composable
fun ParentAdviceScreen(isMom: Boolean, state: SproutState, onBack: () -> Unit) {
    val child = state.selectedChild()
    var weightLb by remember { mutableStateOf("") }
    var feeds by remember { mutableStateOf("8") }
    val ozPerDayLow = weightLb.toDoubleOrNull()?.times(2.0)
    val ozPerDayHigh = weightLb.toDoubleOrNull()?.times(2.5)?.coerceAtMost(32.0)
    val feedsNum = feeds.toIntOrNull()?.coerceAtLeast(1) ?: 1

    ScreenColumn {
        ToolHeader(if (isMom) "Mom advice" else "Dad advice", onBack, child.name)

        if (isMom) {
            AdviceCard("Latch basics", "Bring baby to the breast rather than leaning toward baby. Aim the nipple toward the roof of the mouth, wait for a wide-open mouth, bring the chin in first, and look for more areola visible above the top lip than below.")
            AdviceCard("If latching hurts", "Break suction gently with a clean finger and try again rather than pushing through sharp pain. Cross-cradle can give more head control; football can help after a C-section or with larger breasts; laid-back and side-lying can be comfortable once established.")
            AdviceCard("Different positions", "Try cross-cradle for control, football for a clear view, laid-back for gravity-assisted attachment, and side-lying for rest. A good latch should feel like pulling/tugging rather than pinching.")
            AdviceCard("Calming a crying baby", "Check hunger, diaper, temperature and obvious pain first. Reduce stimulation, hold close, use steady shushing/white noise and gentle movement. If you're overloaded, put baby safely in the crib and take a short reset.")
        } else {
            AdviceCard("Breastfeeding support", "Bring water, snacks and pillows, handle burping/diapers afterward, protect quiet feeding time, and help track questions for a lactation consultant if feeding is painful or weight/wet diapers are concerning.")
            AdviceCard("Calming toolkit", "Work through basics first, then dim lights, hold baby close, use steady shushing or white noise, sway slowly, offer a pacifier if used, or step outside for a change of sensory input.")
            AdviceCard("Surviving the rough stretches", "Trade shifts when possible. If frustration is climbing, put baby safely in the crib and reset. Never shake a baby. Ask another adult to take over before you're at the breaking point.")
        }

        SectionCard("Formula planning calculator", "Rough planning estimate only — feeding needs vary by age and baby. Always follow the formula label and your baby's clinician, especially for newborns, premature babies or growth concerns.") {
            OutlinedTextField(weightLb, { weightLb = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Baby weight (lb)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(feeds, { feeds = it.filter(Char::isDigit) }, label = { Text("Feeds per day") }, modifier = Modifier.fillMaxWidth())
            if (ozPerDayLow != null && ozPerDayHigh != null) {
                val perFeedLow = ozPerDayLow / feedsNum
                val perFeedHigh = ozPerDayHigh / feedsNum
                Text("Common rough range: " + "%.1f".format(ozPerDayLow) + "–" + "%.1f".format(ozPerDayHigh) + " fl oz/day")
                Text("Across " + feedsNum + " feeds: about " + "%.1f".format(perFeedLow) + "–" + "%.1f".format(perFeedHigh) + " fl oz/feed")
                Text("Do not force a baby to finish a bottle. Hunger/fullness cues matter.", color = SproutPrimary)
            }
        }
    }
}

@Composable
private fun AdviceCard(title: String, body: String) {
    SectionCard(title) { Text(body) }
}

@Composable
fun PregnancyScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val child = state.selectedChild()
    val readOnly = state.settings.grandparentMode
    var contractionStart by remember { mutableStateOf<Long?>(null) }

    ScreenColumn {
        ToolHeader("Pregnancy", onBack, child.name)
        SectionCard("Kick counter", "Use the method your maternity care team recommends.") {
            Text(child.pregnancy.kickCount.toString(), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = SproutPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    "+ Kick",
                    {
                        onUpdate(state.updateChild(child.copy(pregnancy = child.pregnancy.copy(kickCount = child.pregnancy.kickCount + 1))))
                    },
                    enabled = !readOnly,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { onUpdate(state.updateChild(child.copy(pregnancy = child.pregnancy.copy(kickCount = 0)))) },
                    enabled = !readOnly,
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }
            }
        }

        SectionCard("Contractions", "Tap Start when one begins and Stop when it ends.") {
            if (contractionStart == null) {
                PrimaryButton("Start contraction", { contractionStart = System.currentTimeMillis() }, enabled = !readOnly, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton(
                    "Stop + save",
                    {
                        val start = contractionStart ?: return@PrimaryButton
                        val duration = ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(1)
                        val updatedPregnancy = child.pregnancy.copy(
                            contractions = child.pregnancy.contractions + Contraction(startedAt = start, durationSeconds = duration)
                        )
                        onUpdate(state.updateChild(child.copy(pregnancy = updatedPregnancy)))
                        contractionStart = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            child.pregnancy.contractions.takeLast(8).reversed().forEach {
                Text(formatTimestamp(it.startedAt) + " · " + it.durationSeconds + " sec")
            }
        }
    }
}

@Composable
fun HelpNowScreen(onBack: () -> Unit) {
    val problems = listOf(
        "Baby won't stop crying",
        "Baby won't latch",
        "Gas or spit-up",
        "Fever / seems unwell",
        "I'm overwhelmed",
        "Choking or trouble breathing"
    )
    var selected by remember { mutableStateOf(problems.first()) }
    var open by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }

    ScreenColumn {
        ToolHeader("What do I do right now?", onBack, "Three calm steps first.")
        SectionCard("Choose the problem") {
            Box {
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(selected) }
                DropdownMenu(open, { open = false }) {
                    problems.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { selected = item; open = false; show = false })
                    }
                }
            }
            PrimaryButton("Show 3 steps", { show = true }, modifier = Modifier.fillMaxWidth())
        }
        if (show) {
            val guide = helpGuide(selected)
            SectionCard("Do these first") {
                guide.first.forEachIndexed { index, step ->
                    Text((index + 1).toString() + ". " + step, fontSize = 18.sp)
                }
            }
            SectionCard("More context") { Text(guide.second, color = SproutMuted) }
        }
    }
}

private fun helpGuide(problem: String): Pair<List<String>, String> = when (problem) {
    "Baby won't stop crying" -> Pair(
        listOf("Check hunger, diaper, temperature and obvious pain.", "Lower stimulation: dim lights, hold close, use steady shushing/white noise and gentle movement.", "If you're overloaded, place baby safely in the crib and take a short reset."),
        "Persistent or unusual crying with fever, breathing trouble, poor responsiveness, dehydration signs or something that feels seriously wrong needs medical assessment."
    )
    "Baby won't latch" -> Pair(
        listOf("Reset positioning: belly-to-belly with baby's nose near the nipple.", "Wait for a wide mouth, then bring baby in close with the chin touching first.", "If it pinches or hurts sharply, break suction gently and relatch."),
        "Ongoing painful latch, poor milk transfer, fewer wet diapers or weight concerns are good reasons to contact a lactation professional or clinician."
    )
    "Gas or spit-up" -> Pair(
        listOf("Pause feeding and burp gently.", "Keep baby upright for a while after feeding.", "Try smaller, paced feeds and avoid pressure on the belly."),
        "Forceful or green vomit, blood, dehydration or poor weight gain should be medically assessed."
    )
    "Fever / seems unwell" -> Pair(
        listOf("Check the temperature with a reliable thermometer.", "Keep baby comfortable and offer normal feeds/fluids as appropriate.", "Write down the temperature, symptoms, wet diapers and when it started."),
        "Young infants with fever and any child with trouble breathing, severe lethargy, seizure, blue/gray color, dehydration or rapidly worsening symptoms need urgent medical care."
    )
    "I'm overwhelmed" -> Pair(
        listOf("Put baby somewhere safe, such as the crib.", "Step away for a few minutes, drink water and slow your breathing.", "Tag another trusted adult in if one is available."),
        "Never shake a baby. If you feel you may lose control, keep baby safe and get immediate help from someone you trust or emergency services if needed."
    )
    else -> Pair(
        listOf("Call emergency services if the child cannot breathe, cry or cough effectively.", "Use age-appropriate choking first aid if you know it; do not blindly sweep the mouth.", "If they become unresponsive, follow emergency-dispatch instructions and begin appropriate CPR."),
        "Choking or serious breathing trouble is an emergency and should not be troubleshot inside the app."
    )
}

@Composable
fun InventoryScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val readOnly = state.settings.grandparentMode
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var lowAt by remember { mutableStateOf("1") }

    ScreenColumn {
        ToolHeader("Inventory", onBack)
        SectionCard("Add supply") {
            OutlinedTextField(name, { name = it }, label = { Text("Item") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("Quantity") }, modifier = Modifier.weight(1f))
                OutlinedTextField(lowAt, { lowAt = it.filter(Char::isDigit) }, label = { Text("Low at") }, modifier = Modifier.weight(1f))
            }
            PrimaryButton(
                "Add item",
                {
                    onUpdate(state.copy(inventory = state.inventory + InventoryItem(name = name.trim(), quantity = quantity.toIntOrNull() ?: 1, lowAt = lowAt.toIntOrNull() ?: 1)))
                    name = ""
                },
                enabled = !readOnly && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard("Supplies") {
            if (state.inventory.isEmpty()) Text("No items yet.", color = SproutMuted)
            state.inventory.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                        Text(item.quantity.toString() + " " + item.unit + if (item.quantity <= item.lowAt) " · LOW" else "", color = if (item.quantity <= item.lowAt) SproutPrimary else SproutMuted)
                    }
                    OutlinedButton(
                        onClick = {
                            onUpdate(state.copy(inventory = state.inventory.map { if (it.id == item.id) it.copy(quantity = (it.quantity - 1).coerceAtLeast(0)) else it }))
                        },
                        enabled = !readOnly
                    ) { Text("−") }
                    Spacer(Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = {
                            onUpdate(state.copy(inventory = state.inventory.map { if (it.id == item.id) it.copy(quantity = it.quantity + 1) else it }))
                        },
                        enabled = !readOnly
                    ) { Text("+") }
                }
            }
        }
    }
}

@Composable
fun EmergencyScreen(state: SproutState, onUpdate: (SproutState) -> Unit, onBack: () -> Unit) {
    val readOnly = state.settings.grandparentMode
    var allergies by remember(state.emergency) { mutableStateOf(state.emergency.allergies) }
    var meds by remember(state.emergency) { mutableStateOf(state.emergency.medications) }
    var conditions by remember(state.emergency) { mutableStateOf(state.emergency.conditions) }
    var doctor by remember(state.emergency) { mutableStateOf(state.emergency.doctor) }
    var doctorPhone by remember(state.emergency) { mutableStateOf(state.emergency.doctorPhone) }
    var contacts by remember(state.emergency) { mutableStateOf(state.emergency.contacts) }

    ScreenColumn {
        ToolHeader("Emergency card", onBack, "Stored locally on this phone.")
        SectionCard("Important information") {
            OutlinedTextField(allergies, { allergies = it }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            OutlinedTextField(meds, { meds = it }, label = { Text("Medications") }, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            OutlinedTextField(conditions, { conditions = it }, label = { Text("Conditions / notes") }, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            OutlinedTextField(doctor, { doctor = it }, label = { Text("Doctor / clinic") }, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            OutlinedTextField(doctorPhone, { doctorPhone = it }, label = { Text("Doctor phone") }, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            OutlinedTextField(contacts, { contacts = it }, label = { Text("Emergency contacts") }, modifier = Modifier.fillMaxWidth(), enabled = !readOnly)
            PrimaryButton(
                "Save card",
                {
                    onUpdate(state.copy(emergency = EmergencyCard(allergies, meds, conditions, doctor, doctorPhone, contacts)))
                },
                enabled = !readOnly,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProfileBackupScreen(
    state: SproutState,
    repository: SproutRepository,
    onUpdate: (SproutState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val child = state.selectedChild()
    var name by remember(child.id, child.name) { mutableStateOf(child.name) }
    var stage by remember(child.id, child.stage) { mutableStateOf(child.stage) }
    var birthday by remember(child.id, child.birthday) { mutableStateOf(child.birthday) }
    var dueDate by remember(child.id, child.dueDate) { mutableStateOf(child.dueDate) }
    var stageMenu by remember { mutableStateOf(false) }
    var addChild by remember { mutableStateOf(false) }
    var backupText by remember { mutableStateOf("") }

    ScreenColumn {
        ToolHeader("Profiles + settings", onBack)

        SectionCard("Current child") {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedButton(onClick = { stageMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(stage.name) }
                DropdownMenu(stageMenu, { stageMenu = false }) {
                    ChildStage.entries.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = { stage = it; stageMenu = false })
                    }
                }
            }
            OutlinedTextField(birthday, { birthday = it }, label = { Text("Birthday YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(
                "Save profile",
                {
                    onUpdate(state.updateChild(child.copy(name = name.ifBlank { "Child" }, stage = stage, birthday = birthday.trim(), dueDate = dueDate.trim())))
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { addChild = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add another child")
            }
        }

        SectionCard("Backup / import", "JSON stays readable and portable.") {
            PrimaryButton(
                "Copy backup",
                {
                    val text = repository.exportJson(state)
                    backupText = text
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("SproutBook backup", text))
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                backupText,
                { backupText = it },
                label = { Text("Backup JSON / paste here to import") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )
            OutlinedButton(
                onClick = {
                    repository.importJson(backupText)?.let(onUpdate)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import pasted backup") }
        }
    }

    if (addChild) {
        AddChildDialog(
            onDismiss = { addChild = false },
            onSave = { newChild ->
                onUpdate(state.copy(children = state.children + newChild, selectedChildId = newChild.id))
                addChild = false
            }
        )
    }
}

@Composable
private fun AddChildDialog(onDismiss: () -> Unit, onSave: (ChildProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(ChildStage.Baby) }
    var menu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add child") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Text(stage.name) }
                    DropdownMenu(menu, { menu = false }) {
                        ChildStage.entries.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { stage = it; menu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ChildProfile(name = name.trim(), stage = stage)) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    return runCatching {
        DateTimeFormatter.ofPattern("MMM d · h:mm a")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestamp))
    }.getOrDefault("")
}
