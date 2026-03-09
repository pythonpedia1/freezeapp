package com.divinefreeze.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.util.ArrayList;
import java.util.List;

public class FreezeVpnService extends VpnService {

    public static final String ACTION_START = "com.divinefreeze.START";
    public static final String ACTION_STOP = "com.divinefreeze.STOP";
    public static final String EXTRA_PACKAGES = "packages";
    private static final String CHANNEL_ID = "freeze_channel";
    private static final int NOTIF_ID = 101;

    private ParcelFileDescriptor vpnInterface;
    private Thread packetThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            ArrayList<String> packages = intent.getStringArrayListExtra(EXTRA_PACKAGES);
            startVpn(packages);
        } else if (ACTION_STOP.equals(action)) {
            stopVpn();
        }
        return START_STICKY;
    }

    private void startVpn(List<String> frozenPackages) {
        stopVpn(); // stop any existing session first

        try {
            Builder builder = new Builder();
            builder.setSession("FreezeApp");
            builder.addAddress("10.0.0.2", 32);
            builder.addDnsServer("8.8.8.8");
            // Route ALL IPv4 traffic through us
            builder.addRoute("0.0.0.0", 0);

            // Only the frozen apps go through the VPN tunnel
            // All other apps are excluded → they keep their normal internet
            for (String pkg : frozenPackages) {
                try {
                    builder.addAllowedApplication(pkg);
                } catch (Exception e) {
                    // Package not found, skip
                }
            }

            // Allow our own app to bypass the VPN (so UI still works)
            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (Exception ignored) {}

            vpnInterface = builder.establish();

            if (vpnInterface != null) {
                // Start a thread that reads packets and drops them (black hole)
                packetThread = new Thread(() -> {
                    try {
                        java.io.FileInputStream in =
                                new java.io.FileInputStream(vpnInterface.getFileDescriptor());
                        byte[] buf = new byte[32767];
                        while (!Thread.interrupted()) {
                            // Just read and discard — packets go nowhere
                            int len = in.read(buf);
                            if (len < 0) break;
                            // We intentionally do NOT write anything back
                            // This means frozen apps send data into a black hole
                        }
                    } catch (Exception e) {
                        // VPN closed
                    }
                });
                packetThread.setDaemon(true);
                packetThread.start();

                startForeground(NOTIF_ID, buildNotification(frozenPackages.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVpn() {
        if (packetThread != null) {
            packetThread.interrupt();
            packetThread = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception ignored) {}
            vpnInterface = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification(int frozenCount) {
        createNotificationChannel();

        Intent tapIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, FreezeVpnService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nb = new Notification.Builder(this, CHANNEL_ID);
        } else {
            nb = new Notification.Builder(this);
        }

        return nb.setContentTitle("❄️ FreezeApp Active")
                .setContentText(frozenCount + " app(s) frozen — you appear offline")
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_play, "Unfreeze", stopPending)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FreezeApp Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows when apps are frozen");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
