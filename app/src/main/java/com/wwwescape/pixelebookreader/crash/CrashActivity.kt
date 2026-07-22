package com.wwwescape.pixelebookreader.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.ui.theme.PixelEBookReaderTheme
import java.io.File

/** Replaces the platform's default crash dialog — see [CrashHandler]. A plain [ComponentActivity],
 * not part of the main nav graph, since the app's own Compose UI may be in whatever state it was
 * in when it crashed. */
class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE).orEmpty()

        setContent {
            PixelEBookReaderTheme {
                CrashScreen(
                    stackTrace = stackTrace,
                    onRestart = {
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(launchIntent)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_STACK_TRACE = "stack_trace"
    }
}

@Composable
private fun CrashScreen(stackTrace: String, onRestart: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
            Icon(imageVector = Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = stringResource(R.string.crash_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.crash_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Text(
                    text = stackTrace,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                )
            }

            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(stackTrace)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_copy)) }
                    OutlinedButton(
                        onClick = { shareStackTrace(context, stackTrace) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_share)) }
                }
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.crash_action_restart))
                }
            }
        }
    }
}

private fun shareStackTrace(context: android.content.Context, stackTrace: String) {
    runCatching {
        val file = File(context.cacheDir, "crash_report.txt").apply { writeText(stackTrace) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
