package com.snowsnooks.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TPRadioMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileRadioApp()
                }
            }
        }
    }
}

@Composable
fun MobileRadioApp() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TPRadio Mobile",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Coming Soon!",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun TPRadioMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TPRadioMobileTheme {
        MobileRadioApp()
    }
}