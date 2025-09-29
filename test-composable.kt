package com.example.test

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Test Composable for Phase 3 rendering verification
 */
@Composable
fun TestButton() {
    Button(
        onClick = { /* Test action */ },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Test Button")
    }
}

@Composable
fun TestCard() {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Test Card",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "This is a test card for Phase 3 rendering verification",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {}) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {}) {
                    Text("OK")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TestButtonPreview() {
    MaterialTheme {
        TestButton()
    }
}

@Preview(showBackground = true)
@Composable
fun TestCardPreview() {
    MaterialTheme {
        TestCard()
    }
}
