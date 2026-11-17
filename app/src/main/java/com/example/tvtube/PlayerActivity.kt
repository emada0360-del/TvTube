package com.example.tvtube

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.StreamInfo

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var view: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = PlayerView(this)
        setContentView(view)
        val url = intent.getStringExtra("url") ?: return finish()
        Thread {
            try {
                val info = StreamInfo.getInfo(url)
                val video = info.videoOnlyStreams
                    .filter { it.format == MediaFormat.MPEG_4 && it.height <= 720 }
                    .maxByOrNull { it.height }
                val audio = info.audioStreams
                    .filter { it.format == MediaFormat.M4A }
                    .maxByOrNull { it.averageBitrate }
                val progressive = info.videoStreams.maxByOrNull { it.height }
                runOnUiThread { play(video?.content, audio?.content, progressive?.content) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun play(v: String?, a: String?, p: String?) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 5.1.1) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)
        val f = ProgressiveMediaSource.Factory(http)
        val source = when {
            v != null && a != null -> MergingMediaSource(
                f.createMediaSource(MediaItem.fromUri(v)),
                f.createMediaSource(MediaItem.fromUri(a)))
            p != null -> f.createMediaSource(MediaItem.fromUri(p))
            else -> { Toast.makeText(this, "ما لقيت ستريم مناسب", Toast.LENGTH_LONG).show(); return }
        }
        player = ExoPlayer.Builder(this).build().also {
            it.setMediaSource(source); it.prepare(); it.playWhenReady = true
            view.player = it
        }
    }

    override fun onDestroy() { player?.release(); super.onDestroy() }
}
