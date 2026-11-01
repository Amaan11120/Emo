package com.emo.app
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.emoji2.text.EmojiCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FancyStyle(val name: String, val up: Int, val lo: Int, val dig: Int = -1)

val STYLES = listOf(
  FancyStyle("Normal", 'A'.code, 'a'.code, '0'.code),
  FancyStyle("𝗕𝗼𝗹𝗱", 0x1D5D4, 0x1D5EE, 0x1D7EC),
  FancyStyle("𝓢𝓬𝓻𝓲𝓹𝓽", 0x1D4D0, 0x1D4EA),
  FancyStyle("𝕱𝖗𝖆𝖐𝖙𝖚𝖗", 0x1D56C, 0x1D586),
  FancyStyle("𝙼𝚘𝚗𝚘", 0x1D670, 0x1D68A, 0x1D7F6),
  FancyStyle("𝘽𝙤𝙡𝙙𝙄", 0x1D63C, 0x1D656),
  FancyStyle("Ⓑⓤⓑⓑⓛⓔ", 0x24B6, 0x24D0),
  FancyStyle("🅱🅻🅰🅲🅺", 0x1F170, 0x1F170),
  FancyStyle("🆂🆀", 0x1F130, 0x1F130),
  FancyStyle("Ｆｕｌｌ", 0xFF21, 0xFF41, 0xFF10)
)

fun fancy(c: Char, s: FancyStyle): String = when {
  c in 'A'..'Z' -> String(Character.toChars(s.up + (c - 'A')))
  c in 'a'..'z' -> String(Character.toChars(s.lo + (c - 'a')))
  c in '0'..'9' && s.dig >= 0 -> String(Character.toChars(s.dig + (c - '0')))
  else -> c.toString()
}

fun bindEmoji(t: TextView, e: String, tf: Typeface) {
  val ok = try {
    val ec = EmojiCompat.get()
    ec.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED &&
      ec.getEmojiMatch(e, Int.MAX_VALUE) == EmojiCompat.EMOJI_SUPPORTED
  } catch (x: Exception) { false }
  if (ok) { t.typeface = Typeface.DEFAULT; t.text = EmojiCompat.get().process(e) }
  else { t.typeface = tf; t.text = e }
}

class EmoKeyboardService : InputMethodService() {
  private val groups = LinkedHashMap<String, MutableList<String>>()
  private lateinit var tf: Typeface
  private lateinit var root: LinearLayout
  private var adapter: EmojiAdapter? = null
  private var shift = false
  private var fontIdx = 0
  private var d = 1f
  private val h = Handler(Looper.getMainLooper())

  private fun dp(v: Int) = (v * d).toInt()

