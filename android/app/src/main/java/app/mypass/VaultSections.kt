package app.mypass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun VaultSectionTabs(
    current: String,
    onDocuments: () -> Unit,
    onCredentials: () -> Unit,
    onMedical: () -> Unit,
    onInsurance: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MpColors.SurfaceLow,
        border = BorderStroke(1.dp, MpColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            VaultTab(
                label = stringResource(R.string.documents),
                icon = Icons.Filled.List,
                selected = current == "documents",
                onClick = onDocuments,
                modifier = Modifier.weight(1f),
            )
            VaultTab(
                label = stringResource(R.string.credentials),
                icon = Icons.Filled.Lock,
                selected = current == "credentials",
                onClick = onCredentials,
                modifier = Modifier.weight(1f),
            )
            VaultTab(
                label = stringResource(R.string.medical),
                icon = Icons.Filled.Info,
                selected = current == "medical",
                onClick = onMedical,
                modifier = Modifier.weight(1f),
            )
            VaultTab(
                label = stringResource(R.string.insurance),
                icon = Icons.Filled.CheckCircle,
                selected = current == "insurance",
                onClick = onInsurance,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VaultTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MpColors.Surface else MpColors.SurfaceLow,
        border = if (selected) BorderStroke(1.dp, MpColors.Border) else null,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MpColors.Primary else MpColors.OnSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MpColors.Primary else MpColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
