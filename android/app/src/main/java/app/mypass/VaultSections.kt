package app.mypass

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun VaultSectionTabs(
    current: String,
    onDocuments: () -> Unit,
    onCredentials: () -> Unit,
    onMedical: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MpFilterChip(
            text = stringResource(R.string.documents),
            selected = current == "documents",
            onClick = onDocuments,
        )
        MpFilterChip(
            text = stringResource(R.string.credentials),
            selected = current == "credentials",
            onClick = onCredentials,
        )
        MpFilterChip(
            text = stringResource(R.string.medical),
            selected = current == "medical",
            onClick = onMedical,
        )
    }
}
