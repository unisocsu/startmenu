package kernel.unisocsu.startnenu;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;

public class StatusNotificationService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        showNotification("Start Menu Opened");
    }

    private void showNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "start_menu_channel")
                .setContentTitle("Start Menu")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        
        // Note: For API 19, simpler Notification creation might be needed.
        // This is a simplified example.
        notificationManager.notify(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
