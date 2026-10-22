package com.nusastream.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private val Bg = Color(0xFF090B10)
private val Accent = Color(0xFF55D6BE)
private val Muted = Color(0xFF9AA4B5)
private val Panel = Color(0xFF171B24)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NusaStreamApp(this) }
    }
}

private data class Channel(val name: String, val url: String)

@Composable
fun NusaStreamApp(activity: MainActivity) {
    var page by remember { mutableStateOf("Home") }
    var streamUrl by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var magnetUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Sedia") }
    var channels by remember { mutableStateOf(listOf<Channel>()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var bufferSeconds by remember { mutableFloatStateOf(10f) }
    val scope = rememberCoroutineScope()
    val prefs = remember { activity.getSharedPreferences("nusastream", 0) }
    val player = remember {
        ExoPlayer.Builder(activity).build().apply {
            playWhenReady = false
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(Unit) {
        streamUrl = prefs.getString("last_stream", "") ?: ""
        playlistUrl = prefs.getString("playlist_url", "") ?: ""
        magnetUrl = prefs.getString("last_magnet", "") ?: ""
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.width(190.dp).fillMaxHeight().background(Color(0xFF0D1017)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("NusaStream", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                    Text("MEDIA HUB", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    listOf("Home", "Player", "M3U/IPTV", "Torrent", "Add-ons", "Settings").forEach { item ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (page == item) Color(0xFF253A40) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { page = item }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item, color = if (page == item) Accent else Color.White,
                                fontWeight = if (page == item) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Sumber sendiri / sah sahaja", color = Muted, fontSize = 11.sp)
                }

                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text(page, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    when (page) {
                        "Home" -> {
                            Text("Pusat streaming peribadi", color = Muted)
                            Spacer(Modifier.height(18.dp))
                            Text("Mula pantas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            ActionCard("Mainkan URL video", "Masukkan URL video HTTP(S) yang anda dibenarkan akses.") { page = "Player" }
                            ActionCard("Playlist M3U/M3U8", "Import playlist IPTV sendiri.") { page = "M3U/IPTV" }
                            ActionCard("Magnet torrent", "Hantar magnet link ke aplikasi torrent yang dipasang.") { page = "Torrent" }
                            Spacer(Modifier.height(12.dp))
                            Text("Status: $status", color = Accent)
                        }
                        "Player" -> {
                            Text("URL video HTTP(S)", color = Muted)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("https://.../video.m3u8 atau .mp4") },
                                singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val u = streamUrl.trim()
                                    if (u.startsWith("https://") || u.startsWith("http://")) {
                                        prefs.edit().putString("last_stream", u).apply()
                                        selectedChannel = null
                                        player.setMediaItem(MediaItem.fromUri(u))
                                        player.prepare()
                                        player.play()
                                        status = "Sedang memainkan URL"
                                    } else status = "Masukkan URL http:// atau https:// yang sah"
                                }) { Text("Mainkan") }
                                OutlinedButton(onClick = { player.pause(); status = "Dijeda" }) { Text("Pause") }
                                OutlinedButton(onClick = { player.seekBack() }) { Text("−10s") }
                                OutlinedButton(onClick = { player.seekForward() }) { Text("+10s") }
                            }
                            Spacer(Modifier.height(12.dp))
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(12.dp)),
                                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                                update = { it.player = player }
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Status: $status", color = Muted, fontSize = 12.sp)
                        }
                        "M3U/IPTV" -> {
                            Text("Masukkan URL playlist M3U/M3U8. Playlist akan dibaca dan boleh dipilih untuk dimainkan.", color = Muted)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = playlistUrl, onValueChange = { playlistUrl = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("URL playlist https://...") }, singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                val u = playlistUrl.trim()
                                if (!u.startsWith("https://") && !u.startsWith("http://")) {
                                    status = "URL playlist tidak sah"
                                } else {
                                    status = "Memuat turun playlist..."
                                    scope.launch {
                                        try {
                                            val loaded = withContext(Dispatchers.IO) { loadM3u(u) }
                                            channels = loaded
                                            prefs.edit().putString("playlist_url", u).apply()
                                            status = "Berjaya: ${loaded.size} item ditemui"
                                        } catch (e: Exception) {
                                            status = "Gagal baca playlist: ${e.message ?: "ralat rangkaian"}"
                                        }
                                    }
                                }
                            }) { Text("Import playlist") }
                            Spacer(Modifier.height(10.dp))
                            Text(status, color = Accent)
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(channels) { channel ->
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedChannel = channel
                                            streamUrl = channel.url
                                            prefs.edit().putString("last_stream", channel.url).apply()
                                            player.setMediaItem(MediaItem.fromUri(channel.url))
                                            player.prepare()
                                            player.play()
                                            page = "Player"
                                            status = "Memainkan: ${channel.name}"
                                        }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(channel.url, color = Muted, fontSize = 10.sp, maxLines = 1)
                                        }
                                        Text("▶", color = Accent)
                                    }
                                }
                            }
                        }
                        "Torrent" -> {
                            Text("Magnet link / torrent", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Masukkan magnet link untuk dibuka dengan aplikasi torrent yang serasi pada peranti. Main balik torrent terus di dalam NusaStream belum diaktifkan dalam binaan ini.", color = Muted)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = magnetUrl, onValueChange = { magnetUrl = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("magnet:?xt=urn:btih:...") })
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                val m = magnetUrl.trim()
                                if (!m.startsWith("magnet:?")) {
                                    status = "Magnet link tidak sah"
                                } else {
                                    prefs.edit().putString("last_magnet", m).apply()
                                    try {
                                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m)))
                                        status = "Magnet link dihantar ke aplikasi torrent"
                                    } catch (_: Exception) {
                                        status = "Tiada aplikasi torrent yang boleh membuka magnet link"
                                        Toast.makeText(activity, status, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) { Text("Buka magnet link") }
                            Spacer(Modifier.height(8.dp))
                            Text("Gunakan torrent yang anda miliki atau mempunyai kebenaran untuk muat turun/strim.", color = Accent)
                            Text("Nota: torrent terbina dalam memerlukan enjin P2P, pengurusan fail dan pelayan aliran setempat; itu belum diuji atau didakwa berfungsi di sini.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Status: $status", color = Muted)
                        }
                        "Add-ons" -> {
                            Text("Pengurus sumber asas", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Tambah URL playlist di halaman M3U/IPTV atau URL video di Player. Katalog/add-on Stremio belum dilaksanakan.", color = Muted)
                        }
                        else -> {
                            Text("Buffer sasaran: ${bufferSeconds.toInt()} saat", color = Color.White)
                            Slider(value = bufferSeconds, onValueChange = { bufferSeconds = it }, valueRange = 5f..30f,
                                steps = 4)
                            Text("Tetapan ini menunjukkan sasaran pengguna; ia belum mengubah load control Media3 dalam binaan ini.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                prefs.edit().putFloat("buffer_seconds", bufferSeconds).apply()
                                status = "Tetapan disimpan"
                            }) { Text("Simpan tetapan") }
                            Text(status, color = Accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Panel, RoundedCornerShape(12.dp))
        .clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Muted, fontSize = 12.sp)
        }
        Text("›", color = Accent, fontSize = 26.sp)
    }
}

private fun loadM3u(address: String): List<Channel> {
    val connection = URL(address).openConnection().apply {
        connectTimeout = 15000
        readTimeout = 20000
        setRequestProperty("User-Agent", "NusaStream/0.2")
    }
    return connection.getInputStream().bufferedReader(Charsets.UTF_8).use { reader ->
        val result = mutableListOf<Channel>()
        var nextName = "Stream"
        reader.forEachLine { line ->
            val s = line.trim()
            when {
                s.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = s.lastIndexOf(',')
                    nextName = if (comma >= 0 && comma < s.length - 1) s.substring(comma + 1).trim() else "Stream"
                }
                s.isNotEmpty() && !s.startsWith("#") && (s.startsWith("http://") || s.startsWith("https://")) -> {
                    result.add(Channel(nextName, s))
                    nextName = "Stream"
                }
            }
        }
        result
    }
}
