package com.mednavigator.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.viewmodel.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    var sexExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Rounded.LocalHospital,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Welcome to MedNavigator",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Let's get to know you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.name.value = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Rounded.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Age
        OutlinedTextField(
            value = age,
            onValueChange = { viewModel.age.value = it },
            label = { Text("Age") },
            leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Sex dropdown
        ExposedDropdownMenuBox(
            expanded = sexExpanded,
            onExpandedChange = { sexExpanded = !sexExpanded }
        ) {
            OutlinedTextField(
                value = selectedSex,
                onValueChange = {},
                readOnly = true,
                label = { Text("Biological Sex") },
                leadingIcon = { Icon(Icons.Rounded.Wc, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                viewModel.sexOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.selectedSex.value = option
                            sexExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Country dropdown
        ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = !countryExpanded }
        ) {
            OutlinedTextField(
                value = selectedCountry,
                onValueChange = {},
                readOnly = true,
                label = { Text("Country") },
                leadingIcon = { Icon(Icons.Rounded.Public, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                viewModel.countryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.selectedCountry.value = option
                            countryExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Language dropdown
        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = !languageExpanded }
        ) {
            OutlinedTextField(
                value = viewModel.languageOptions[selectedLanguage] ?: "English",
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Language") },
                leadingIcon = { Icon(Icons.Rounded.Language, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                viewModel.languageOptions.forEach { (code, displayName) ->
                    DropdownMenuItem(
                        text = { Text(displayName) },
                        onClick = {
                            viewModel.selectedLanguage.value = code
                            languageExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Error message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Get Started button
        Button(
            onClick = {
                if (viewModel.validateAndSave(onboardingRepository)) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
