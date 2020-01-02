/*
    MIT License

    Copyright (c) 2020 mr-notorious

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
import android.text.format.DateFormat;

public final class MainService extends Service implements Bind {
    private final HttpServer server = new HttpServer("127.0.0.1", 8888, this);
    private final IBinder binder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            server.start();
            startForeground(1, getNotification("Running"));
        } catch (Exception e) {
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
        server.stop();
        stopForeground(true);
    }

    @Override
    public boolean is24HourFormat() {
        return DateFormat.is24HourFormat(this);
    }

    @Override
    public void setNotification(String text) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, getNotification(text));
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(text)
                .setOngoing(true)
                .setPriority(2)
                .build();
    }

    void loadPreferences(SharedPreferences preferences) {
        server.init(
                preferences.getString(Bind.USERNAME, null),
                preferences.getString(Bind.PASSWORD, null),
                preferences.getString(Bind.SERVICE, null),
                preferences.getString(Bind.SERVER, null),
                preferences.getInt(Bind.QUALITY, R.id.r_hd) - R.id.r_hd + 1
        );
    }

    class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }
}
