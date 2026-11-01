package com.emo.app

import android.content.ClipboardManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Th(val bg: Int, val key: Int, val alt: Int, val acc: Int, val txt: Int, val onAcc: Int)

val THEME_NAMES = listOf("Purple", "AMOLED", "Light", "Ocean", "Rose")
val THEMES = listOf(
  Th(0xFF14121F.toInt(), 0xFF2A2640.toInt(), 0xFF3A3458.toInt(), 0xFF8B5CF6.toInt(), Color.WHITE, Color.WHITE),
  Th(0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF2A2A2A.toInt(), 0xFF22D3EE.toInt(), Color.WHITE, Color.BLACK),
  Th(0xFFE8E8EE.toInt(), 0xFFFFFFFF.toInt(), 0xFFC9C9D6.toInt(), 0xFF6D28D9.toInt(), Color.BLACK, Color.WHITE),
  Th(0xFF0B1E33.toInt(), 0xFF16324F.toInt(), 0xFF1F4568.toInt(), 0xFF38BDF8.toInt(), Color.WHITE, Color.BLACK),
  Th(0xFF2A0F1C.toInt(), 0xFF43192C.toInt(), 0xFF5A2339.toInt(), 0xFFFB7185.toInt(), Color.WHITE, Color.BLACK)
)

class FancyStyle(val name: String, val up: Int, val lo: Int, val dig: Int = -1, val comb: Int = 0)

val STYLES = listOf(
  FancyStyle("Normal", 'A'.code, 'a'.code, '0'.code),
  FancyStyle("𝗕𝗼𝗹𝗱", 0x1D5D4, 0x1D5EE, 0x1D7EC),
  FancyStyle("𝘐𝘵𝘢𝘭𝘪𝘤", 0x1D608, 0x1D622),
  FancyStyle("𝘽𝙤𝙡𝙙𝙄", 0x1D63C, 0x1D656),
  FancyStyle("𝓢𝓬𝓻𝓲𝓹𝓽", 0x1D4D0, 0x1D4EA),
  FancyStyle("𝕱𝖗𝖆𝖐𝖙𝖚𝖗", 0x1D56C, 0x1D586),
  FancyStyle("𝙼𝚘𝚗𝚘", 0x1D670, 0x1D68A, 0x1D7F6),
  FancyStyle("Ⓑⓤⓑⓑⓛⓔ", 0x24B6, 0x24D0),
  FancyStyle("🅐🅑🅒", 0x1F150, 0x1F150),
  FancyStyle("🅱🅻🅰🅲🅺", 0x1F170, 0x1F170),
  FancyStyle("🄰🄱🄲", 0x1F130, 0x1F130),
  FancyStyle("Ｆｕｌｌ", 0xFF21, 0xFF41, 0xFF10),
  FancyStyle("Strike", 'A'.code, 'a'.code, '0'.code, 0x0336),
  FancyStyle("Underline", 'A'.code, 'a'.code, '0'.code, 0x0332)
)

fun fancy(c: Char, s: FancyStyle): String {
  val b = when {
    c in 'A'..'Z' -> String(Character.toChars(s.up + (c - 'A')))
    c in 'a'..'z' -> String(Character.toChars(s.lo + (c - 'a')))
    c in '0'..'9' && s.dig >= 0 -> String(Character.toChars(s.dig + (c - '0')))
    else -> c.toString()
  }
  return if (s.comb != 0 && c != ' ') b + String(Character.toChars(s.comb)) else b
}

