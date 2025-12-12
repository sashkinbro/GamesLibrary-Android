package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.ProfileViewModel

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

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

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by rememberSaveable { mutableStateOf("") }
    var editNameError by remember { mutableStateOf<String?>(null) }
    var editNameLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val bgBrush = Brush.verticalGradient(listOf(CyberBlack, CyberDark, CyberBlack))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column {
                    TopAppBar(modifier = Modifier.statusBarsPadding(),
                        title = {
                            GlitchText(
                                text = stringResource(R.string.profile_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
                                    fontSize = 22.sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { safeClick(onBack) },
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                                    .background(CyberDark)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = CyberRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CyberYellow
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberYellow.copy(alpha = 0.12f)
                    )
                }
            }
        ) { pv ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
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
                        GuestHeaderCyber()

                        Spacer(Modifier.height(14.dp))

                        GuestActionsCardCyber(
                            onGoogleSignIn = {
                                safeClick { launcher.launch(viewModel.getGoogleSignInIntent(context)) }
                            },
                            onEmailSignIn = { safeClick(onOpenLogin) },
                            onRegister = { safeClick(onOpenRegister) }
                        )

                        Spacer(Modifier.height(14.dp))

                        GuestBenefitsCyber()

                        Spacer(Modifier.height(24.dp))
                        Spacer(
                            modifier = Modifier.windowInsetsBottomHeight(
                                WindowInsets.navigationBars
                            )
                        )
                        return@Column
                    }

                    UserHeaderCardCyber(
                        displayName = user.displayName.orEmpty(),
                        email = user.email.orEmpty(),
                        photoUrl = user.photoUrl?.toString()
                    )

                    Spacer(Modifier.height(12.dp))

                    val panelShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
                    Surface(
                        shape = panelShape,
                        color = CyberDark,
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    CyberRed.copy(alpha = 0.7f),
                                    CyberYellow.copy(alpha = 0.7f),
                                    CyberBlue.copy(alpha = 0.7f)
                                )
                            )
                        ),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            CyberOutlinedButton(
                                onClick = {
                                    safeClick {
                                        editNameText = user.displayName.orEmpty()
                                        editNameError = null
                                        showEditNameDialog = true
                                    }
                                },
                                icon = Icons.Filled.Edit,
                                text = stringResource(R.string.profile_edit_name)
                            )

                            CyberOutlinedButton(
                                onClick = { safeClick { showLogoutDialog = true } },
                                icon = Icons.AutoMirrored.Filled.Logout,
                                text = stringResource(R.string.sign_out),
                                tint = CyberRed
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.profile_menu_section).ifBlank { "" }.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        color = CyberYellow
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileMenuCardCyber(
                            icon = Icons.Filled.History,
                            title = stringResource(R.string.my_tests),
                            subtitle = stringResource(R.string.profile_menu_tests_sub),
                            onClick = { safeClick(onOpenMyTests) }
                        )

                        ProfileMenuCardCyber(
                            icon = Icons.AutoMirrored.Filled.Message,
                            title = stringResource(R.string.my_comments),
                            subtitle = stringResource(R.string.profile_menu_comments_sub),
                            onClick = { safeClick(onOpenMyComments) }
                        )

                        ProfileMenuCardCyber(
                            icon = Icons.Filled.Favorite,
                            title = stringResource(R.string.my_favorites),
                            subtitle = stringResource(R.string.profile_menu_favorites_sub),
                            onClick = { safeClick(onOpenMyFavorites) }
                        )

                        ProfileMenuCardCyber(
                            icon = Icons.Filled.PhoneAndroid,
                            title = stringResource(R.string.my_devices_title),
                            subtitle = stringResource(R.string.profile_menu_devices_sub),
                            onClick = { safeClick(onOpenMyDevices) }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Spacer(
                        modifier = Modifier.windowInsetsBottomHeight(
                            WindowInsets.navigationBars
                        )
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        CyberAlertDialog(
            onDismiss = { showLogoutDialog = false },
            title = stringResource(R.string.sign_out_confirm_title),
            body = stringResource(R.string.sign_out_confirm_body),
            confirmText = stringResource(R.string.sign_out_confirm_yes),
            dismissText = stringResource(R.string.sign_out_confirm_no),
            onConfirm = {
                showLogoutDialog = false
                viewModel.signOut(context)
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!editNameLoading) showEditNameDialog = false },
            containerColor = CyberDark,
            tonalElevation = 0.dp,
            shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
            title = {
                Text(
                    stringResource(R.string.profile_edit_name_title).uppercase(),
                    color = CyberYellow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = {
                            editNameText = it
                            editNameError = null
                        },
                        singleLine = true,
                        label = {
                            Text(
                                stringResource(R.string.auth_name_label),
                                color = CyberBlue.copy(alpha = 0.9f),
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.auth_name_hint),
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        isError = editNameError != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberYellow,
                            unfocusedBorderColor = CyberYellow.copy(alpha = 0.35f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CyberYellow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editNameError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberBlack,
                            border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = editNameError!!,
                                color = CyberRed,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
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
                            strokeWidth = 2.dp,
                            color = CyberYellow
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        stringResource(R.string.common_save).uppercase(),
                        color = CyberYellow,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !editNameLoading,
                    onClick = { showEditNameDialog = false }
                ) {
                    Text(
                        stringResource(R.string.common_cancel).uppercase(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        )
    }
}


@Composable
private fun GuestHeaderCyber() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
            color = CyberDark,
            border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.5f)),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = CyberYellow,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        GlitchText(
            text = stringResource(R.string.profile_guest_title).uppercase(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.profile_guest_body),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace
            ),
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@Composable
private fun GuestActionsCardCyber(
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    onRegister: () -> Unit
) {
    val shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)

    Surface(
        shape = shape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
        ),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CyberPrimaryButton(
                onClick = onGoogleSignIn,
                leading = {
                    Image(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = stringResource(R.string.sign_in_google)
            )

            CyberOutlinedButton(
                onClick = onEmailSignIn,
                icon = Icons.Filled.MailOutline,
                text = stringResource(R.string.auth_sign_in_email),
                tint = CyberBlue
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                color = CyberYellow.copy(alpha = 0.18f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.auth_no_account),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = onRegister) {
                    Text(
                        text = stringResource(R.string.auth_go_register).uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = CyberYellow
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestBenefitsCyber() {
    val shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)

    Surface(
        shape = shape,
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.25f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GuestBenefitRowCyber(
                icon = Icons.Filled.FavoriteBorder,
                text = stringResource(R.string.profile_guest_hint),
                tint = CyberRed
            )
            GuestBenefitDividerCyber()
            GuestBenefitRowCyber(
                icon = Icons.Filled.History,
                text = stringResource(R.string.profile_guest_hint_tests),
                tint = CyberBlue
            )
            GuestBenefitDividerCyber()
            GuestBenefitRowCyber(
                icon = Icons.AutoMirrored.Filled.Message,
                text = stringResource(R.string.profile_guest_hint_comments),
                tint = CyberYellow
            )
        }
    }
}

@Composable
private fun GuestBenefitRowCyber(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(10.dp),
            color = CyberBlack,
            border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = FontFamily.Monospace,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

@Composable
private fun GuestBenefitDividerCyber() {
    HorizontalDivider(
        color = CyberYellow.copy(alpha = 0.12f),
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
private fun UserHeaderCardCyber(
    displayName: String,
    email: String,
    photoUrl: String?
) {
    val shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)

    Surface(
        shape = shape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberBlue, CyberYellow))
        ),
        tonalElevation = 0.dp,
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
                        .background(CyberGray)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = CyberYellow,
                    modifier = Modifier.size(88.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_signed_in_as).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = CyberBlue.copy(alpha = 0.85f)
                )

                if (displayName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.Black,
                        color = CyberYellow
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}


@Composable
private fun ProfileMenuCardCyber(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)

    val accent = when (icon) {
        Icons.Filled.History -> CyberRed
        Icons.AutoMirrored.Filled.Message -> CyberBlue
        Icons.Filled.Favorite -> CyberYellow
        Icons.Filled.PhoneAndroid -> CyberBlue
        else -> CyberYellow
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.25f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, accent, CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = CyberYellow
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = CyberYellow.copy(alpha = 0.7f)
            )
        }
    }
}


@Composable
private fun CyberPrimaryButton(
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    text: String
) {
    val shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)

    Surface(
        shape = shape,
        color = CyberBlack,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberYellow, CyberBlue))
        ),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(CyberDark, CyberGray, CyberDark)
                    )
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = CyberYellow
            )
        }
    }
}

@Composable
private fun CyberOutlinedButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    tint: Color = CyberYellow
) {
    val shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)

    Surface(
        shape = shape,
        color = CyberBlack,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.7f)),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(8.dp))
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = tint
            )
        }
    }
}

@Composable
private fun CyberAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    body: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDark,
        tonalElevation = 0.dp,
        shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
        title = {
            Text(
                title.uppercase(),
                color = CyberYellow,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        },
        text = {
            Text(
                body,
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText.uppercase(),
                    color = CyberYellow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    dismissText.uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    )
}
