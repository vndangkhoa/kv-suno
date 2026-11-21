package com.kvsuno.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kvsuno.app.data.api.RetrofitClient
import com.kvsuno.app.data.repository.AuthRepository
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.data.update.AppUpdateInfo
import com.kvsuno.app.data.update.UpdateChecker
import com.kvsuno.app.player.AudioCacheManager
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoCoralRed
import com.kvsuno.app.ui.theme.SunoEmerald
import com.kvsuno.app.ui.theme.SunoEmeraldMuted
import com.kvsuno.app.ui.theme.SunoPureBlack
import com.kvsuno.app.ui.theme.SunoTextPrimary
import com.kvsuno.app.ui.theme.SunoTextSubdued
import com.kvsuno.app.ui.theme.SunoTinted
import com.kvsuno.app.ui.theme.SunoWhite08
import com.kvsuno.app.ui.theme.SunoWhite10
import com.kvsuno.app.ui.theme.SunoWhite20
import com.kvsuno.app.ui.theme.YouTubeSansFontFamily
import com.kvsuno.app.ui.theme.YtmSubtitle01
import com.kvsuno.app.ui.theme.YtmTitle02
import com.kvsuno.app.ui.theme.YtmTitle03
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val tag: String, val checkedAt: String) : UpdateUiState
    data class Available(val info: AppUpdateInfo, val checkedAt: String) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

private fun nowStamp(): String =
    SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date())

