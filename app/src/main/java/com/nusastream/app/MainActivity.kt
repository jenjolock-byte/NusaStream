package com.nusastream.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

private data class MediaCard(val name: String, val type: String, val addonName: String, val poster: String = "", val id: String = "")
private data class Addon(val name: String, val manifest: String, val types: List<String> = emptyList())

@Composable
fun NusaStreamApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("nusastream", 0) }
    var page by remember { mutableStateOf("Home") }
    var streamUrl by remember { mutableStateOf("") }
    var magnetUrl by remember { mutableStateOf("") }
    var addonInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Sedia") }
    var playerChoice by remember { mutableStateOf(prefs.getString("player_choice", "Built-in") ?: "Built-in") }
    var addons by remember { mutableStateOf(listOf<Addon>()) }
    var catalog by remember { mutableStateOf(listOf<MediaCard>()) }
    var selectedMedia by remember { mutableStateOf<MediaCard?>(null) }
    var streams by remember { mutableStateOf(listOf<Pair<String,String>>()) }
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(activity).build() }
    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(Unit) {
        streamUrl = prefs.getString("last_stream", "") ?: ""
        magnetUrl = prefs.getString("last_magnet", "") ?: ""
        playerChoice = prefs.getString("player_choice", "Built-in") ?: "Built-in"
        val saved = prefs.getStringSet("addons", emptySet()) ?: emptySet()
        addons = saved.mapNotNull { raw -> runCatching {
            val j = JSONObject(raw); Addon(j.optString("name"), j.optString("manifest"), j.optJSONArray("types")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList())
        }.getOrNull() }
        if (addons.isNotEmpty()) loadCatalogs(addons, { catalog = it }, { status = it })
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(190.dp).fillMaxHeight().background(Color(0xFF0D1017)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("NusaStream", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                    Text("MOVIES • SERIES • LIVE", color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.height(12.dp))
                    listOf("Home", "Movies / Series", "Player", "Torrent", "Add-ons", "Settings").forEach { item ->
                        Row(Modifier.fillMaxWidth().background(if (page == item) Color(0xFF253A40) else Color.Transparent, RoundedCornerShape(9.dp)).clickable { page = item }.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item, color = if (page == item) Accent else Color.White, fontWeight = if (page == item) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Sumber yang sah sahaja", color = Muted, fontSize = 10.sp)
                }
                Column(Modifier.fillMaxSize().padding(22.dp)) {
                    Text(page, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    when (page) {
                        "Home" -> {
                            Text("Pusat streaming peribadi", color = Muted)
                            Spacer(Modifier.height(16.dp))
                            ActionCard("Movie & Series", "Lihat katalog daripada add-on yang dipasang.") { page = "Movies / Series" }
                            ActionCard("Tambah add-on Stremio", "Masukkan URL manifest untuk membaca katalog.") { page = "Add-ons" }
                            ActionCard("Pemain & sambungan", "Pilih player, buka WARP atau jalankan Speedtest.") { page = "Settings" }
                            Spacer(Modifier.height(14.dp))
                            Text("Status: $status", color = Accent)
                            if (catalog.isEmpty()) Text("Pasang add-on yang serasi untuk mengisi katalog filem/siri.", color = Muted)
                        }
                        "Movies / Series" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scope.launch { loadCatalogs(addons, { catalog = it }, { status = it }) } }) { Text("Refresh katalog") }
                                OutlinedButton(onClick = { catalog = catalog.filter { it.type.equals("series", true) } }) { Text("Siri") }
                                OutlinedButton(onClick = { catalog = catalog.filter { it.type.equals("movie", true) } }) { Text("Filem") }
                                OutlinedButton(onClick = { scope.launch { loadCatalogs(addons, { catalog = it }, { status = it }) } }) { Text("Semua") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(status, color = Muted, fontSize = 12.sp)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(catalog) { item ->
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).clickable {
                                        selectedMedia = item
                                        scope.launch {
                                            status = "Mencari sumber untuk ${item.name}..."
                                            streams = findStreams(addons, item)
                                            status = if (streams.isEmpty()) "Tiada stream ditemui daripada add-on ini" else "Pilih sumber untuk dimainkan"
                                        }
                                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text("${item.type} • ${item.addonName}", color = Muted, fontSize = 11.sp)
                                        }
                                        Text("Lihat sumber ›", color = Accent, fontSize = 12.sp)
                                    }
                                }
                            }
                            selectedMedia?.let { item ->
                                Spacer(Modifier.height(8.dp))
                                Text("Sumber: ${item.name}", color = Accent, fontWeight = FontWeight.Bold)
                                streams.forEach { (name, url) ->
                                    Text("▶  $name", color = Color.White, modifier = Modifier.clickable {
                                        streamUrl = url; prefs.edit().putString("last_stream", url).apply()
                                        playUrl(activity, player, playerChoice, url) { status = it }; page = "Player"
                                    }.padding(6.dp))
                                }
                            }
                        }
                        "Player" -> {
                            OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("URL video HTTP(S) / HLS .m3u8") }, singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { prefs.edit().putString("last_stream", streamUrl.trim()).apply(); playUrl(activity, player, playerChoice, streamUrl.trim()) { status = it } }) { Text("Mainkan") }
                                OutlinedButton(onClick = { player.pause(); status = "Dijeda" }) { Text("Pause") }
                                OutlinedButton(onClick = { player.seekBack() }) { Text("−10s") }
                                OutlinedButton(onClick = { player.seekForward() }) { Text("+10s") }
                            }
                            Spacer(Modifier.height(10.dp))
                            AndroidView(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(10.dp)), factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } }, update = { view -> view.player = player })
                            Text(status, color = Muted, fontSize = 12.sp)
                        }
                        "Torrent" -> {
                            Text("Magnet link", color = Color.White)
                            OutlinedTextField(value = magnetUrl, onValueChange = { magnetUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("magnet:?xt=urn:btih:...") })
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                if (!magnetUrl.trim().startsWith("magnet:?")) status = "Magnet link tidak sah"
                                else try {
                                    prefs.edit().putString("last_magnet", magnetUrl.trim()).apply()
                                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(magnetUrl.trim())))
                                    status = "Dihantar ke aplikasi torrent luaran"
                                } catch (_: Exception) { status = "Pasang aplikasi torrent yang serasi dahulu" }
                            }) { Text("Buka dengan aplikasi torrent") }
                            Text("Torrent terbina dalam belum disepadukan; magnet dibuka menggunakan aplikasi luaran.", color = Muted)
                            Text(status, color = Accent)
                        }
                        "Add-ons" -> {
                            Text("Add-on Stremio yang serasi (URL manifest)", color = Muted)
                            OutlinedTextField(value = addonInput, onValueChange = { addonInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("https://domain/manifest.json") }, singleLine = true)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                val url = addonInput.trim()
                                if (!(url.startsWith("https://") || url.startsWith("http://"))) status = "URL manifest tidak sah"
                                else scope.launch {
                                    status = "Memeriksa manifest..."
                                    try {
                                        val a = withContext(Dispatchers.IO) { readAddon(url) }
                                        addons = (addons.filterNot { it.manifest == a.manifest } + a)
                                        prefs.edit().putStringSet("addons", addons.map { addonJson(it) }.toSet()).apply()
                                        addonInput = ""
                                        loadCatalogs(addons, { catalog = it }, { status = it })
                                    } catch (e: Exception) { status = "Gagal baca manifest: ${e.message ?: "ralat"}" }
                                }
                            }) { Text("Tambah & muat katalog") }
                            Spacer(Modifier.height(8.dp))
                            Text(status, color = Accent, fontSize = 12.sp)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(addons) { addon ->
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(addon.name, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(addon.manifest, color = Muted, fontSize = 10.sp)
                                        }
                                        TextButton(onClick = {
                                            addons = addons.filterNot { it.manifest == addon.manifest }
                                            prefs.edit().putStringSet("addons", addons.map { addonJson(it) }.toSet()).apply()
                                            scope.launch { loadCatalogs(addons, { catalog = it }, { status = it }) }
                                        }) { Text("Buang") }
                                    }
                                }
                            }
                            Text("Hanya add-on yang anda percayai. Add-on yang memerlukan API khusus atau tidak mematuhi format Stremio mungkin tidak berfungsi.", color = Muted, fontSize = 11.sp)
                        }
                        else -> {
                            Text("Pemain", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(selected = playerChoice == "Built-in", onClick = { playerChoice = "Built-in"; prefs.edit().putString("player_choice", playerChoice).apply() }, label = { Text("Built-in") })
                                FilterChip(selected = playerChoice == "External", onClick = { playerChoice = "External"; prefs.edit().putString("player_choice", playerChoice).apply() }, label = { Text("External") })
                            }
                            Text("Pilihan semasa: $playerChoice", color = Accent)
                            Spacer(Modifier.height(14.dp))
                            Text("WARP VPN", color = Color.White, fontWeight = FontWeight.Bold)
                            Button(onClick = {
                                val intent = activity.packageManager.getLaunchIntentForPackage("com.cloudflare.onedotonedotonedotone")
                                if (intent != null) activity.startActivity(intent)
                                else {
                                    status = "Aplikasi 1.1.1.1/WARP tidak ditemui. Pasang aplikasi rasmi dahulu."
                                    try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.cloudflare.onedotonedotonedotone"))) } catch (_: Exception) {}
                                }
                            }) { Text("Buka / Run WARP") }
                            Text("NusaStream hanya membuka aplikasi WARP; ia tidak mengawal atau mengesahkan status tunnel.", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.height(14.dp))
                            Text("Speed test", color = Color.White, fontWeight = FontWeight.Bold)
                            Button(onClick = {
                                try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.speedtest.net/"))) }
                                catch (_: Exception) { status = "Tidak dapat membuka Speedtest" }
                            }) { Text("Jalankan Ookla Speedtest") }
                            Text("Membuka Speedtest rasmi dalam browser/aplikasi yang tersedia. Ujian Ookla tidak berjalan tertanam dalam NusaStream.", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.height(14.dp))
                            Text("Status: $status", color = Accent)
                        }
                    }
                }
            }
        }
    }
}

