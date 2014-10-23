/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * (C) Spencer Newsom 2014 Elon University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 */

package edu.elon.tds;

import net.clc.bt.R;
import net.clc.bt.R.id;
import net.clc.bt.R.layout;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * A simple configuration screen that displays the user's current Bluetooth
 * information along with buttons for entering the Bluetooth settings menu and
 * for starting a demo app. This is work in progress and only the demo app
 * buttons are currently available.
 */

public class ConfigActivity extends Activity {

    private ConfigActivity self;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self = this;
        setContentView(R.layout.config);

        Button startTacticalServer = (Button) findViewById(R.id.start_tactical_dance_server);
        startTacticalServer.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Intent serverIntent = new Intent(self, Demo_Multiscreen.class);
                Intent serverIntent = new Intent(self, TacticalDance.class);
                serverIntent.putExtra("TYPE", 0);
                startActivity(serverIntent);
            }
        });

        Button startTacticalDanceClient = (Button) findViewById(R.id.start_tactical_dance_client);
        startTacticalDanceClient.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Intent clientIntent = new Intent(self, Demo_Multiscreen.class);
                Intent clientIntent = new Intent(self, TacticalDance.class);
                clientIntent.putExtra("TYPE", 1);
                startActivity(clientIntent);
            }
        });


    }

}
