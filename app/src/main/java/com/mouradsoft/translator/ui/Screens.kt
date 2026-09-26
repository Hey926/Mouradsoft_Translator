package com.mouradsoft.translator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mouradsoft.translator.R
import com.mouradsoft.translator.data.Direction
import com.mouradsoft.translator.session.SessionState
import com.mouradsoft.translator.session.Work

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    ScreenFrame(1, bottom = { PrimaryAction(stringResource(R.string.start), onStart) }) {
        Spacer(Modifier.height(Space.xs))
        Heading(stringResource(R.string.welcome_title), large = true)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = Palette.Muted)
        Robot(RobotMood.Welcome, Modifier.fillMaxWidth().height(280.dp))
        Text(stringResource(R.string.welcome_eyebrow), Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium,
            color = Palette.TealDark, textAlign = TextAlign.Center)
    }
}

@Composable
fun ChooseScreen(state: SessionState, onChoose: (Direction) -> Unit, onContinue: () -> Unit, onBack: () -> Unit) {
    ScreenFrame(2, onBack, bottom = {
        if (state.direction == null) Text(stringResource(R.string.choose_direction), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        PrimaryAction(stringResource(R.string.continue_action), onContinue, state.direction != null)
    }) {
        Heading(stringResource(R.string.language_title))
        Surface(shape = Corners.medium, color = Color.White) {
            Row(Modifier.fillMaxWidth().padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Palette.Aqua, Corners.small), contentAlignment = Alignment.Center) { Mark(Symbol.Bubble) }
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    Text(stringResource(state.language.label), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.choice_selected), style = MaterialTheme.typography.bodyMedium, color = Palette.TealDark)
                }
                Mark(Symbol.Check)
            }
        }
        Text(stringResource(R.string.language_note), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        Text(stringResource(R.string.direction_question), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Direction.entries.forEach { direction ->
                val selected = state.direction == direction
                Surface(shape = Corners.medium, color = if (selected) Palette.Aqua else Color.White,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.TealDark else Palette.Line)) {
                    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = { onChoose(direction) }).padding(Space.md),
                        verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                            Text(stringResource(direction.label), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(direction.description), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                            if (selected) Text(stringResource(R.string.choice_selected), style = MaterialTheme.typography.labelMedium, color = Palette.TealDark)
                        }
                        RadioButton(selected, onClick = null, modifier = Modifier.clearAndSetSemantics { })
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionScreen(isDemo: Boolean, onFinish: () -> Unit, onBack: () -> Unit) {
    ScreenFrame(4, onBack, bottom = { PrimaryAction(stringResource(R.string.finish), onFinish) }) {
        if (isDemo) DemoLabel()
        Robot(RobotMood.Celebrate, Modifier.fillMaxWidth().height(280.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(color = Palette.Aqua, shape = Corners.medium) { Mark(Symbol.Check, Modifier.padding(Space.sm).size(32.dp)) }
        }
        Heading(stringResource(R.string.done_title), large = true)
        Text(stringResource(R.string.done_body), style = MaterialTheme.typography.bodyLarge, color = Palette.Muted)
    }
}

@Composable
fun ResultScreen(state: SessionState, isDemo: Boolean, onRedo: () -> Unit, onBack: () -> Unit) {
    val translation = (state.work as? Work.Success)?.translation ?: return
    ScreenFrame(5, onBack, bottom = {
        PrimaryAction(stringResource(R.string.redo), onRedo, icon = Symbol.Spark)
        Text(stringResource(R.string.redo_helper), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
    }) {
        Heading(stringResource(R.string.result_title))
        if (isDemo) DemoLabel()
        Column {
            Surface(shape = Corners.large, color = Color.White, shadowElevation = 1.dp) {
                SelectionContainer {
                    Text(translation.text, Modifier.fillMaxWidth().padding(Space.lg), style = MaterialTheme.typography.titleLarge)
                }
            }
            // The tail ends directly above the explaining robot's head.
            Canvas(Modifier.padding(start = 52.dp).size(30.dp, 22.dp).clearAndSetSemantics { }) {
                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width * .35f, size.height); close() }, Color.White)
            }
            Robot(RobotMood.Explain, Modifier.size(132.dp))
        }
        if (translation.explanation.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.explanation_label), style = MaterialTheme.typography.labelLarge, color = Palette.TealDark)
                Text(translation.explanation, style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider(color = Palette.Line)
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(stringResource(R.string.you_said), style = MaterialTheme.typography.labelLarge)
            SelectionContainer { Text(state.input, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted) }
            state.direction?.let { Text(stringResource(it.label), style = MaterialTheme.typography.labelMedium, color = Palette.TealDark) }
        }
        if (!isDemo) Text(stringResource(R.string.ai_reminder), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
    }
}
