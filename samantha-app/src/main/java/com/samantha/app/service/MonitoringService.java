package com.samantha.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.samantha.app.R;
import com.samantha.app.activity.MonitoringActivity;
import com.samantha.app.core.json.JsonFormatter;
import com.samantha.app.core.net.Connection;
import com.samantha.app.core.net.Message;
import com.samantha.app.core.net.MessageWrapper;
import com.samantha.app.core.net.ServerConnection;
import com.samantha.app.event.OnConnectionEvent;
import com.samantha.app.event.SendMessageEvent;
import com.samantha.app.exception.MonitoringException;
import com.samantha.app.service.sys.Monitoring;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import timber.log.Timber;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class MonitoringService extends Service implements Connection.Listener {


    public static final String EXTRA_APPLICATION_INFO = "APPLICATION_INFO_EXTRA";
    public static final String EXTRA_HOSTNAME = "EXTRA_HOSTNAME";
    public static final String EXTRA_PORT = "EXTRA_PORT";

    private static final int NOTIFICATION_ID = 0x01;
    private static final int DEFAULT_PORT = 8888;

    private ScheduledExecutorService mSocketScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture mSocketScheduledFuture;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private Connection mConnection;
    private Monitoring mMonitoring;
    private Binder mBinder = new Binder();
    private MessageHandler mMessageHandler;
    private boolean mConnected;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class Binder extends android.os.Binder {
        public MonitoringService getService() {
            return MonitoringService.this;
        }
    }


    @Override
    public void onCreate() {

        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this);
        mMonitoring = new Monitoring(this);
        mMessageHandler = new MessageHandler(this);
        mConnection = new ServerConnection(this);

        EventBus.getDefault().register(this);
        EventBus.getDefault().register(mMessageHandler);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final ApplicationInfo appInfo = intent.getParcelableExtra(EXTRA_APPLICATION_INFO);
        final int port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT);
        final String hostname = intent.getStringExtra(EXTRA_HOSTNAME);


        if (!isConnectionOpen()) {
            mConnection.setHostname(hostname);
            mConnection.setPort(port);
            openConnection();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
        closeConnection();
        EventBus.getDefault().unregister(mMessageHandler);
        EventBus.getDefault().unregister(this);
    }


    @DebugLog
    public void startMonitoring(ApplicationInfo appInfo) {

        try {
            startForeground(NOTIFICATION_ID, buildNotification(appInfo));
            mMonitoring.start(appInfo);
        } catch (IllegalStateException | NullPointerException e) {
            throw new MonitoringException(e.getMessage(), e);
        }

    }

    @DebugLog
    public void stopMonitoring() {
        if (mMonitoring.isMonitoring()) {
            mMonitoring.stop();
            stopForeground(true);
        }
    }


    public boolean isMonitoring() {
        return mMonitoring.isMonitoring();
    }

    @DebugLog
    public void openConnection() {
        mConnection.open();
    }


    @DebugLog
    public void closeConnection() {
        mConnection.close();
    }

    public void onEventBackgroundThread(SendMessageEvent event) {
        sendMessage(event.message, event.address);
    }

    @DebugLog
    public void sendMessage(Message message, String address) {
        if (isConnectionOpen()) {
            mConnection.sendMessage(message, address);
        }
    }

    private Notification buildNotification(ApplicationInfo appInfo) {

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Monitoring " + getPackageManager().getApplicationLabel(appInfo));

        Intent activityIntent = new Intent(this, MonitoringActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder
                .setContentTitle("Monitoring " + getPackageManager().getApplicationLabel(appInfo))
                .setSmallIcon(R.drawable.ic_launcher)
                .setStyle(inboxStyle)
                .setContentIntent(pendingIntent);


        return mNotificationBuilder.build();
    }

    public boolean isConnectionOpen() {
        return mConnection != null && mConnected;
    }

    @Override
    public void onOpen() {
        Timber.i("Socket connected");
        mConnected = true;
        if (mSocketScheduledFuture != null) {
            mSocketScheduledFuture.cancel(true);
        }

        EventBus.getDefault().post(new OnConnectionEvent(true));
    }

    @DebugLog
    @Override
    public void onMessage(String messageString) {
        try {
            mMessageHandler.onMessage(JsonFormatter.fromJson(messageString, MessageWrapper.class));
        } catch (IOException e) {
            Timber.e(e, "Error parsing message");
        }
    }


    @DebugLog
    @Override
    public void onClose() {
        Timber.i("Socket disconnected");
        mConnected = false;
        EventBus.getDefault().post(new OnConnectionEvent(false));
    }

    @Override
    public void onError(Exception error) {
        Timber.w(error, "Socket error");
    }
}
