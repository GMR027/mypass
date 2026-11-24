package app.mypass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AgendaAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("reminder_id") ?: return
        runCatching { AgendaReminders.fire(context, id) }
    }
}
