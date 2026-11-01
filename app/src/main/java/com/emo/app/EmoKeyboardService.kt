package com.emo.app
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EmoKeyboardService : InputMethodService() {
  private val groups = LinkedHashMap<String, MutableList<String>>()

  override fun onCreate() {
    super.onCreate()
    assets.open("emojis.txt").bufferedReader().forEachLine {
      val p = it.split("|")
      if (p.size == 2) groups.getOrPut(p[0]) { mutableListOf() }.add(p[1])
    }
  }

  override fun onCreateInputView(): View {
    val tf = Typeface.createFromAsset(assets, "NotoColorEmoji.ttf")
    val d = resources.displayMetrics.density
    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setBackgroundColor(0xFF14121F.toInt())

    val adapter = EmojiAdapter(tf) { currentInputConnection?.commitText(it, 1) }
    val list = RecyclerView(this)
    list.layoutManager = GridLayoutManager(this, 8)
    list.adapter = adapter
    list.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (250 * d).toInt())

    val tabs = LinearLayout(this)
    for ((name, items) in groups) {
      val t = TextView(this)
      t.text = items[0]
      t.typeface = tf
      t.textSize = 24f
      t.setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
      t.setOnClickListener { adapter.items = items; adapter.notifyDataSetChanged(); list.scrollToPosition(0) }
      tabs.addView(t)
    }
    val scroll = HorizontalScrollView(this)
    scroll.addView(tabs)

    val bottom = LinearLayout(this)
    fun key(label: String, action: () -> Unit): TextView {
      val k = TextView(this)
      k.text = label; k.textSize = 22f; k.setTextColor(0xFFFFFFFF.toInt()); k.gravity = Gravity.CENTER
      k.setOnClickListener { action() }
      k.layoutParams = LinearLayout.LayoutParams(0, (48 * d).toInt(), 1f)
      return k
    }
    bottom.addView(key("🌐") { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
    bottom.addView(key("⌫") { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) })

    adapter.items = groups.values.first()
    root.addView(scroll); root.addView(list); root.addView(bottom)
    return root
  }
}

class EmojiAdapter(val tf: Typeface, val onPick: (String) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  var items: List<String> = emptyList()
  override fun onCreateViewHolder(p: ViewGroup, v: Int): RecyclerView.ViewHolder {
    val t = TextView(p.context)
    t.typeface = tf; t.textSize = 26f; t.gravity = Gravity.CENTER
    t.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (52 * p.resources.displayMetrics.density).toInt())
    return object : RecyclerView.ViewHolder(t) {}
  }
  override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
    val t = h.itemView as TextView
    t.text = items[i]
    t.setOnClickListener { onPick(items[i]) }
  }
  override fun getItemCount() = items.size
}
