package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import com.example.data.local.ActiveStoreSession
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.WarningRed
import com.example.util.AuthManager
import com.example.util.CurrencyFormatter
import com.example.util.PinSecurityUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun StoreAccountDialog(
    activeSession: ActiveStoreSession?,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    onDismiss: () -> Unit,
    onRegister: (phone: String, pin: String, storeName: String) -> Unit,
    onLogin: (phone: String, pin: String) -> Unit,
    onSyncToCloud: (pin: String) -> Unit,
    onRestoreFromCloud: (phone: String, pin: String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    // Tab state: 0 = Masuk Akun, 1 = Daftar Baru
    var selectedAuthTab by remember { mutableIntStateOf(if (activeSession == null) 0 else 0) }

    // Form inputs
    var phoneNumberInput by remember { mutableStateOf(activeSession?.phoneNumber ?: "") }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var storeNameInput by remember { mutableStateOf(activeSession?.storeName ?: "") }

    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Prompt for PIN during sync/restore dialogs
    var showPinPromptForSync by remember { mutableStateOf(false) }
    var showPinPromptForRestore by remember { mutableStateOf(false) }
    var actionPinInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("store_account_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Akun Cloud & Pindah HP",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Menu & data toko anti hilang saat ganti HP",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Firebase Cloud Auth Status Notice
                if (currentUser == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Koneksi Cloud Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Hubungkan akun Google untuk otorisasi database cloud.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    AuthManager.signInWithGoogle(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            currentUser = Firebase.auth.currentUser
                                            Toast.makeText(context, "Google Cloud Terhubung", Toast.LENGTH_SHORT).show()
                                        },
                                        onAuthError = { err ->
                                            errorMessage = err
                                        },
                                        scope = scope
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Hubungkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Error Message Banner
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Active Session State vs Unauthenticated Form
                if (activeSession != null) {
                    // Logged in Store Account View
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MoneyGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Akun Aktif & Terlindungi",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MoneyGreen
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Online Cloud",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = activeSession.storeName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Nomor HP: ${activeSession.phoneNumber}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Sinkron terakhir: ${if (activeSession.lastSyncTimestamp > 0) CurrencyFormatter.formatDateTime(activeSession.lastSyncTimestamp) else "Belum pernah"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sync action buttons
                    Button(
                        onClick = {
                            actionPinInput = ""
                            showPinPromptForSync = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_to_cloud_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Menyinkronkan...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cadangkan Menu Sekarang ke Cloud", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            actionPinInput = ""
                            showPinPromptForRestore = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_from_cloud_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSyncing
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pulihkan Menu dari Cloud (Saat Pindah HP)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_account_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Keluar / Ganti Nomor Akun", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }

                } else {
                    // Unauthenticated: Tabs [Masuk Akun HP] vs [Daftar Akun Baru]
                    TabRow(
                        selectedTabIndex = selectedAuthTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Tab(
                            selected = selectedAuthTab == 0,
                            onClick = {
                                selectedAuthTab = 0
                                errorMessage = null
                            },
                            text = { Text("Masuk Akun", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedAuthTab == 1,
                            onClick = {
                                selectedAuthTab = 1
                                errorMessage = null
                            },
                            text = { Text("Daftar Akun Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedAuthTab == 0) {
                        // Form: Masuk Akun (Pindah HP)
                        Text(
                            text = "Punya akun di HP lama? Masukkan Nomor HP & PIN untuk memulihkan seluruh menu secara instan.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { phoneNumberInput = it },
                            label = { Text("Nomor HP Toko *") },
                            placeholder = { Text("081234567890") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_phone_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                            label = { Text("PIN Keamanan (6 Digit) *") },
                            placeholder = { Text("Contoh: 123456") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                    Icon(
                                        imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_pin_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                if (!PinSecurityUtil.isValidPhoneNumber(phoneNumberInput)) {
                                    errorMessage = "Nomor HP harus berawalan 08 dan memiliki panjang 10-15 digit."
                                    return@Button
                                }
                                if (!PinSecurityUtil.isValidPin(pinInput)) {
                                    errorMessage = "PIN harus berupa 6 digit angka."
                                    return@Button
                                }

                                if (Firebase.auth.currentUser == null) {
                                    // Trigger Google Sign-In first to authorize Firestore
                                    AuthManager.signInWithGoogle(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            currentUser = Firebase.auth.currentUser
                                            onLogin(phoneNumberInput.trim(), pinInput.trim())
                                        },
                                        onAuthError = { err ->
                                            errorMessage = "Perlu koneksi Google untuk cloud sync: $err"
                                        },
                                        scope = scope
                                    )
                                } else {
                                    onLogin(phoneNumberInput.trim(), pinInput.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_login_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Memverifikasi...")
                            } else {
                                Text("Masuk & Pulihkan Menu Toko", fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        // Form: Daftar Akun Baru
                        Text(
                            text = "Daftarkan akun toko baru untuk mengamankan menu ke Cloud. Jika ganti HP di masa depan, menu tidak akan hilang.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = storeNameInput,
                            onValueChange = { storeNameInput = it },
                            label = { Text("Nama Toko / Usaha *") },
                            placeholder = { Text("Contoh: Toko Sejahtera") },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_store_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { phoneNumberInput = it },
                            label = { Text("Nomor HP Toko *") },
                            placeholder = { Text("081234567890") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_phone_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                            label = { Text("Buat PIN 6 Digit *") },
                            placeholder = { Text("6 digit angka rahasia") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                    Icon(
                                        imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_pin_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pinConfirmInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinConfirmInput = it },
                            label = { Text("Ulangi PIN 6 Digit *") },
                            placeholder = { Text("Masukkan ulang PIN") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_pin_confirm_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                if (storeNameInput.isBlank()) {
                                    errorMessage = "Nama toko tidak boleh kosong."
                                    return@Button
                                }
                                if (!PinSecurityUtil.isValidPhoneNumber(phoneNumberInput)) {
                                    errorMessage = "Nomor HP harus berawalan 08 dan memiliki panjang 10-15 digit."
                                    return@Button
                                }
                                if (!PinSecurityUtil.isValidPin(pinInput)) {
                                    errorMessage = "PIN harus berupa 6 digit angka."
                                    return@Button
                                }
                                if (pinInput != pinConfirmInput) {
                                    errorMessage = "Konfirmasi PIN tidak cocok dengan PIN pertama."
                                    return@Button
                                }

                                if (Firebase.auth.currentUser == null) {
                                    AuthManager.signInWithGoogle(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            currentUser = Firebase.auth.currentUser
                                            onRegister(phoneNumberInput.trim(), pinInput.trim(), storeNameInput.trim())
                                        },
                                        onAuthError = { err ->
                                            errorMessage = "Perlu koneksi Google untuk cloud sync: $err"
                                        },
                                        scope = scope
                                    )
                                } else {
                                    onRegister(phoneNumberInput.trim(), pinInput.trim(), storeNameInput.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_register_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mendaftarkan...")
                            } else {
                                Text("Daftar & Cadangkan Menu Toko", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal PIN confirmation for Syncing to Cloud
    if (showPinPromptForSync && activeSession != null) {
        AlertDialog(
            onDismissRequest = { showPinPromptForSync = false },
            title = { Text("Konfirmasi PIN Cadangan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Masukkan PIN 6 digit untuk mengunggah dan mencadangkan menu toko ke Cloud:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = actionPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) actionPinInput = it },
                        label = { Text("PIN 6 Digit") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sync_pin_confirm_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionPinInput.length == 6) {
                            showPinPromptForSync = false
                            onSyncToCloud(actionPinInput)
                        } else {
                            Toast.makeText(context, "PIN harus 6 digit", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Cadangkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinPromptForSync = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal PIN confirmation for Restoring from Cloud (Pindah HP)
    if (showPinPromptForRestore && activeSession != null) {
        AlertDialog(
            onDismissRequest = { showPinPromptForRestore = false },
            title = { Text("Pulihkan Menu dari Cloud?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Menu di HP ini akan digantikan dengan versi terbaru yang tersimpan di Cloud. Masukkan PIN 6 digit Anda:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = actionPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) actionPinInput = it },
                        label = { Text("PIN 6 Digit") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("restore_pin_confirm_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionPinInput.length == 6) {
                            showPinPromptForRestore = false
                            onRestoreFromCloud(activeSession.phoneNumber, actionPinInput)
                        } else {
                            Toast.makeText(context, "PIN harus 6 digit", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Pulihkan Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinPromptForRestore = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
