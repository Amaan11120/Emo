package com.emo.app
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B1035), Color(0xFF0B0B14)))),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
          Text("🫶", fontSize = 80.sp)
          Spacer(Modifier.height(12.dp))
          Text("Emo", color = Color.White, fontSize = 40.sp)
          Text("Every emoji, on every phone", color = Color(0xFFB9B4D6), fontSize = 16.sp)
          Spacer(Modifier.height(40.dp))
          Button(
            onClick = {
              startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier.fillMaxWidth().height(60.dp)
          ) { Text("1. Enable Emo Keyboard", fontSize = 18.sp) }
          Spacer(Modifier.height(14.dp))
          Button(
            onClick = {
              (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(60.dp)
          ) { Text("2. Set as default", fontSize = 18.sp) }
        }
      }
    }
  }
}
