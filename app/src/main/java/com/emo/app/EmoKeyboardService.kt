package com.emo.app
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class EmoKeyboardService : InputMethodService() {
  override fun onCreateInputView(): View {
    val row = LinearLayout(this)
    row.setBackgroundColor(0xFF14121F.toInt())
    for (e in listOf("😀", "🫠", "🥹", "🫶", "🫨")) {
      val b = Button(this)
      b.text = e
      b.textSize = 24f
      b.setOnClickListener { currentInputConnection?.commitText(e, 1) }
      row.addView(b)
    }
    return row
  }
}
