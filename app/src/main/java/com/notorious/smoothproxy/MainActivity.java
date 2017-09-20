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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.notorious.smoothproxy.pojo.Server;
import com.notorious.smoothproxy.pojo.ServiceProvider;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences mPreferences;
    private MainService mService;
    private boolean mIsBound;
    private EditText etUsername;
    private EditText etPassword;
    private Spinner spinnerService;
    private Spinner spinnerServer;
    private RadioGroup rgQuality;
    private RadioGroup rgEpg;

    private ServiceProvider[] serviceProviders = new ServiceProvider[] {
            new ServiceProvider("view247", "Live247"),
            new ServiceProvider("viewmmasr", "MMA SR+"),
            new ServiceProvider("viewms", "MyStreams"),
            new ServiceProvider("viewss", "StarStreams"),
            new ServiceProvider("viewstvn", "StreamTVNow")
    };

    private Server[] servers = new Server[] {
            new Server("deu", "Europe Mix"),
            new Server("deu-de", "EU DE Mix"),
            new Server("deu-nl", "EU NL Mix"),
            new Server("deu-nl1", "EU NL 1 (i3d)"),
            new Server("deu-nl2", "EU NL 2 (i3d)"),
            new Server("deu-nl3", "EU NL 3 (Amsterdam)"),
            new Server("deu-nl4", "EU NL 4 (Breda)"),
            new Server("deu-nl5", "EU NL 5 (Enschede)"),
            new Server("deu-uk", "EU UK Mix"),
            new Server("deu-uk1", "EU UK 1 (io)"),
            new Server("deu-uk2", "EU UK 2 (100TB)"),
            new Server("dna", "North America Mix"),
            new Server("dnae", "NA East Mix"),
            new Server("dnae1", "NA East 1 (New Jersey)"),
            new Server("dnae2", "NA East 2 (Virginia)"),
            new Server("dnae3", "NA East 3 (Montreal)"),
            new Server("dnae4", "NA East 4 (Toronto)"),
            new Server("dnae6", "NA East 6 (New York)"),
            new Server("dnaw", "NA West Mix"),
            new Server("dnaw1", "NA West 1 (Phoenix)"),
            new Server("dnaw2", "NA West 2 (Los Angeles)"),
            new Server("dnaw3", "NA West 3 (San Jose)"),
            new Server("dnaw4", "NA West 4 (Chicago)"),
            new Server("dap", "Asia Pacific Mix")
    };

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
        rgQuality = (RadioGroup) findViewById(R.id.rg_quality);
        rgEpg = (RadioGroup) findViewById(R.id.rg_epg);
        spinnerService = (Spinner) findViewById(R.id.spinner_service);
        ArrayAdapter<ServiceProvider> serviceProviderArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, serviceProviders);
        spinnerService.setAdapter(serviceProviderArrayAdapter);
        spinnerServer = (Spinner) findViewById(R.id.spinner_server);
        ArrayAdapter<Server> serverArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, servers);
        spinnerServer.setAdapter(serverArrayAdapter);

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
        spinnerService.setSelection(mPreferences.getInt("serviceSpinnerPosition", 0));
        spinnerServer.setSelection(mPreferences.getInt("serverSpinnerPosition", 0));
        rgQuality.check(mPreferences.getInt("quality", R.id.r_hd));
        rgEpg.check(mPreferences.getInt("epg", R.id.r_full));
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
        editor.putString("username", etUsername.getText().toString().trim());
        editor.putString("password", etPassword.getText().toString().trim());
        ServiceProvider selectedServiceProvider = (ServiceProvider) spinnerService.getSelectedItem();
        editor.putString("service", selectedServiceProvider.getId());
        editor.putInt("serviceSpinnerPosition", spinnerService.getSelectedItemPosition());
        Server selectedServer = (Server) spinnerServer.getSelectedItem();
        editor.putString("server", selectedServer.getId());
        editor.putInt("serverSpinnerPosition", spinnerServer.getSelectedItemPosition());
        editor.putInt("quality", rgQuality.getCheckedRadioButtonId());
        editor.putInt("epg", rgEpg.getCheckedRadioButtonId());
        editor.commit();

        if (mIsBound) mService.loadPreferences(mPreferences);
        Toast.makeText(getApplicationContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
    }
}
