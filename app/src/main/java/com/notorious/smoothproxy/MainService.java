/*
    MIT License

    Copyright (c) 2017 mr-notorious

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
 */

package com.notorious.smoothproxy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class MainService extends Service implements Pipe {
    private final SmoothProxy proxy = new SmoothProxy("127.0.0.1", 8888, this);
    private final IBinder binder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            proxy.start();
            startForeground(1, getNotification("Ready to serve."));
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        proxy.stop();
        stopForeground(true);
    }

    @Override
    public void setNotification(String text) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, getNotification(text));
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SmoothProxy")
                .setContentText(text)
                .setOngoing(true)
                .setPriority(2)
                .build();
    }

    void loadPreferences(SharedPreferences preferences) {
        proxy.init(
                preferences.getString("username", null),
                preferences.getString("password", null),
                preferences.getString("service", null),
                preferences.getString("server", null),
                preferences.getInt("quality", R.id.r_hd) - R.id.r_hd + 1
        );
    }

    class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }
}
