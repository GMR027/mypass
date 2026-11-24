package app.mypass

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AgendaReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("reminder_id") ?: return Result.success()
        runCatching { AgendaReminders.fire(applicationContext, id) }
        return Result.success()
    }
}
