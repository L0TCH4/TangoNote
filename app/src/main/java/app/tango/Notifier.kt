package app.tango

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.Calendar

object Notifier {
    const val CH = "review"
    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26)
            c.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CH, "復習リマインド", NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun show(c: Context, n: Int) {
        ensureChannel(c)
        val nm = NotificationManagerCompat.from(c)
        if (!nm.areNotificationsEnabled()) return
        val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val nt = NotificationCompat.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("復習の時間です")
            .setContentText("復習できる単語が${n}語あります")
            .setContentIntent(pi).setAutoCancel(true).build()
        try { nm.notify(1, nt) } catch (e: SecurityException) {}
    }
}

class ReminderWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result {
        val sp = applicationContext.getSharedPreferences("t", 0)
        val arr = JSONArray(sp.getString("due", "[]"))
        val now = System.currentTimeMillis()
        var n = 0
        for (i in 0 until arr.length()) if (arr.getLong(i) <= now) n++
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (n > 0 && h in 8..21 && now - sp.getLong("last", 0) > 4 * 3600_000L) {
            Notifier.show(applicationContext, n)
            sp.edit().putLong("last", now).apply()
        }
        return Result.success()
    }
}