val ALTS = mapOf(
  'a' to listOf("à", "á", "â", "ä", "ã", "å", "ā"), 'e' to listOf("è", "é", "ê", "ë", "ē", "ę"),
  'i' to listOf("ì", "í", "î", "ï", "ī"), 'o' to listOf("ò", "ó", "ô", "ö", "õ", "ō", "ø"),
  'u' to listOf("ù", "ú", "û", "ü", "ū"), 'c' to listOf("ç", "ć", "č"), 'n' to listOf("ñ", "ń"),
  's' to listOf("ß", "ś", "š"), 'y' to listOf("ý", "ÿ"), 'z' to listOf("ž", "ź", "ż")
)
val DOT_ALTS = listOf(",", "?", "!", ":", ";", "'", "\"", "-", "@")
val COMMON = listOf("the", "and", "you", "that", "this", "with", "have", "will", "what", "when", "where", "how", "are", "for", "not", "but", "can", "hello", "hi", "thanks", "thank", "please", "good", "morning", "night", "love", "yes", "no", "okay", "sorry", "bro", "kya", "hai", "haan", "nahi", "kaise", "kab", "kaha", "mera", "tera", "aap", "tum", "main", "mein", "bhai", "acha", "accha", "theek", "thik", "abhi", "bahut", "kuch", "karo", "kar", "raha", "rahi", "kyun", "chalo", "aur", "lekin", "phir", "wala", "kal", "aaj", "just", "because", "about", "would", "could", "there", "their", "going", "today", "tomorrow", "friend", "happy", "birthday", "welcome", "really", "message", "call", "know", "think", "time", "like", "want", "need", "very", "also", "from", "they", "been", "some", "more", "work", "home", "food")

private val glyphPaint = Paint()
private val kindCache = HashMap<String, Int>()

// 0 = system renders it, 1 = EmojiCompat, 2 = bundled Noto font
fun bindEmoji(t: TextView, e: String, tf: Typeface) {
  var k = kindCache[e]
  if (k == null) {
    var kk = 2
    var decided = true
    if (android.os.Build.VERSION.SDK_INT >= 23 && glyphPaint.hasGlyph(e)) kk = 0
    else {
      try {
        val ec = EmojiCompat.get()
        if (ec.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED) {
          val s = ec.process(e)
          if (s is android.text.Spanned && s.getSpans(0, s.length, EmojiSpan::class.java).isNotEmpty()) kk = 1
        } else decided = false
      } catch (x: Exception) { decided = false }
    }
    if (decided) kindCache[e] = kk
    k = kk
  }
  when (k) {
    0 -> { t.typeface = Typeface.DEFAULT; t.text = e }
    1 -> { t.typeface = Typeface.DEFAULT; t.text = EmojiCompat.get().process(e) }
    else -> { t.typeface = tf; t.text = e }
  }
}

class EmoKeyboardService : InputMethodService() {
  private val groups = LinkedHashMap<String, MutableList<String>>()
  private var glist: List<List<String>> = emptyList()
  private val names = ArrayList<Pair<String, String>>()
  private val variants = HashMap<String, MutableList<String>>()
  private lateinit var tf: Typeface
  private lateinit var root: LinearLayout
  private lateinit var prefs: android.content.SharedPreferences
  private var adapter: EmojiAdapter? = null
  private var shiftState = 0
  private var fontIdx = 0
  private var d = 1f
  private var th = THEMES[0]
  private var keyH = 48
  private var vib = true
  private var snd = false
  private var hindi = false
  private var hmode = 0
  private var tab = 0
  private var bgBmp: Bitmap? = null
  private var bgStamp = 0L
  private val recents = ArrayList<String>()
  private val favs = ArrayList<String>()
  private val clips = ArrayList<String>()
  private val words = HashMap<String, Int>()
  private val query = StringBuilder()
  private var sugg: Array<TextView>? = null
  private var pop: PopupWindow? = null
  private val h = Handler(Looper.getMainLooper())

  private fun dp(v: Int) = (v * d).toInt()
  private fun norm(s: String) = s.replace("\uFE0F", "")
  private fun kc(c: Int) = if (bgBmp != null) (c and 0x00FFFFFF) or 0xCC000000.toInt() else c

  private fun load(k: String, l: MutableList<String>, sep: String) {
    l.clear()
    (prefs.getString(k, "") ?: "").split(sep).filter { it.isNotEmpty() }.forEach { l.add(it) }
  }
  private fun save(k: String, l: List<String>, sep: String) { prefs.edit().putString(k, l.joinToString(sep)).apply() }

