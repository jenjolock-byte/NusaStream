package com.nusastream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

private val Bg = Color(0xFF090B10)
private val Accent = Color(0xFF55D6BE)
private val Muted = Color(0xFF9AA4B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NusaStreamApp() }
    }
}

@Composable
fun NusaStreamApp() {
    var page by remember { mutableStateOf("Home") }
    var selected by remember { mutableStateOf<StreamItem?>(null) }
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Color(0xFF171B24), primary = Accent)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(190.dp).fillMaxHeight().background(Color(0xFF0D1017)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NusaStream", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
                    Text("STREAMING • TV", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    listOf("Home", "Discover", "Player", "Add-ons", "WARP VPN", "Settings").forEach { item ->
                        Row(Modifier.fillMaxWidth().background(if (page == item) Color(0xFF253A40) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { page = item }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item, color = if (page == item) Accent else Color.White, fontWeight = if (page == item) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Low-cache playback", color = Muted, fontSize = 12.sp)
                }
                Column(Modifier.fillMaxSize().padding(28.dp)) {
                    Text(page, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    when (page) {
                        "Home", "Discover" -> {
                            Text("Your streaming hub", color = Muted)
                            Spacer(Modifier.height(24.dp))
                            Text("Explore", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(demoItems) { item ->
                                    Column(Modifier.width(175.dp).height(205.dp).background(item.tint, RoundedCornerShape(18.dp)).clickable { selected = item; page = "Player" }.padding(14.dp), verticalArrangement = Arrangement.Bottom) {
                                        Text(item.icon, fontSize = 34.sp, color = Color.White)
                                        Spacer(Modifier.height(8.dp))
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                                        Text(item.subtitle, color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text("Starter build", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text("Connect a video URL you own or are authorized to use. Demo cards are placeholders, not streaming sources.", color = Muted)
                        }
                        "Player" -> {
                            Text(selected?.title ?: "Online video player", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(10.dp))
                            Text("Media3 ExoPlayer dependencies are included; the playback screen still needs a real authorized stream URL wired in.", color = Muted)
                            Spacer(Modifier.height(16.dp))
                            Text("Persistent media cache is not configured. Streaming still uses RAM and temporary system buffers.", color = Accent)
                        }
                        "Add-ons" -> {
                            Text("Add-on manager placeholder", color = Color.White)
                            Text("Only add sources you own or are authorized to use.", color = Muted)
                        }
                        "WARP VPN" -> {
                            Text("Cloudflare WARP is not connected in this starter.", color = Color(0xFFFFC56B))
                            Spacer(Modifier.height(8.dp))
                            Text("This screen does not create a VPN tunnel. A real integration requires a supported method and device testing.", color = Muted)
                        }
                        else -> {
                            Text("Buffer target: 5–15 seconds (configuration to be wired into player)", color = Color.White)
                            Text("Persistent stream cache: not configured", color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text("Actual storage use depends on Android, player buffering and stream source.", color = Muted)
                        }
                    }
                }
            }
        }
    }
}

private data class StreamItem(val title: String, val subtitle: String, val icon: String, val tint: Color)
private val demoItems = listOf(
    StreamItem("Live TV", "Authorized source", "▶", Color(0xFF284E55)),
    StreamItem("Movies", "Personal library", "▣", Color(0xFF493753)),
    StreamItem("Series", "Continue watching", "◉", Color(0xFF4A3E2D)),
    StreamItem("Discover", "Browse sources", "⌕", Color(0xFF263D5A))
)
