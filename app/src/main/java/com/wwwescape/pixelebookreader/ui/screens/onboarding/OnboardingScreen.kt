package com.wwwescape.pixelebookreader.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wwwescape.pixelebookreader.R

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit, modifier: Modifier = Modifier) {
    // Shown directly from MainActivity instead of PixelEBookReaderApp/Scaffold (see
    // MainActivity.onCreate), so — unlike every other screen — it doesn't automatically inherit
    // Scaffold's background/content-color painting. A bare Column here would show whatever the
    // raw Activity window background happens to be (not this composition's actual light/dark
    // theme) and fall back to Compose's unthemed default content color for any Text/Icon without
    // an explicit color. Surface reproduces Scaffold's own background+content-color behavior.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.onboarding_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                OnboardingFeature(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.onboarding_feature_formats_title), stringResource(R.string.onboarding_feature_formats_body))
                OnboardingFeature(Icons.Rounded.Palette, stringResource(R.string.onboarding_feature_customization_title), stringResource(R.string.onboarding_feature_customization_body))
                OnboardingFeature(Icons.Rounded.Widgets, stringResource(R.string.onboarding_feature_library_title), stringResource(R.string.onboarding_feature_library_body))
                OnboardingFeature(Icons.Rounded.Lock, stringResource(R.string.onboarding_feature_privacy_title), stringResource(R.string.onboarding_feature_privacy_body))
            }

            Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
                Text(stringResource(R.string.action_get_started))
            }
        }
    }
}

@Composable
private fun OnboardingFeature(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp).padding(top = 2.dp))
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
