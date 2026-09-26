package com.mouradsoft.translator.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import com.mouradsoft.translator.R
import com.mouradsoft.translator.data.DemoCatalog
import com.mouradsoft.translator.session.SessionState
import com.mouradsoft.translator.session.SessionViewModel
import com.mouradsoft.translator.session.Work
import com.mouradsoft.translator.speech.SpeechAvailability
import com.mouradsoft.translator.speech.SpeechController

@Composable
fun InputScreen(state: SessionState, viewModel: SessionViewModel) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val feedback = remember { BringIntoViewRequester() }
    val latestState by rememberUpdatedState(state)
    var speechConsent by remember { mutableStateOf(false) }
    var textConsent by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var speechMode by remember { mutableStateOf<SpeechAvailability?>(null) }
    val controller = remember(context, viewModel) {
        SpeechController(context, viewModel::transcript, { viewModel.speechProblem(it) }, viewModel::finishingSpeech)
    }
    fun cancelSpeech() { controller.cancel(); viewModel.endListening() }
    fun beginSpeech() {
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val availability = controller.availability()
        if (availability == SpeechAvailability.Missing) {
            viewModel.speechProblem(R.string.voice_missing)
            return
        }
        if (availability == SpeechAvailability.Service && !viewModel.state.value.cloudSpeechConsented) {
            speechConsent = true
            return
        }
        focus.clearFocus()
        speechMode = availability
        viewModel.beginListening()
        controller.start(latestState.language.speechTag, availability == SpeechAvailability.OnDevice)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) scope.launch { owner.lifecycle.withResumed { beginSpeech() } }
        else {
            val noRationale = (context as? Activity)?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false
            val permanent = permissionRequested && noRationale
            viewModel.speechProblem(if (permanent) R.string.voice_permanent else R.string.voice_denied, permanent)
        }
    }
    fun askPermissionOrStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginSpeech()
        else {
            permissionRequested = true
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun requestSpeech() {
        when (controller.availability()) {
            SpeechAvailability.Missing -> viewModel.speechProblem(R.string.voice_missing)
            SpeechAvailability.Service -> if (!latestState.cloudSpeechConsented) speechConsent = true else askPermissionOrStart()
            SpeechAvailability.OnDevice -> askPermissionOrStart()
        }
    }
    fun submit() {
        cancelSpeech()
        focus.clearFocus()
        if (!viewModel.isDemo && !latestState.liveTextConsented) textConsent = true else viewModel.translate()
    }
    DisposableEffect(owner, controller) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) cancelSpeech() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); cancelSpeech() }
    }
    val working = state.work is Work.Translating
    val listening = state.work as? Work.Listening
    LaunchedEffect(state.work) {
        if (state.work is Work.Failure || state.work is Work.Clarification) feedback.bringIntoView()
    }
    ScreenFrame(3, onBack = { cancelSpeech(); viewModel.back() }, bottom = {
        if (working) {
            Row(Modifier.fillMaxWidth().padding(Space.sm).semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Space.md))
                Text(stringResource(R.string.working), style = MaterialTheme.typography.bodyMedium)
            }
        } else PrimaryAction(stringResource(if (state.work is Work.Failure) R.string.retry else R.string.translate), ::submit,
            enabled = state.canTranslate, icon = Symbol.Spark)
    }) {
        Heading(stringResource(R.string.input_title))
        state.direction?.let {
            Text(stringResource(R.string.selection_summary, stringResource(state.language.label), stringResource(it.label)),
                style = MaterialTheme.typography.labelMedium, color = Palette.TealDark)
        }
        if (viewModel.isDemo) DemoLabel()
        else Text(stringResource(R.string.live_notice), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)

        Surface(shape = Corners.large, color = Palette.Aqua) {
            Column(Modifier.fillMaxWidth().padding(Space.md), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                if (working) Robot(RobotMood.Thinking, Modifier.size(100.dp))
                else {
                    FilledTonalButton(onClick = {
                        if (listening == null) requestSpeech()
                        else if (listening.finishing) cancelSpeech() else controller.stop()
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = Corners.medium,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Palette.TealDark, contentColor = androidx.compose.ui.graphics.Color.White)) {
                        Mark(if (listening == null) Symbol.Mic else Symbol.Stop, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(Modifier.width(Space.sm))
                        Text(stringResource(when { listening == null -> R.string.tap_speak; listening.finishing -> R.string.cancel_listening; else -> R.string.stop_listening }))
                    }
                    Text(stringResource(when { listening == null -> R.string.review_words; listening.finishing -> R.string.finishing_speech; else -> R.string.listening }),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (listening != null && speechMode != null) Text(stringResource(if (speechMode == SpeechAvailability.OnDevice) R.string.voice_local else R.string.voice_remote),
                        style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                }
            }
        }
        state.speechNotice?.let { message ->
            Notice(stringResource(message), error = true) {
                if (state.needsSettings) TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                }) { Text(stringResource(R.string.open_settings)) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            OutlinedTextField(value = state.input, onValueChange = { cancelSpeech(); viewModel.edit(it) },
                modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8,
                enabled = !working, shape = Corners.medium,
                label = { Text(stringResource(R.string.message_label)) },
                placeholder = { Text(stringResource(R.string.message_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.bodyLarge,
                supportingText = { Text(stringResource(R.string.counter, state.input.length), Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.White))
            Text(stringResource(R.string.message_helper), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            if (state.limitReached) Text(stringResource(R.string.limit_reached), style = MaterialTheme.typography.bodyMedium, color = Palette.Error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Box(Modifier.bringIntoViewRequester(feedback)) { when (val work = state.work) {
            is Work.Failure -> Notice(stringResource(work.problem.message), error = true)
            is Work.Clarification -> Notice(stringResource(R.string.clarify_title)) {
                Text(work.question, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.clarify_helper), style = MaterialTheme.typography.bodyMedium)
            }
            else -> Unit
        } }
        if (!working) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.try_example), style = MaterialTheme.typography.labelLarge)
                state.direction?.let { direction -> DemoCatalog.examples(direction).forEach { example ->
                    val phrase = stringResource(example.input)
                    OutlinedButton(onClick = { cancelSpeech(); viewModel.edit(phrase) }, shape = Corners.small,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(Space.sm)) {
                        Text(phrase, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                    }
                } }
            }
        }
        Text(stringResource(R.string.privacy_reminder), style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
    }
    if (speechConsent) AlertDialog(onDismissRequest = { speechConsent = false },
        title = { Text(stringResource(R.string.voice_consent_title)) }, text = { Text(stringResource(R.string.voice_consent_body)) },
        confirmButton = { TextButton(onClick = { speechConsent = false; viewModel.consentSpeech(); askPermissionOrStart() }) { Text(stringResource(R.string.voice_allow)) } },
        dismissButton = { TextButton(onClick = { speechConsent = false }) { Text(stringResource(R.string.type_instead)) } })
    if (textConsent) AlertDialog(onDismissRequest = { textConsent = false },
        title = { Text(stringResource(R.string.live_consent_title)) }, text = { Text(stringResource(R.string.live_consent_body)) },
        confirmButton = { TextButton(onClick = { textConsent = false; viewModel.consentText(); viewModel.translate() }) { Text(stringResource(R.string.send_words)) } },
        dismissButton = { TextButton(onClick = { textConsent = false }) { Text(stringResource(R.string.keep_editing)) } })
}
