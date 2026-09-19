package com.nothatcher.sproutbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private lateinit var soundMachine: SoundMachine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SproutRepository(this)
        soundMachine = SoundMachine()

        setContent {
            SproutBookTheme {
                SproutBookApp(repository, soundMachine)
            }
        }
    }

    override fun onDestroy() {
        soundMachine.stop()
        super.onDestroy()
    }
}

enum class RootTab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Rounded.Home),
    Schedule("Schedule", Icons.Rounded.CalendarMonth),
    Care("Care", Icons.Rounded.VolunteerActivism),
    More("More", Icons.Rounded.MoreHoriz)
}

enum class Tool {
    Feeding,
    Solids,
    SleepSound,
    Teeth,
    Milestones,
    MomAdvice,
    DadAdvice,
    Pregnancy,
    HelpNow,
    Inventory,
    Emergency,
    ProfileBackup
}

@Composable
fun SproutBookApp(repository: SproutRepository, soundMachine: SoundMachine) {
    var state by remember { mutableStateOf(repository.load()) }
    var tab by remember { mutableStateOf(RootTab.Today) }
    var tool by remember { mutableStateOf<Tool?>(null) }

    fun update(newState: SproutState) {
        val normalized = newState.normalized()
        state = normalized
        repository.save(normalized)
    }

    BackHandler(enabled = tool != null) { tool = null }

    DisposableEffect(Unit) {
        onDispose { soundMachine.stop() }
    }

    val child = state.selectedChild()

    Scaffold(
        containerColor = SproutBackground,
        topBar = {
            SproutTopBar(
                state = state,
                onSelectChild = { id -> update(state.copy(selectedChildId = id)) },
                onProfile = { tool = Tool.ProfileBackup }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SproutBackground) {
                RootTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item && tool == null,
                        onClick = {
                            soundMachine.stop()
                            tool = null
                            tab = item
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val activeTool = tool
            if (activeTool == null) {
                when (tab) {
                    RootTab.Today -> TodayScreen(
                        state = state,
                        onUpdate = ::update,
                        onOpenTool = { tool = it }
                    )
                    RootTab.Schedule -> ScheduleScreen(
                        state = state,
                        onUpdate = ::update
                    )
                    RootTab.Care -> CareScreen(
                        child = child,
                        onOpenTool = { tool = it }
                    )
                    RootTab.More -> MoreScreen(
                        state = state,
                        onUpdate = ::update,
                        onOpenTool = { tool = it }
                    )
                }
            } else {
                ToolScreen(
                    tool = activeTool,
                    state = state,
                    repository = repository,
                    soundMachine = soundMachine,
                    onUpdate = ::update,
                    onBack = {
                        soundMachine.stop()
                        tool = null
                    }
                )
            }
        }
    }
}

@Composable
private fun SproutTopBar(
    state: SproutState,
    onSelectChild: (String) -> Unit,
    onProfile: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val child = state.selectedChild()

    Surface(color = SproutBackground, tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.weight(1f)) {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SproutSurface2,
                        contentColor = SproutText
                    )
                ) {
                    Text(child.name, maxLines = 1)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    state.children.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = {
                                onSelectChild(item.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onProfile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SproutSurface,
                    contentColor = SproutPrimary
                )
            ) {
                Text(child.stage.name, fontSize = 14.sp)
            }

            Text(
                "SproutBook",
                color = SproutPrimary,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
fun ToolScreen(
    tool: Tool,
    state: SproutState,
    repository: SproutRepository,
    soundMachine: SoundMachine,
    onUpdate: (SproutState) -> Unit,
    onBack: () -> Unit
) {
    when (tool) {
        Tool.Feeding -> FeedingScreen(state, onUpdate, onBack)
        Tool.Solids -> SolidsScreen(state, onUpdate, onBack)
        Tool.SleepSound -> SleepSoundScreen(state, onUpdate, soundMachine, onBack)
        Tool.Teeth -> TeethScreen(state, onUpdate, onBack)
        Tool.Milestones -> MilestonesScreen(state, onUpdate, onBack)
        Tool.MomAdvice -> ParentAdviceScreen(true, state, onBack)
        Tool.DadAdvice -> ParentAdviceScreen(false, state, onBack)
        Tool.Pregnancy -> PregnancyScreen(state, onUpdate, onBack)
        Tool.HelpNow -> HelpNowScreen(onBack)
        Tool.Inventory -> InventoryScreen(state, onUpdate, onBack)
        Tool.Emergency -> EmergencyScreen(state, onUpdate, onBack)
        Tool.ProfileBackup -> ProfileBackupScreen(state, repository, onUpdate, onBack)
    }
}
