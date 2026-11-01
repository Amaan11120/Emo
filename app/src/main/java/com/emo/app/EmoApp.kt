package com.emo.app
import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat

class EmoApp : Application() {
  override fun onCreate() {
    super.onCreate()
    EmojiCompat.init(BundledEmojiCompatConfig(this).setReplaceAll(true))
  }
}
