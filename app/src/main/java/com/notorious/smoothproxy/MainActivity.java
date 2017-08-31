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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences mPreferences;
    private MainService mService;
    private boolean mIsBound;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etService;
    private EditText etServer;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MainService.LocalBinder) service).getService();
            mService.loadPreferences(mPreferences);
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreferences = getPreferences(Context.MODE_PRIVATE);

        etUsername = (EditText) findViewById(R.id.et_username);
        etPassword = (EditText) findViewById(R.id.et_password);
        etService = (EditText) findViewById(R.id.et_service);
        etServer = (EditText) findViewById(R.id.et_server);

        Button bSave = (Button) findViewById(R.id.b_save);
        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
            }
        });

        Button bExit = (Button) findViewById(R.id.b_exit);
        bExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        startService(new Intent(this, MainService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, MainService.class), mConnection, BIND_AUTO_CREATE);
        etUsername.setText(mPreferences.getString("username", null));
        etPassword.setText(mPreferences.getString("password", null));
        etService.setText(mPreferences.getString("service", null));
        etServer.setText(mPreferences.getString("server", null));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, MainService.class));
    }

    @Override
    public void onBackPressed() {
        savePreferences();
        moveTaskToBack(true);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("username", etUsername.getText().toString());
        editor.putString("password", etPassword.getText().toString());
        editor.putString("service", etService.getText().toString());
        editor.putString("server", etServer.getText().toString());
        editor.commit();
        if (mIsBound) mService.loadPreferences(mPreferences);
        Toast.makeText(getApplicationContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
    }
}
