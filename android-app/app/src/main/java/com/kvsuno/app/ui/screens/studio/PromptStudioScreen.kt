package com.kvsuno.app.ui.screens.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kvsuno.app.data.api.DirectSunoClient
import com.kvsuno.app.data.model.GeneratedLyricsResult
import com.kvsuno.app.data.model.PromptScoreResult
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.ui.adaptive.LocalWindowState
import com.kvsuno.app.ui.components.YtmIcons
import com.kvsuno.app.ui.theme.SunoBase
import com.kvsuno.app.ui.theme.SunoElevated
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
import com.kvsuno.app.ui.theme.YtmTitle03
import kotlinx.coroutines.launch

private val wikiCategories = listOf(
    "All",
    "Vietnamese DNA",
    "Genres",
    "Instruments",
    "Vocals",
    "SFX & Mastering",
    "Metatags"
)

private val lyricGenres = listOf("Pop Ballad", "V-Pop", "Rap / Folk Trap", "Bolero", "EDM / Vinahouse", "Cinematic")
private val lyricStructures = listOf("Standard Pop", "Rap / Hip-Hop", "Cinematic")

@Composable
fun PromptStudioScreen(
    musicRepo: MusicRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowState = LocalWindowState.current
    val coroutineScope = rememberCoroutineScope()

    val studioTags by musicRepo.studioTags.collectAsState()
    val customPromptText by musicRepo.customPromptText.collectAsState()
    val generatedLyricsText by musicRepo.generatedLyricsText.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var wikiQuery by remember { mutableStateOf("") }

    var scoreResult by remember { mutableStateOf<PromptScoreResult?>(null) }
    var styleExplanation by remember { mutableStateOf<String?>(null) }
    var isScoring by remember { mutableStateOf(false) }

    var lyricTopic by remember { mutableStateOf("Đêm Mưa Sài Gòn") }
    var lyricGenre by remember { mutableStateOf(lyricGenres.first()) }
    var lyricStructure by remember { mutableStateOf(lyricStructures.first()) }
    var lyricLanguage by remember { mutableStateOf("vi") }
    var generatedLyricsInfo by remember { mutableStateOf<GeneratedLyricsResult?>(null) }
    var isGeneratingLyrics by remember { mutableStateOf(false) }

    val filteredWiki = remember(selectedCategory, wikiQuery) {
        val q = wikiQuery.trim().lowercase()
        DirectSunoClient.soundWikiCatalog.filter { item ->
            val catOk = selectedCategory == "All" || item.category == selectedCategory
            val qOk = q.isEmpty() || item.name.lowercase().contains(q) || item.promptSnippet.lowercase().contains(q)
            catOk && qOk
        }
    }

    fun copyToClipboard(label: String, content: String) {
        if (content.isBlank()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, content))
        Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
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

    val studioComposerBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Card 1: Active Style Prompt Composer
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = YtmIcons.Spark24,
                            contentDescription = null,
                            tint = SunoEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Prompt Composer", style = YtmTitle03)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = SunoEmeraldMuted) {
                        Text(
                            text = "${studioTags.size} tags",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SunoEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customPromptText,
                    onValueChange = { musicRepo.setCustomPromptText(it) },
                    label = { Text("Suno Style Prompt (tap Sound Wiki tags to append)") },
                    minLines = 3,
                    maxLines = 5,
                    colors = fieldColors,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, color = SunoTextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (studioTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StudioTagChipFlow(
                        tags = studioTags,
                        onRemoveTag = { musicRepo.removeStudioTag(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val active = customPromptText.ifBlank { studioTags.joinToString(", ") }
                            isScoring = true
                            coroutineScope.launch {
                                scoreResult = musicRepo.optimizePrompt(active, lyricLanguage)
                                styleExplanation = musicRepo.explainStyle("Studio Prompt", active, lyricLanguage).explanation
                                isScoring = false
                            }
                        },
                        enabled = !isScoring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunoEmerald,
                            contentColor = SunoPureBlack
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isScoring) {
                            CircularProgressIndicator(color = SunoPureBlack, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Score & Optimize", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val active = customPromptText.ifBlank { studioTags.joinToString(", ") }
                            copyToClipboard("Suno Style Prompt", active)
                        },
                        border = BorderStroke(1.dp, SunoWhite20),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Copy", color = SunoTextPrimary, fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (musicRepo.saveCurrentPrompt()) {
                                Toast.makeText(context, "Saved preset to Library", Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, SunoWhite20),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Save", color = SunoEmerald, fontSize = 12.5.sp)
                    }

                    if (customPromptText.isNotBlank() || studioTags.isNotEmpty()) {
                        IconButton(onClick = {
                            musicRepo.clearStudio()
                            scoreResult = null
                            styleExplanation = null
                        }) {
                            Icon(YtmIcons.CloseX24, contentDescription = "Clear", tint = SunoTextSubdued)
                        }
                    }
                }
            }

            // Card 2: 5-Pillar Optimizer Result
            val sr = scoreResult
            if (sr != null) {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("5-Pillar Acoustic Score", style = YtmTitle03)
                        Surface(shape = RoundedCornerShape(10.dp), color = SunoEmerald) {
                            Text(
                                text = "${sr.score}/100 • Grade ${sr.producerGrade}",
                                color = SunoPureBlack,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    sr.pillars.forEach { (pillar, detail) ->
                        Text(
                            text = "${pillar.replaceFirstChar { it.uppercase() }}: $detail",
                            fontSize = 12.5.sp,
                            color = SunoTextSubdued
                        )
                    }
                    if (!styleExplanation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = styleExplanation!!, fontSize = 12.sp, color = SunoTextPrimary)
                    }
                    if (sr.suggestedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to add suggested tags:", fontSize = 11.5.sp, color = SunoEmerald, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(sr.suggestedTags) { sug ->
                                Surface(
                                    onClick = { musicRepo.addStudioTag(sug) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = SunoEmeraldMuted
                                ) {
                                    Text(
                                        text = "+ $sug",
                                        fontSize = 11.5.sp,
                                        color = SunoEmerald,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { musicRepo.loadFullStyle(sr.optimizedPrompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = SunoTextPrimary, contentColor = SunoPureBlack),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Apply Optimized Prompt", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }

            // Card 3: Autonomous AI Lyrics Generator
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Lyrics Generator", style = YtmTitle03)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("vi" to "VI", "en" to "EN").forEach { (code, label) ->
                            val selected = lyricLanguage == code
                            Surface(
                                onClick = { lyricLanguage = code },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) SunoEmerald else SunoTinted
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) SunoPureBlack else SunoTextSubdued,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lyricTopic,
                    onValueChange = { lyricTopic = it },
                    label = { Text("Song Topic / Story") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(lyricGenres) { g ->
                        val selected = lyricGenre == g
                        Surface(
                            onClick = { lyricGenre = g },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) SunoEmerald else SunoTinted
                        ) {
                            Text(
                                text = g,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) SunoPureBlack else SunoTextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(lyricStructures) { s ->
                        val selected = lyricStructure == s
                        Surface(
                            onClick = { lyricStructure = s },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) SunoTextPrimary else SunoTinted
                        ) {
                            Text(
                                text = s,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) SunoPureBlack else SunoTextSubdued,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        isGeneratingLyrics = true
                        coroutineScope.launch {
                            val res = musicRepo.generateLyrics(
                                topic = lyricTopic,
                                genre = lyricGenre,
                                mood = "Emotional and Catchy",
                                vocalType = "Expressive Lead",
                                structure = lyricStructure,
                                language = lyricLanguage
                            )
                            generatedLyricsInfo = res
                            musicRepo.setGeneratedLyricsText(res.lyrics)
                            isGeneratingLyrics = false
                        }
                    },
                    enabled = !isGeneratingLyrics,
                    colors = ButtonDefaults.buttonColors(containerColor = SunoEmerald, contentColor = SunoPureBlack),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGeneratingLyrics) {
                        CircularProgressIndicator(color = SunoPureBlack, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Generate Structured Suno Lyrics", fontWeight = FontWeight.Bold)
                    }
                }

                if (generatedLyricsText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = generatedLyricsText,
                        onValueChange = { musicRepo.setGeneratedLyricsText(it) },
                        label = { Text(generatedLyricsInfo?.title ?: "Generated Lyrics") },
                        minLines = 5,
                        maxLines = 10,
                        colors = fieldColors,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SunoTextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        generatedLyricsInfo?.suggestedStylePrompt?.takeIf { it.isNotBlank() }?.let { sug ->
                            TextButton(onClick = { musicRepo.loadFullStyle(sug) }) {
                                Text("Load Style", color = SunoEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { copyToClipboard("Suno Lyrics", generatedLyricsText) }) {
                            Text("Copy Lyrics", color = SunoTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val soundWikiBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioCard {
                Text("Sound Wiki Acoustic Catalog", style = YtmTitle03)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any tag below to inject it directly into your active Suno prompt.",
                    style = YtmSubtitle01.copy(fontSize = 12.5.sp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = wikiQuery,
                    onValueChange = { wikiQuery = it },
                    label = { Text("Filter tags (e.g. Vinahouse, Rhodes, [Drop])") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(wikiCategories) { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) SunoEmerald else SunoTinted
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) SunoPureBlack else SunoTextPrimary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            filteredWiki.forEach { item ->
                Surface(
                    onClick = {
                        musicRepo.addStudioTag(item.promptSnippet)
                        Toast.makeText(context, "Added '${item.name}' to Prompt Studio", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = SunoElevated,
                    border = BorderStroke(1.dp, SunoWhite08),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SunoTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = SunoTinted) {
                                    Text(
                                        text = item.category,
                                        fontSize = 10.sp,
                                        color = SunoEmerald,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.promptSnippet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                color = SunoTextSubdued,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = SunoEmeraldMuted) {
                            Text(
                                text = "+ Add",
                                color = SunoEmerald,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SunoPureBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Prompt Studio & Wiki",
                    style = TextStyle(
                        fontFamily = YouTubeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.04).em,
                        color = SunoTextPrimary
                    )
                )
                Text(
                    text = "Zero-key 5-pillar optimizer, AI lyrics & acoustic tags",
                    style = YtmSubtitle01.copy(fontSize = 13.sp)
                )
            }
        }

        if (windowState.isLandscapeTwoPaneWatch) {
            // Wide Tablet / Foldable Landscape: 2-Pane side-by-side Studio + Sound Wiki
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item { studioComposerBlock() }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    item { soundWikiBlock() }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { studioComposerBlock() }
                item { soundWikiBlock() }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioTagChipFlow(
    tags: List<String>,
    onRemoveTag: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SunoEmeraldMuted,
                border = BorderStroke(1.dp, SunoEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.clickable { onRemoveTag(tag) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = SunoEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("×", color = SunoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StudioCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SunoWhite08, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = SunoBase
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