  override fun onCreate() {
    super.onCreate()
    prefs = getSharedPreferences("emo", MODE_PRIVATE)
    assets.open("emojis.txt").bufferedReader().forEachLine {
      val p = it.split("|")
      if (p.size >= 3) {
        val e = p[1]
        val mods = ArrayList<Int>()
        val sb = StringBuilder()
        var i = 0
        while (i < e.length) {
          val cp = e.codePointAt(i)
          if (cp in 0x1F3FB..0x1F3FF) mods.add(cp) else sb.appendCodePoint(cp)
          i += Character.charCount(cp)
        }
        if (mods.isEmpty()) {
          groups.getOrPut(p[0]) { mutableListOf() }.add(e)
          names.add(Pair(e, p[2].lowercase()))
        } else if (mods.toSet().size == 1) {
          variants.getOrPut(norm(sb.toString())) { mutableListOf() }.add(e)
        }
      }
    }
    glist = groups.values.toList()
    load("recents", recents, " "); load("favs", favs, " "); load("clips", clips, "\u0001")
    (prefs.getString("words", "") ?: "").split("\n").forEach {
      val p = it.split("\t")
      if (p.size == 2) words[p[0]] = p[1].toIntOrNull() ?: 1
    }
    try {
      EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
        override fun onInitialized() { adapter?.notifyDataSetChanged() }
      })
    } catch (e: Exception) {}
    try {
      (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener { grabClip() }
    } catch (e: Exception) {}
  }

  private fun grabClip() {
    try {
      val c = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
      val t = c?.getItemAt(0)?.coerceToText(this)?.toString()
      if (!t.isNullOrBlank()) {
        clips.remove(t); clips.add(0, t)
        while (clips.size > 20) clips.removeAt(clips.size - 1)
        save("clips", clips, "\u0001")
      }
    } catch (e: Exception) {}
  }

  private fun loadPrefs() {
    th = THEMES[prefs.getInt("theme", 0).coerceIn(0, THEMES.size - 1)]
    keyH = prefs.getInt("keyH", 48)
    vib = prefs.getBoolean("vib", true)
    snd = prefs.getBoolean("snd", false)
    val f = java.io.File(filesDir, "bg.jpg")
    if (prefs.getBoolean("useBg", false) && f.exists()) {
      if (bgBmp == null || f.lastModified() != bgStamp) { bgBmp = BitmapFactory.decodeFile(f.path); bgStamp = f.lastModified() }
    } else bgBmp = null
  }

  override fun onCreateInputView(): View {
    tf = Typeface.createFromAsset(assets, "NotoColorEmoji.ttf")
    d = resources.displayMetrics.density
    root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    loadPrefs()
    home()
    return root
  }

  override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
    super.onStartInputView(info, restarting)
    if (!::root.isInitialized) return
    loadPrefs()
    val cm = currentInputConnection?.getCursorCapsMode(info?.inputType ?: 0) ?: 0
    shiftState = if (cm != 0) 1 else 0
    home()
  }

  private fun home() { if (hindi) hindiPage() else letters() }

  private fun begin() {
    pop?.dismiss(); pop = null; sugg = null
    root.removeAllViews()
    val b = bgBmp
    if (b != null) {
      val bd = BitmapDrawable(resources, b)
      bd.colorFilter = PorterDuffColorFilter(0xFF8A8A8A.toInt(), PorterDuff.Mode.MULTIPLY)
      root.background = bd
    } else root.setBackgroundColor(th.bg)
    root.setPadding(dp(4), dp(4), dp(4), dp(6))
  }

  private fun feedback(v: View) {
    if (vib) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    if (snd) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
  }

  private fun key(label: String, w: Float = 1f, bg: Int = th.key, size: Float = 20f, alts: List<String>? = null, onTap: () -> Unit): TextView {
    val k = TextView(this)
    k.text = label; k.textSize = size; k.setTextColor(th.txt); k.gravity = Gravity.CENTER; k.maxLines = 1
    k.background = GradientDrawable().apply { setColor(kc(bg)); cornerRadius = dp(8).toFloat() }
    k.layoutParams = LinearLayout.LayoutParams(0, dp(keyH), w).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
    k.setOnClickListener { feedback(k); onTap() }
    if (alts != null) k.setOnLongClickListener {
      showAlt(k, alts.map { a -> Pair<String, () -> Unit>(a, { commit(a) }) }); true
    }
    return k
  }

  private fun row(keys: List<View>, sidePad: Int = 0): LinearLayout {
    val r = LinearLayout(this)
    r.setPadding(dp(sidePad), 0, dp(sidePad), 0)
    keys.forEach { r.addView(it) }
    return r
  }

  private fun showAlt(anchor: View, items: List<Pair<String, () -> Unit>>) {
    pop?.dismiss()
    val box = LinearLayout(this)
    box.background = GradientDrawable().apply { setColor(th.alt); cornerRadius = dp(12).toFloat(); setStroke(dp(1), th.acc) }
    box.setPadding(dp(6), dp(6), dp(6), dp(6))
    val p = PopupWindow(box, ViewGroup.LayoutParams.WRAP_CONTENT, dp(58))
    items.forEach { (label, act) ->
      val t = TextView(this)
      bindEmoji(t, label, tf)
      t.textSize = 24f; t.setTextColor(th.txt); t.gravity = Gravity.CENTER
      t.setPadding(dp(10), 0, dp(10), 0)
      t.setOnClickListener { act(); p.dismiss() }
      box.addView(t)
    }
    p.isOutsideTouchable = true
    pop = p
    p.showAsDropDown(anchor, 0, -(anchor.height + dp(58)))
  }

  private fun commit(s: String) { currentInputConnection?.commitText(s, 1); updateSugg() }
  private fun del() { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL); h.postDelayed({ updateSugg() }, 40) }
  private fun imePicker() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }

  private fun curWord(): String {
    val b = currentInputConnection?.getTextBeforeCursor(30, 0)?.toString() ?: ""
    var i = b.length
    while (i > 0 && (b[i - 1].isLetter() || b[i - 1] == '\'')) i--
    return b.substring(i)
  }

  private fun learn(w: String) {
    val lw = w.lowercase()
    if (lw.length < 2) return
    words[lw] = (words[lw] ?: 0) + 1
    if (words.size > 3000) words.keys.take(500).forEach { words.remove(it) }
    prefs.edit().putString("words", words.entries.joinToString("\n") { it.key + "\t" + it.value }).apply()
  }

  private fun spaceTap() { val w = curWord(); if (w.isNotEmpty()) learn(w); commit(" ") }

  private fun enter() {
    val w = curWord(); if (w.isNotEmpty()) learn(w)
    val ei = currentInputEditorInfo
    if (ei != null) {
      val a = ei.imeOptions and EditorInfo.IME_MASK_ACTION
      if (a != EditorInfo.IME_ACTION_NONE && a != EditorInfo.IME_ACTION_UNSPECIFIED &&
        (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) { currentInputConnection?.performEditorAction(a); return }
    }
    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
  }

  private fun type(c: Char) {
    val ch = if (shiftState > 0) c.uppercaseChar() else c
    commit(fancy(ch, STYLES[fontIdx]))
    if (shiftState == 1) { shiftState = 0; letters() }
  }

  private fun updateSugg() {
    val s = sugg ?: return
    val cw = curWord()
    val w = cw.lowercase()
    val out = ArrayList<String>()
    if (w.isNotEmpty()) {
      val pool = HashMap<String, Int>()
      COMMON.forEach { pool[it] = 1 }
      words.forEach { (k, v) -> pool[k] = v + 2 }
      val cap = cw[0].isUpperCase()
      pool.entries.filter { it.key.startsWith(w) && it.key != w }.sortedByDescending { it.value }.take(3)
        .forEach { out.add(if (cap) it.key.replaceFirstChar { c -> c.uppercase() } else it.key) }
    }
    for (i in 0..2) s[i].text = out.getOrElse(i) { "" }
  }

  private fun pickSugg(w: String) {
    if (w.isEmpty()) return
    val cw = curWord()
    currentInputConnection?.deleteSurroundingText(cw.length, 0)
    currentInputConnection?.commitText("$w ", 1)
    learn(w); updateSugg()
  }

  private fun delKey(w: Float): TextView {
    val k = key("⌫", w, th.alt) {}
    val rep = object : Runnable { override fun run() { del(); h.postDelayed(this, 60) } }
    k.setOnClickListener(null)
    k.setOnTouchListener { _, e ->
      when (e.action) {
        MotionEvent.ACTION_DOWN -> { feedback(k); del(); h.postDelayed(rep, 400) }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> h.removeCallbacks(rep)
      }
      true
    }
    return k
  }

  private fun suggBar(): View {
    val bar = LinearLayout(this)
    val arr = Array(3) {
      val t = TextView(this)
      t.textSize = 16f; t.gravity = Gravity.CENTER; t.setTextColor(th.txt); t.maxLines = 1
      t.layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f)
      t.setOnClickListener { pickSugg(t.text.toString()) }
      bar.addView(t)
      t
    }
    sugg = arr
    return bar
  }

  private fun styleBar(): View {
    val bar = LinearLayout(this)
    STYLES.forEachIndexed { i, s ->
      val t = TextView(this)
      t.text = s.name; t.textSize = 14f; t.gravity = Gravity.CENTER
      t.setTextColor(if (i == fontIdx) th.onAcc else th.txt)
      t.background = GradientDrawable().apply { setColor(if (i == fontIdx) th.acc else kc(th.key)); cornerRadius = dp(16).toFloat() }
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
    val lang = key(if (hindi) "EN" else "हिं", 1f, th.alt, 16f) { hindi = !hindi; hmode = 0; home() }
    lang.setOnLongClickListener { imePicker(); true }
    val spc = key("space", 3.5f) { spaceTap() }
    spc.setOnLongClickListener { imePicker(); true }
    val ent = key("↵", 1.4f, th.acc) { enter() }
    ent.setTextColor(th.onAcc)
    return row(listOf(
      key(first, 1.4f, th.alt, 16f, null, firstTap),
      key("😀", 1f, th.alt) { emoji() },
      lang, spc,
      key(".", 1f, th.key, 20f, DOT_ALTS) { commit(".") },
      ent
    ))
  }

  private fun letters() {
    begin()
    root.addView(suggBar())
    root.addView(styleBar())
    val r = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    fun lk(c: Char) = key(if (shiftState > 0) c.uppercase() else c.toString(),
      alts = ALTS[c]?.map { if (shiftState > 0) it.uppercase() else it }) { type(c) }
    root.addView(row(r[0].map { lk(it) }))
    root.addView(row(r[1].map { lk(it) }, 14))
    val third = ArrayList<View>()
    val sh = key(if (shiftState == 2) "⇪" else "⇧", 1.5f, if (shiftState > 0) th.acc else th.alt) {
      shiftState = if (shiftState == 0) 1 else 0; letters()
    }
    sh.setTextColor(if (shiftState > 0) th.onAcc else th.txt)
    sh.setOnLongClickListener { shiftState = 2; letters(); true }
    third.add(sh)
    r[2].forEach { third.add(lk(it)) }
    third.add(delKey(1.5f))
    root.addView(row(third))
    root.addView(bottomRow("?123") { symbols() })
    updateSugg()
  }

  private fun symbols() {
    begin()
    val r = listOf("1234567890", "@#$%&-+()/", "*\"':;!?,_=")
    r.forEachIndexed { i, s ->
      val ks = ArrayList<View>()
      s.forEach { c -> ks.add(key(c.toString()) { commit(c.toString()) }) }
      if (i == 2) ks.add(delKey(1.5f))
      root.addView(row(ks))
    }
    root.addView(row(listOf("₹", "€", "£", "¥", "~", "|", "{", "}", "[", "]").map { s -> key(s) { commit(s) } }))
    root.addView(bottomRow("ABC") { home() })
  }

  private fun hindiPage() {
    begin()
    fun chars(s: String) = s.map { it.toString() }
    fun hr(l: List<String>, pre: View? = null, post: View? = null) {
      val ks = ArrayList<View>()
      if (pre != null) ks.add(pre)
      l.forEach { s -> ks.add(key(s) { commit(s) }) }
      if (post != null) ks.add(post)
      root.addView(row(ks))
    }
    val tog = key(if (hmode == 0) "अ आ" else "क ख", 1.5f, th.alt, 16f) { hmode = 1 - hmode; hindiPage() }
    if (hmode == 0) {
      hr(chars("कखगघचछजझटठ")); hr(chars("डढणतथदधनपफ")); hr(chars("बभमयरलवशष"))
      hr(listOf("स", "ह", "ङ", "ञ", "क्ष", "त्र", "ज्ञ"), tog, delKey(1.5f))
    } else {
      hr(chars("अआइईउऊएऐओऔ")); hr(chars("ािीुूेैोौ"))
      hr(chars("ंँःृ़्।ऋ"), null, delKey(1.5f))
      hr(listOf("ळ", "ॉ", "ज़", "फ़", "ड़", "ढ़"), tog, null)
    }
    root.addView(bottomRow("?123") { symbols() })
  }

  private fun items(): List<String> = when (tab) {
    0 -> recents
    1 -> favs
    else -> glist.getOrElse(tab - 2) { emptyList() }
  }

  private fun pickEmoji(e: String) {
    commit(e)
    recents.remove(e); recents.add(0, e)
    while (recents.size > 40) recents.removeAt(recents.size - 1)
    save("recents", recents, " ")
  }

  private fun toggleFav(e: String) {
    if (favs.contains(e)) favs.remove(e) else favs.add(0, e)
    save("favs", favs, " ")
    if (tab == 1) { adapter?.items = favs; adapter?.notifyDataSetChanged() }
  }

  private fun emojiMenu(v: View, e: String) {
    val opts = ArrayList<Pair<String, () -> Unit>>()
    opts.add(Pair<String, () -> Unit>(if (favs.contains(e)) "💔" else "⭐", { toggleFav(e) }))
    variants[norm(e)]?.forEach { x -> opts.add(Pair<String, () -> Unit>(x, { pickEmoji(x) })) }
    showAlt(v, opts)
  }

  private fun emoji() {
    begin()
    val a = EmojiAdapter(tf, { e -> pickEmoji(e) }, { v, e -> emojiMenu(v, e) })
    adapter = a
    val list = RecyclerView(this)
    list.layoutManager = GridLayoutManager(this, 8)
    list.adapter = a
    list.setHasFixedSize(true)
    list.setItemViewCacheSize(40)
    list.layoutParams = LinearLayout.LayoutParams(-1, dp(220))

    val tabViews = ArrayList<TextView>()
    fun show(i: Int) {
      tab = i
      a.items = items(); a.notifyDataSetChanged(); list.scrollToPosition(0)
      tabViews.forEachIndexed { j, t ->
        t.background = GradientDrawable().apply { setColor(if (j == i) th.alt else Color.TRANSPARENT); cornerRadius = dp(10).toFloat() }
      }
    }
    val tabs = LinearLayout(this)
    val labels = listOf("🕘", "⭐") + glist.map { it[0] }
    labels.forEachIndexed { i, l ->
      val t = TextView(this)
      bindEmoji(t, l, tf)
      t.textSize = 22f; t.gravity = Gravity.CENTER
      t.setPadding(dp(12), dp(6), dp(12), dp(6))
      t.setOnClickListener { show(i) }
      tabs.addView(t); tabViews.add(t)
    }
    val sv = HorizontalScrollView(this)
    sv.isHorizontalScrollBarEnabled = false
    sv.addView(tabs)
    root.addView(sv)
    root.addView(list)
    root.addView(row(listOf(
      key("ABC", 1.5f, th.alt, 16f) { home() },
      key("🔍", 1f, th.alt) { query.clear(); search() },
      key("📋", 1f, th.alt) { clipPanel() },
      key("space", 3.5f) { spaceTap() },
      delKey(1.5f)
    )))
    show(if (tab == 0 && recents.isEmpty()) 2 else tab)
  }

  private fun search() {
    begin()
    val q = TextView(this)
    q.textSize = 18f; q.setTextColor(th.txt)
    q.setPadding(dp(12), dp(8), dp(12), dp(8))
    q.background = GradientDrawable().apply { setColor(kc(th.key)); cornerRadius = dp(10).toFloat() }
    q.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
    val a = EmojiAdapter(tf, { e -> pickEmoji(e) }, { v, e -> emojiMenu(v, e) })
    adapter = a
    val list = RecyclerView(this)
    list.layoutManager = GridLayoutManager(this, 8)
    list.adapter = a
    list.layoutParams = LinearLayout.LayoutParams(-1, dp(100))
    fun refresh() {
      q.text = if (query.isEmpty()) "Search emoji…" else query.toString()
      val s = query.toString().trim().lowercase()
      a.items = if (s.isEmpty()) emptyList() else names.filter { it.second.contains(s) }.take(80).map { it.first }
      a.notifyDataSetChanged()
    }
    root.addView(q); root.addView(list)
    val saved = keyH
    keyH = 40
    root.addView(row("qwertyuiop".map { c -> key(c.toString(), size = 18f) { query.append(c); refresh() } }))
    root.addView(row("asdfghjkl".map { c -> key(c.toString(), size = 18f) { query.append(c); refresh() } }, 14))
    val third = ArrayList<View>()
    "zxcvbnm".forEach { c -> third.add(key(c.toString(), size = 18f) { query.append(c); refresh() }) }
    third.add(key("⌫", 1.5f, th.alt) { if (query.isNotEmpty()) query.deleteCharAt(query.length - 1); refresh() })
    root.addView(row(third))
    root.addView(row(listOf(
      key("ABC", 1.5f, th.alt, 16f) { emoji() },
      key("space", 4f) { query.append(' '); refresh() },
      key("✕", 1.5f, th.alt) { query.clear(); refresh() }
    )))
    keyH = saved
    refresh()
  }

  private fun clipPanel() {
    begin()
    grabClip()
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    if (clips.isEmpty()) {
      val t = TextView(this)
      t.text = "Clipboard is empty. Copy some text first."
      t.setTextColor(th.txt); t.setPadding(dp(12), dp(16), dp(12), dp(16))
      col.addView(t)
    }
    clips.toList().forEach { c ->
      val t = TextView(this)
      t.text = c; t.maxLines = 2; t.ellipsize = TextUtils.TruncateAt.END; t.textSize = 15f
      t.setTextColor(th.txt); t.setPadding(dp(12), dp(10), dp(12), dp(10))
      t.background = GradientDrawable().apply { setColor(kc(th.key)); cornerRadius = dp(10).toFloat() }
      t.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(4), dp(3), dp(4), dp(3)) }
      t.setOnClickListener { commit(c) }
      t.setOnLongClickListener { clips.remove(c); save("clips", clips, "\u0001"); clipPanel(); true }
      col.addView(t)
    }
    val sv = ScrollView(this)
    sv.addView(col)
    sv.layoutParams = LinearLayout.LayoutParams(-1, dp(220))
    root.addView(sv)
    root.addView(row(listOf(
      key("ABC", 1.5f, th.alt, 16f) { home() },
      key("😀", 1f, th.alt) { emoji() },
      key("Clear all", 2f, th.alt, 15f) { clips.clear(); save("clips", clips, "\u0001"); clipPanel() },
      delKey(1.5f)
    )))
  }
}

class EmojiAdapter(val tf: Typeface, val onPick: (String) -> Unit, val onLong: (View, String) -> Unit) :
  RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  var items: List<String> = emptyList()
  override fun onCreateViewHolder(p: ViewGroup, v: Int): RecyclerView.ViewHolder {
    val t = TextView(p.context)
    t.textSize = 26f; t.gravity = Gravity.CENTER
    t.layoutParams = RecyclerView.LayoutParams(-1, (52 * p.resources.displayMetrics.density).toInt())
    return object : RecyclerView.ViewHolder(t) {}
  }
  override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
    val t = h.itemView as TextView
    val e = items[i]
    bindEmoji(t, e, tf)
    t.setOnClickListener { onPick(e) }
    t.setOnLongClickListener { onLong(t, e); true }
  }
  override fun getItemCount() = items.size
}
