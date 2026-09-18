package com.futuretech.poweruser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.futuretech.poweruser.ui.CodingTocScreen
import com.futuretech.poweruser.ui.theme.FutureTechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FutureTechTheme(darkTheme = false) {
                CodingTocScreen()
            }
        }
    }
}
