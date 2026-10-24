package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions 
import dev.rakara.app.data.UserPreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateScreen(prefsRepo: UserPreferencesRepository, onSuccess: () -> Unit) {
    var keyInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Akses Terkunci 🔒", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Masukkan Unique Key untuk membuka akses.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = keyInput,
            onValueChange = { 
                keyInput = it.uppercase()
                errorMsg = null 
            },
            label = { Text("Akses Key") },
            placeholder = { Text("XXXX-XXXX-XXXX") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = errorMsg != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        
        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (keyInput.replace("-", "").length < 8) {
                    errorMsg = "Key minimal 8 karakter."
                } else {
                    scope.launch {
                        prefsRepo.saveUserKey(keyInput)
                        onSuccess()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Buka Perpustakaan", fontWeight = FontWeight.Bold)
        }
    }
}