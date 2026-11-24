package app.mypass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AgendaBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> runCatching { AgendaReminders.rescheduleStored(context) }
        }
    }
}
