/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.dji.ux.beta.sample;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.multidex.MultiDex;

import com.secneo.sdk.Helper;

import dji.ux.beta.core.communication.DefaultGlobalPreferences;
import dji.ux.beta.core.communication.GlobalPreferencesManager;

import static com.dji.ux.beta.sample.DJIConnectionControlActivity.ACCESSORY_ATTACHED;

/**
 * An application that loads the SDK classes.
 */
public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //For the global preferences to take effect, this must be done before the widgets are initialized
        //If this is not done, no global preferences will take effect or persist across app restarts
        GlobalPreferencesManager.initialize(new DefaultGlobalPreferences(this));

        BroadcastReceiver br = new OnDJIUSBAttachedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACCESSORY_ATTACHED);
        registerReceiver(br, filter);
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(SampleApplication.this);
        MultiDex.install(this);
    }

}
