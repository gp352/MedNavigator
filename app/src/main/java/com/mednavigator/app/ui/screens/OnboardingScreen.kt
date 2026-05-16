package com.mednavigator.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository

import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.theme.CozyOnPrimaryFixedVariant
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.theme.CozySurfaceContainerHighest
import com.mednavigator.app.ui.theme.CozySurfaceContainerLow
import com.mednavigator.app.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    navController: NavController,
    onboardingRepository: OnboardingRepository,
    viewModel: OnboardingViewModel = viewModel()
) {
    val name by viewModel.name.collectAsState()
    val age by viewModel.age.collectAsState()
    val selectedSex by viewModel.selectedSex.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    var sexExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    val inf = rememberInfiniteTransition(label = "glow")
    val glowA by inf.animateFloat(0.12f, 0.22f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g")
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = CozySurfaceContainerLow, focusedContainerColor = CozySurfaceContainerLow,
        unfocusedBorderColor = Color.Transparent, focusedBorderColor = CozyPrimary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, focusedLeadingIconColor = CozyPrimary
    )
    val dropdownColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = CozySurfaceContainerLow, focusedContainerColor = CozySurfaceContainerLow,
        disabledContainerColor = CozySurfaceContainerLow,
        unfocusedBorderColor = Color.Transparent, focusedBorderColor = CozyPrimary,
        disabledBorderColor = Color.Transparent,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, focusedLeadingIconColor = CozyPrimary,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp)
            .background(Brush.horizontalGradient(listOf(CozyPrimary, CozyPrimaryFixed, CozyPrimary)))
            .align(Alignment.TopCenter))
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(CozySurfaceContainerLow)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = CozyPrimary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().blur(40.dp).background(CozyPrimaryFixed.copy(alpha = glowA), CircleShape))
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(CozyPrimaryFixed), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, modifier = Modifier.size(72.dp), tint = CozyOnPrimaryFixedVariant)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text("Let's get started", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text("We'd love to know a bit about you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(36.dp))
            Text("What is your name?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp))
            OutlinedTextField(value = name, onValueChange = { viewModel.name.value = it },
                placeholder = { Text("Enter your name") }, leadingIcon = { Icon(Icons.Rounded.Person, null) },
                modifier = Modifier.fillMaxWidth().height(64.dp), singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(modifier = Modifier.height(24.dp))
            Text("How old are you?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp))
            OutlinedTextField(value = age, onValueChange = { viewModel.age.value = it },
                placeholder = { Text("Years") }, leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) },
                modifier = Modifier.fillMaxWidth().height(64.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp), colors = fieldColors)
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { sexExpanded = !sexExpanded }) {
                OutlinedTextField(value = selectedSex, onValueChange = {}, readOnly = true, enabled = false,
                    label = { Text("Biological Sex") }, leadingIcon = { Icon(Icons.Rounded.Wc, null) },
                    trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = dropdownColors)
                DropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                    viewModel.sexOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { viewModel.selectedSex.value = it; sexExpanded = false }) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { countryExpanded = !countryExpanded }) {
                OutlinedTextField(value = selectedCountry, onValueChange = {}, readOnly = true, enabled = false,
                    label = { Text("Country") }, leadingIcon = { Icon(Icons.Rounded.Public, null) },
                    trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = dropdownColors)
                DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                    viewModel.countryOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { viewModel.selectedCountry.value = it; countryExpanded = false }) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { languageExpanded = !languageExpanded }) {
                OutlinedTextField(value = viewModel.languageOptions[selectedLanguage] ?: "English", onValueChange = {}, readOnly = true, enabled = false,
                    label = { Text("Preferred Language") }, leadingIcon = { Icon(Icons.Rounded.Language, null) },
                    trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = dropdownColors)
                DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                    viewModel.languageOptions.forEach { (code, display) ->
                        DropdownMenuItem(text = { Text(display) }, onClick = { viewModel.selectedLanguage.value = code; languageExpanded = false })
                    }
                }
            }
            errorMessage?.let { Spacer(modifier = Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(modifier = Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(32.dp).height(8.dp).clip(CircleShape).background(CozyPrimary))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CozySurfaceContainerHighest))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CozySurfaceContainerHighest))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                if (viewModel.validateAndSave(onboardingRepository)) {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CozyPrimary)) {
                Text("Next", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your information is kept safe and secure in our digital living room.", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outlineVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
