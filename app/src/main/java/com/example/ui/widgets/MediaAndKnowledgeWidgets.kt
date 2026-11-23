package com.example.ui.widgets

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.KnowledgeEntry
import com.example.data.media.MediaTrackState
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomSage

@Composable
fun MediaNowPlayingTile(
    mediaState: MediaTrackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("media_now_playing_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (!mediaState.hasNotificationAccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = StillroomAmber)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Media Session Monitor", color = StillroomLinen, fontSize = 14.sp)
                            Text(
                                "Enable Notification Access to read live track & artwork",
                                color = StillroomBone,
                                fontSize = 11.sp
                            )
                        }
                    }
                    TextButton(
                        onClick = onOpenNotificationAccess,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("grant_media_access_button")
                    ) {
                        Text("Connect", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    }
                }
            } else if (!mediaState.hasActiveSession) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Album, contentDescription = "No active audio", tint = StillroomAsh, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("NOW PLAYING", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh, letterSpacing = 1.4.sp)
                        Text("No active media session on device", fontSize = 13.sp, color = StillroomBone)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        val art = mediaState.artwork
                        if (art != null) {
                            Image(
                                bitmap = art.asImageBitmap(),
                                contentDescription = "Album artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Album, contentDescription = null, tint = StillroomAmber)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = mediaState.title,
                                fontFamily = PlusJakartaFontFamily,
                                fontSize = 15.sp,
                                color = StillroomLinen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mediaState.artist,
                                fontFamily = PlusJakartaFontFamily,
                                fontSize = 12.sp,
                                color = StillroomBone,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPrev,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("media_prev_button")
                        ) {
                            Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track", tint = StillroomLinen)
                        }
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("media_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (mediaState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                tint = StillroomAmber
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("media_next_button")
                        ) {
                            Icon(Icons.Outlined.SkipNext, contentDescription = "Next track", tint = StillroomLinen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyKnowledgeTile(
    entry: KnowledgeEntry,
    onNext: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onClick = onNext,
        onLongClick = onLongPress,
        modifier = modifier.testTag("knowledge_widget_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = StillroomSage, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = entry.category.label.uppercase(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = StillroomSage
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "“${entry.text}”",
                fontFamily = CormorantFontFamily,
                fontSize = 21.sp,
                lineHeight = 27.sp,
                fontStyle = FontStyle.Italic,
                color = StillroomLinen
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "— ${entry.attribution} · Tap for next",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = StillroomAsh
            )
        }
    }
}

@Composable
fun CustomMantraTile(
    text: String,
    onEditClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onClick = onEditClick,
        onLongClick = onLongPress,
        modifier = modifier.testTag("custom_mantra_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                fontFamily = CormorantFontFamily,
                fontSize = 19.sp,
                color = StillroomLinen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