private fun playUrl(activity: MainActivity, player: ExoPlayer, choice: String, url: String, report: (String) -> Unit) {
    if (!(url.startsWith("https://") || url.startsWith("http://"))) { report("Masukkan URL HTTP(S) yang sah"); return }
    if (choice == "External") {
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/*")
        try { activity.startActivity(i); report("Dibuka dengan pemain luaran") }
        catch (_: ActivityNotFoundException) { report("Tiada pemain luaran dipasang") }
    } else {
        player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play()
        report("Sedang memainkan stream")
    }
}

private fun readAddon(url: String): Addon {
    val conn = URL(url).openConnection().apply { connectTimeout = 12000; readTimeout = 18000; setRequestProperty("User-Agent", "NusaStream/0.3") }
    val j = JSONObject(conn.getInputStream().bufferedReader().use { it.readText() })
    val name = j.optJSONObject("catalogs")?.optString("name") ?: j.optString("name", URL(url).host)
    val types = mutableListOf<String>()
    val cats = j.optJSONArray("catalogs")
    if (cats != null) for (i in 0 until cats.length()) types.add(cats.optJSONObject(i)?.optString("type") ?: "")
    if (types.isEmpty()) { types.add("movie"); types.add("series") }
    return Addon(name.ifBlank { URL(url).host }, url, types.filter { it.isNotBlank() }.distinct())
}

private fun addonJson(a: Addon): String = JSONObject().put("name", a.name).put("manifest", a.manifest).put("types", org.json.JSONArray(a.types)).toString()

private suspend fun loadCatalogs(addons: List<Addon>, onResult: (List<MediaCard>) -> Unit, onStatus: (String) -> Unit) {
    try {
        val items = withContext(Dispatchers.IO) {
            val all = mutableListOf<MediaCard>()
            addons.forEach { addon ->
                runCatching {
                    val base = addon.manifest.substringBeforeLast("/")
                    val manifest = JSONObject(URL(addon.manifest).openConnection().apply { connectTimeout=10000; readTimeout=15000 }.getInputStream().bufferedReader().use { it.readText() })
                    val cats = manifest.optJSONArray("catalogs") ?: org.json.JSONArray()
                    for (i in 0 until cats.length()) {
                        val c = cats.optJSONObject(i) ?: continue
                        val type = c.optString("type", "movie")
                        val id = c.optString("id")
                        if (id.isBlank()) continue
                        val metas = JSONObject(URL("$base/catalog/$type/$id.json").openConnection().apply { connectTimeout=10000; readTimeout=15000 }.getInputStream().bufferedReader().use { it.readText() }).optJSONArray("metas") ?: continue
                        for (m in 0 until metas.length()) {
                            val meta = metas.optJSONObject(m) ?: continue
                            all.add(MediaCard(meta.optString("name", "Untitled"), meta.optString("type", type), addon.name, meta.optString("poster"), meta.optString("id")))
                        }
                    }
                }
            }
            all.distinctBy { "${it.addonName}:${it.id}" }.take(300)
        }
        onResult(items)
        onStatus("Katalog dimuat: ${items.size} item")
    } catch (e: Exception) { onStatus("Katalog gagal dimuat: ${e.message ?: "ralat"}") }
}

private suspend fun findStreams(addons: List<Addon>, item: MediaCard): List<Pair<String,String>> = withContext(Dispatchers.IO) {
    val result = mutableListOf<Pair<String,String>>()
    addons.forEach { addon ->
        runCatching {
            val base = addon.manifest.substringBeforeLast("/")
            val j = JSONObject(URL("$base/stream/${item.type}/${Uri.encode(item.id)}.json").openConnection().apply { connectTimeout=8000; readTimeout=10000 }.getInputStream().bufferedReader().use { it.readText() })
            val arr = j.optJSONArray("streams") ?: return@runCatching
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val url = s.optString("url")
                if (url.startsWith("https://") || url.startsWith("http://")) result.add((s.optString("title").ifBlank { addon.name }) to url)
            }
        }
    }
    result.distinctBy { it.second }.take(30)
}

@Composable
private fun ActionCard(title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Panel, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Muted, fontSize = 12.sp)
        }
        Text("›", color = Accent, fontSize = 24.sp)
    }
}