@Composable
fun SettingsScreen(
    authRepo: AuthRepository,
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(RetrofitClient.getBaseUrl()) }
    var saveSuccess by remember { mutableStateOf(false) }

    var directFailoverEnabled by remember { mutableStateOf(RetrofitClient.isDirectFailoverEnabled()) }
    var proxyInput by remember { mutableStateOf(RetrofitClient.getProxyUrl()) }
    var proxySaved by remember { mutableStateOf(false) }
    var crawlerStatusMsg by remember { mutableStateOf<String?>(null) }
    var isCheckingServer by remember { mutableStateOf(false) }

    val currentUser by authRepo.currentUser.collectAsState()
    var profileNameInput by remember { mutableStateOf(currentUser?.name.orEmpty()) }
    var profileHandleInput by remember { mutableStateOf(currentUser?.id.orEmpty()) }

    val installedVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.0.3"
        } catch (_: Exception) {
            "2.0.3"
        }
    }
    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    var notesExpanded by remember { mutableStateOf(false) }

    fun checkForUpdates() {
        coroutineScope.launch {
            updateState = UpdateUiState.Checking
            notesExpanded = false
            val latest = UpdateChecker.fetchLatest()
            updateState = when {
                latest == null || latest.tag.isBlank() ->
                    UpdateUiState.Error("Couldn't reach GitHub/Forgejo release feeds. Try again later.")
                UpdateChecker.isNewer(latest.tag, installedVersion) ->
                    UpdateUiState.Available(latest, nowStamp())
                else ->
                    UpdateUiState.UpToDate(latest.tag, nowStamp())
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SunoEmerald,
        unfocusedBorderColor = SunoWhite20,
        focusedContainerColor = SunoTinted,
        unfocusedContainerColor = SunoTinted,
        focusedTextColor = SunoTextPrimary,
        unfocusedTextColor = SunoTextPrimary,
        focusedLabelColor = SunoEmerald,
        unfocusedLabelColor = SunoTextSubdued,
        cursorColor = SunoEmerald
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunoPureBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Settings",
                style = TextStyle(
                    fontFamily = YouTubeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.04).em,
                    color = SunoTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "KV Suno host, direct CDN failover, creator profile & updates",
                style = YtmSubtitle01
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Server URL Card
            SunoSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = "Server", tint = SunoEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("KV Suno Server Host", style = YtmTitle03)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect to your self-hosted KV Suno Docker / Synology NAS instance",
                    style = YtmSubtitle01
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; saveSuccess = false },
                    label = { Text("Server URL (default https://suno.khoavo.myds.me)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            isCheckingServer = true
                            coroutineScope.launch {
                                val st = musicRepo.getCrawlerStatus()
                                crawlerStatusMsg = if (st != null) {
                                    "Connected: ${st.totalCreators} creators, ${st.totalIndexedSongs} tracks (${st.vietnameseHotHitsCount} VN hits)"
                                } else {
                                    "Server unreachable — Direct Suno API failover active"
                                }
                                isCheckingServer = false
                            }
                        },
                        enabled = !isCheckingServer,
                        border = BorderStroke(1.dp, SunoWhite20),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isCheckingServer) "Checking..." else "Test Server",
                            color = SunoTextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = {
                            RetrofitClient.setBaseUrl(serverUrl)
                            saveSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SunoEmerald, contentColor = SunoPureBlack),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (saveSuccess) {
                            Icon(YtmIcons.Check24, contentDescription = null, tint = SunoPureBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saved", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        } else {
                            Text("Save Host", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
                if (crawlerStatusMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = crawlerStatusMsg!!,
                        color = if (crawlerStatusMsg!!.startsWith("Connected")) SunoEmerald else SunoTextSubdued,
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Direct Suno API & CDN Failover
            SunoSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnLock, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Direct Suno CDN Failover", style = YtmTitle03, modifier = Modifier.weight(1f))
                    Surface(
                        color = if (directFailoverEnabled) SunoEmeraldMuted else SunoTinted,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (directFailoverEnabled) "AUTO FAILOVER" else "PROXY ONLY",
                            color = if (directFailoverEnabled) SunoEmerald else SunoTextSubdued,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Fallback to studio-api.prod.suno.com & CDN", style = YtmTitle02)
                        Text(
                            text = "Uses your NAS FFmpeg AAC cache first and seamlessly fails over to cdn1.suno.ai if needed",
                            style = YtmSubtitle01.copy(fontSize = 12.sp)
                        )
                    }
                    Switch(
                        checked = directFailoverEnabled,
                        onCheckedChange = {
                            directFailoverEnabled = it
                            RetrofitClient.setDirectFailoverEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SunoPureBlack,
                            checkedTrackColor = SunoEmerald,
                            uncheckedThumbColor = SunoTextSubdued,
                            uncheckedTrackColor = SunoTinted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = proxyInput,
                    onValueChange = { proxyInput = it; proxySaved = false },
                    label = { Text("Optional HTTP/SOCKS Proxy (blank = direct)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        RetrofitClient.setProxyUrl(proxyInput)
                        proxyInput = RetrofitClient.getProxyUrl()
                        proxySaved = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SunoTextPrimary, contentColor = SunoPureBlack),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (proxySaved) "Proxy Saved" else "Save Proxy", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Creator Studio Profile Card
            SunoSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(YtmIcons.PersonAudio24, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Studio Creator Profile", style = YtmTitle03)
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (currentUser != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SunoEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SunoPureBlack
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentUser?.name.orEmpty(), style = YtmTitle02)
                            Text(currentUser?.email.orEmpty(), style = YtmSubtitle01, color = SunoEmerald)
                        }
                        Button(
                            onClick = { authRepo.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = SunoCoralRed, contentColor = SunoTextPrimary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = profileHandleInput,
                        onValueChange = { profileHandleInput = it },
                        label = { Text("Suno @handle (optional)") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { authRepo.saveCreatorProfile(profileNameInput, profileHandleInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = SunoEmerald, contentColor = SunoPureBlack),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Audio Cache Card
            SunoSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Audio Stream Cache", style = YtmTitle03)
                            Text("Clear temporary ExoPlayer LRU audio buffer", style = YtmSubtitle01.copy(fontSize = 12.sp))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            AudioCacheManager.clearCache(context)
                            Toast.makeText(context, "Audio cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, SunoWhite20),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Clear", color = SunoTextPrimary, maxLines = 1, softWrap = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. App Updates Card
            SunoSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("App Updates", style = YtmTitle03, modifier = Modifier.weight(1f))
                    Surface(
                        color = if (updateState is UpdateUiState.Available) SunoEmerald else SunoTinted,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (updateState is UpdateUiState.Available) "NEW" else "v$installedVersion",
                            color = if (updateState is UpdateUiState.Available) SunoPureBlack else SunoTextSubdued,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val available = updateState as? UpdateUiState.Available
                val statusText = when (val s = updateState) {
                    is UpdateUiState.Idle -> "Check for new KV Suno Android releases on GitHub & Forgejo"
                    is UpdateUiState.Checking -> "Checking release feeds..."
                    is UpdateUiState.UpToDate -> "You're up to date (${s.tag}) · checked ${s.checkedAt}"
                    is UpdateUiState.Available -> "${s.info.name} is available · checked ${s.checkedAt}"
                    is UpdateUiState.Error -> s.message
                }
                Text(
                    text = statusText,
                    style = YtmSubtitle01.copy(fontSize = 13.sp),
                    color = if (updateState is UpdateUiState.Error) SunoCoralRed else SunoTextSubdued
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { checkForUpdates() },
                        enabled = updateState !is UpdateUiState.Checking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunoTextPrimary,
                            contentColor = SunoPureBlack
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (updateState is UpdateUiState.Checking) {
                            CircularProgressIndicator(color = SunoPureBlack, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking...", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = SunoPureBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for updates", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (available != null) {
                        OutlinedButton(
                            onClick = {
                                val url = available.info.apkUrl ?: available.info.pageUrl
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            border = BorderStroke(1.dp, SunoEmerald),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(YtmIcons.Download24, contentDescription = null, tint = SunoEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download APK", color = SunoEmerald, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (available != null && available.info.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { notesExpanded = !notesExpanded }) {
                        Text(
                            text = if (notesExpanded) "Hide release notes" else "See what's new",
                            color = SunoEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (notesExpanded) {
                        Surface(
                            color = SunoTinted,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SunoWhite08),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = available.info.notes,
                                color = SunoTextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SunoSettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SunoWhite08, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = SunoBase
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
