package com.sbro.gameslibrary.ui.screens

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    user: FirebaseUser?,
    onBack: () -> Unit,
    onOpenMyTests: () -> Unit,
    onOpenMyComments: () -> Unit,
    onOpenMyFavorites: () -> Unit,
    onOpenMyDevices: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleResult(context, res.data)
        }
    }

    val background = Brush.verticalGradient(
        listOf(cs.background, cs.surfaceContainerLow, cs.background)
    )

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by rememberSaveable { mutableStateOf("") }
    var editNameError by remember { mutableStateOf<String?>(null) }
    var editNameLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(contentWindowInsets = WindowInsets(0),
        containerColor = cs.background,
        topBar = {
            Column {
                TopAppBar(modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = stringResource(R.string.profile_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeClick(onBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = cs.outline.copy(alpha = 0.4f)
                )
            }
        }
    ) { pv ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user == null) {

                    GuestHeader()

                    Spacer(Modifier.height(14.dp))

                    GuestActionsCard(
                        onGoogleSignIn = {
                            safeClick { launcher.launch(viewModel.getGoogleSignInIntent(context)) }
                        },
                        onEmailSignIn = { safeClick(onOpenLogin) },
                        onRegister = { safeClick(onOpenRegister) }
                    )

                    Spacer(Modifier.height(14.dp))

                    GuestBenefits()

                    Spacer(Modifier.height(24.dp))

                    return@Column
                }

                UserHeaderCard(
                    displayName = user.displayName.orEmpty(),
                    email = user.email.orEmpty(),
                    photoUrl = user.photoUrl?.toString()
                )

                Spacer(Modifier.height(12.dp))

                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        OutlinedButton(
                            onClick = {
                                safeClick {
                                    editNameText = user.displayName.orEmpty()
                                    editNameError = null
                                    showEditNameDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_edit_name),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        OutlinedButton(
                            onClick = { safeClick { showLogoutDialog = true } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sign_out),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.profile_menu_section).ifBlank { "" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    color = cs.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileMenuCard(
                        icon = Icons.Filled.History,
                        title = stringResource(R.string.my_tests),
                        subtitle = stringResource(R.string.profile_menu_tests_sub),
                        onClick = { safeClick(onOpenMyTests) }
                    )

                    ProfileMenuCard(
                        icon = Icons.AutoMirrored.Filled.Message,
                        title = stringResource(R.string.my_comments),
                        subtitle = stringResource(R.string.profile_menu_comments_sub),
                        onClick = { safeClick(onOpenMyComments) }
                    )

                    ProfileMenuCard(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.my_favorites),
                        subtitle = stringResource(R.string.profile_menu_favorites_sub),
                        onClick = { safeClick(onOpenMyFavorites) }
                    )

                    ProfileMenuCard(
                        icon = Icons.Filled.PhoneAndroid,
                        title = stringResource(R.string.my_devices_title),
                        subtitle = stringResource(R.string.profile_menu_devices_sub),
                        onClick = { safeClick(onOpenMyDevices) }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.sign_out_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut(context)
                    }
                ) { Text(stringResource(R.string.sign_out_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.sign_out_confirm_no))
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!editNameLoading) showEditNameDialog = false },
            title = { Text(stringResource(R.string.profile_edit_name_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = {
                            editNameText = it
                            editNameError = null
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.auth_name_label)) },
                        placeholder = { Text(stringResource(R.string.auth_name_hint)) },
                        isError = editNameError != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editNameError != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(editNameError!!) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = cs.errorContainer,
                                labelColor = cs.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !editNameLoading,
                    onClick = {
                        safeClick {
                            val newName = editNameText.trim()
                            if (newName.isBlank()) {
                                editNameError = context.getString(R.string.auth_error_empty_name)
                                return@safeClick
                            }

                            editNameLoading = true
                            viewModel.updateDisplayName(
                                context = context,
                                newName = newName,
                                onSuccess = {
                                    editNameLoading = false
                                    showEditNameDialog = false
                                },
                                onError = {
                                    editNameLoading = false
                                    editNameError = it
                                }
                            )
                        }
                    }
                ) {
                    if (editNameLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !editNameLoading,
                    onClick = { showEditNameDialog = false }
                ) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun GuestHeader() {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(86.dp),
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.profile_guest_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.profile_guest_body),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center,
            color = cs.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@Composable
private fun GuestActionsCard(
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    onRegister: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = onGoogleSignIn,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.sign_in_google),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedButton(
                onClick = onEmailSignIn,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Filled.MailOutline,
                    contentDescription = null,
                    tint = cs.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auth_sign_in_email),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                color = cs.outlineVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_no_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = onRegister) {
                    Text(
                        text = stringResource(R.string.auth_go_register),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestBenefits() {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuestBenefitRow(
                icon = Icons.Filled.FavoriteBorder,
                text = stringResource(R.string.profile_guest_hint)
            )

            GuestBenefitDivider()

            GuestBenefitRow(
                icon = Icons.Filled.History,
                text = stringResource(R.string.profile_guest_hint_tests)
            )

            GuestBenefitDivider()

            GuestBenefitRow(
                icon = Icons.AutoMirrored.Filled.Message,
                text = stringResource(R.string.profile_guest_hint_comments)
            )
        }
    }
}

@Composable
private fun GuestBenefitRow(
    icon: ImageVector,
    text: String
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cs.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = cs.onSurface,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

@Composable
private fun GuestBenefitDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UserHeaderCard(
    displayName: String,
    email: String,
    photoUrl: String?
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(44.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(88.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_signed_in_as),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = cs.onSurfaceVariant
                )

                if (displayName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = cs.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = cs.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant
            )
        }
    }
}