  override fun onCreate() {
    super.onCreate()
    assets.open("emojis.txt").bufferedReader().forEachLine {
      val p = it.split("|")
      if (p.size == 2) groups.getOrPut(p[0]) { mutableListOf() }.add(p[1])
    }
    try {
      EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
        override fun onInitialized() { adapter?.notifyDataSetChanged() }
      })
    } catch (e: Exception) {}
  }

  override fun onCreateInputView(): View {
    tf = Typeface.createFromAsset(assets, "NotoColorEmoji.ttf")
    d = resources.displayMetrics.density
    root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setBackgroundColor(0xFF14121F.toInt())
    root.setPadding(dp(4), dp(4), dp(4), dp(6))
    letters()
    return root
  }

  override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
    super.onStartInputView(info, restarting)
    if (::root.isInitialized) { shift = false; letters() }
  }

  private fun key(label: String, w: Float = 1f, bg: Int = 0xFF2A2640.toInt(), onTap: () -> Unit): TextView {
    val k = TextView(this)
    k.text = label; k.textSize = 20f; k.setTextColor(Color.WHITE); k.gravity = Gravity.CENTER
    k.background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(8).toFloat() }
    k.layoutParams = LinearLayout.LayoutParams(0, dp(48), w).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
    k.setOnClickListener { k.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onTap() }
    return k
  }

  private fun row(keys: List<View>, sidePad: Int = 0): LinearLayout {
    val r = LinearLayout(this)
    r.setPadding(dp(sidePad), 0, dp(sidePad), 0)
    keys.forEach { r.addView(it) }
    return r
  }

  private fun del() { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) }

  private fun delKey(w: Float): TextView {
    val k = key("⌫", w, 0xFF3A3458.toInt()) {}
    val rep = object : Runnable { override fun run() { del(); h.postDelayed(this, 60) } }
    k.setOnClickListener(null)
    k.setOnTouchListener { _, e ->
      when (e.action) {
        MotionEvent.ACTION_DOWN -> { k.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); del(); h.postDelayed(rep, 400) }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> h.removeCallbacks(rep)
      }
      true
    }
    return k
  }

  private fun enter() {
    val ei = currentInputEditorInfo
    val a = ei.imeOptions and EditorInfo.IME_MASK_ACTION
    if (a != EditorInfo.IME_ACTION_NONE && a != EditorInfo.IME_ACTION_UNSPECIFIED &&
      (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) performEditorAction(a)
    else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
  }

  private fun type(c: Char) {
    val ch = if (shift) c.uppercaseChar() else c
    currentInputConnection?.commitText(fancy(ch, STYLES[fontIdx]), 1)
    if (shift) { shift = false; letters() }
  }

  private fun styleBar(): View {
    val bar = LinearLayout(this)
    STYLES.forEachIndexed { i, s ->
      val t = TextView(this)
      t.text = s.name; t.textSize = 14f; t.gravity = Gravity.CENTER
      t.setTextColor(if (i == fontIdx) Color.BLACK else Color.WHITE)
      t.background = GradientDrawable().apply {
        setColor(if (i == fontIdx) 0xFF22D3EE.toInt() else 0xFF2A2640.toInt()); cornerRadius = dp(16).toFloat()
      }
      t.setPadding(dp(14), dp(6), dp(14), dp(6))
      t.layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(3), dp(2), dp(3), dp(4)) }
      t.setOnClickListener { fontIdx = i; letters() }
      bar.addView(t)
    }
    val sv = HorizontalScrollView(this)
    sv.isHorizontalScrollBarEnabled = false
    sv.addView(bar)
    return sv
  }

  private fun bottomRow(first: String, firstTap: () -> Unit): LinearLayout {
    return row(listOf(
      key(first, 1.5f, 0xFF3A3458.toInt(), firstTap),
      key("😀", 1f, 0xFF3A3458.toInt()) { emoji() },
      key("space", 4f) { currentInputConnection?.commitText(" ", 1) },
      key(".", 1f) { currentInputConnection?.commitText(".", 1) },
      key("↵", 1.5f, 0xFF8B5CF6.toInt()) { enter() }
    ))
  }

  private fun letters() {
    root.removeAllViews()
    root.addView(styleBar())
    val r = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    root.addView(row(r[0].map { c -> key(c.toString()) { type(c) } }))
    root.addView(row(r[1].map { c -> key(c.toString()) { type(c) } }, 14))
    val third = ArrayList<View>()
    third.add(key("⇧", 1.5f, if (shift) 0xFF8B5CF6.toInt() else 0xFF3A3458.toInt()) { shift = !shift; letters() })
    r[2].forEach { c -> third.add(key(c.toString()) { type(c) }) }
    third.add(delKey(1.5f))
    root.addView(row(third))
    root.addView(bottomRow("?123") { symbols() })
  }

  private fun symbols() {
    root.removeAllViews()
    val r = listOf("1234567890", "@#$%&-+()/", "*\"':;!?,_=")
    r.forEachIndexed { i, s ->
      val ks = ArrayList<View>()
      s.forEach { c -> ks.add(key(c.toString()) { currentInputConnection?.commitText(c.toString(), 1) }) }
      if (i == 2) ks.add(delKey(1.5f))
      root.addView(row(ks))
    }
    root.addView(bottomRow("ABC") { letters() })
  }

  private fun emoji() {
    root.removeAllViews()
    val a = EmojiAdapter(tf) { currentInputConnection?.commitText(it, 1) }
    adapter = a
    val list = RecyclerView(this)
    list.layoutManager = GridLayoutManager(this, 8)
    list.adapter = a
    list.setHasFixedSize(true)
    list.setItemViewCacheSize(40)
    list.layoutParams = LinearLayout.LayoutParams(-1, dp(220))
    a.items = groups.values.first()

    val tabs = LinearLayout(this)
    for ((_, items) in groups) {
      val t = TextView(this)
      bindEmoji(t, items[0], tf)
      t.textSize = 22f
      t.setPadding(dp(14), dp(6), dp(14), dp(6))
      t.setOnClickListener { a.items = items; a.notifyDataSetChanged(); list.scrollToPosition(0) }
      tabs.addView(t)
    }
    val sv = HorizontalScrollView(this)
    sv.isHorizontalScrollBarEnabled = false
    sv.addView(tabs)

    root.addView(sv)
    root.addView(list)
    root.addView(row(listOf(
      key("ABC", 1.5f, 0xFF3A3458.toInt()) { letters() },
      key("🌐", 1f) { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() },
      key("space", 4f) { currentInputConnection?.commitText(" ", 1) },
      delKey(1.5f)
    )))
  }
}

class EmojiAdapter(val tf: Typeface, val onPick: (String) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  var items: List<String> = emptyList()
  override fun onCreateViewHolder(p: ViewGroup, v: Int): RecyclerView.ViewHolder {
    val t = TextView(p.context)
    t.textSize = 26f; t.gravity = Gravity.CENTER
    t.layoutParams = RecyclerView.LayoutParams(-1, (52 * p.resources.displayMetrics.density).toInt())
    return object : RecyclerView.ViewHolder(t) {}
  }
  override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
    val t = h.itemView as TextView
    bindEmoji(t, items[i], tf)
    t.setOnClickListener { onPick(items[i]) }
  }
  override fun getItemCount() = items.size
}
