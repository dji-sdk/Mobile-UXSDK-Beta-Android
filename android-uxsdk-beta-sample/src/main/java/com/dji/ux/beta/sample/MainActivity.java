/*
 * Copyright (c) 2018-2019 DJI
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
 *
 */

package com.dji.ux.beta.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.dji.ux.beta.sample.widgetlist.WidgetsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Handles the connection to the product and provides links to the different test activities. Also
 * shows the current connection status and displays logs for the different steps of the SDK
 * registration process.
 */
public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener{

    //region Constants
    private static final String LAST_USED_BRIDGE_IP = "bridgeip";
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private static final String TIME_FORMAT = "MMM dd, yyyy 'at' h:mm:ss a";
    private static final String TAG = "MainActivity";
    //endregion

    //region Fields
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static boolean isAppStarted = false;
    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                runOnUiThread(() -> {
                    addLog("Registration succeeded");
                    addLog("Connecting to product");
                    registeredTextView.setText(R.string.registered);
                });
            } else {
                runOnUiThread(() -> addLog("Registration failed"));
            }
        }

        @Override
        public void onProductDisconnect() {
            runOnUiThread(() -> {
                addLog("Disconnected from product");
                productNameTextView.setText(R.string.no_product);
            });
        }

        @Override
        public void onProductConnect(BaseProduct product) {
            if (product != null) {
                runOnUiThread(() -> {
                    addLog("Connected to product");
                    productNameTextView.setText(getString(R.string.product_name, product.getModel().getDisplayName()));
                });
            }
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            runOnUiThread(() -> addLog(key.toString() + " changed"));

        }

        @Override
        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int totalProcess) {
            runOnUiThread(() -> addLog(djisdkInitEvent.getInitializationState().toString()));
        }
    };
    private List<String> missingPermission = new ArrayList<>();
    @BindView(R.id.text_view_version)
    protected TextView versionTextView;
    @BindView(R.id.text_view_registered)
    protected TextView registeredTextView;
    @BindView(R.id.text_view_product_name)
    protected TextView productNameTextView;
    @BindView(R.id.edit_text_bridge_ip)
    protected EditText bridgeModeEditText;
    @BindView(R.id.text_view_logs)
    protected TextView logsTextView;
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.dji_ux_sample_app_name_long);
        }
        isAppStarted = true;
        versionTextView.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));
        bridgeModeEditText.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(LAST_USED_BRIDGE_IP, ""));
        bridgeModeEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event != null && event.isShiftPressed()) {
                    return false;
                } else {
                    // the user is done typing.
                    handleBridgeIPTextChange();
                }
            }
            return false; // pass on to other listeners.
        });
        bridgeModeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().contains("\n")) {
                    // the user is done typing.
                    // remove new line characcter
                    final String currentText = bridgeModeEditText.getText().toString();
                    bridgeModeEditText.setText(currentText.substring(0, currentText.indexOf('\n')));
                    handleBridgeIPTextChange();
                }
            }
        });
        checkAndRequestPermissions();
    }

    @Override
    protected void onDestroy() {
        DJISDKManager.getInstance().destroy();
        isAppStarted = false;
        super.onDestroy();
    }
    //endregion

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            addLog("Missing permissions! Will not register SDK to connect to aircraft.");
        }
    }

    /**
     * Start the SDK registration
     */
    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            addLog("Registering product");
            AsyncTask.execute(() -> DJISDKManager.getInstance().registerApp(MainActivity.this, registrationCallback));
        }
    }

    /**
     * React to changes in the Bridge IP text view. If the text view is non-empty, attempts to
     * start a connection over the bridge.
     */
    private void handleBridgeIPTextChange() {
        // the user is done typing.
        final String bridgeIP = bridgeModeEditText.getText().toString();

        if (!TextUtils.isEmpty(bridgeIP)) {
            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(bridgeIP);
            addLog("BridgeMode ON!\nIP: " + bridgeIP);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(LAST_USED_BRIDGE_IP, bridgeIP).apply();
        }
    }

    /**
     * Adds the given text to the logs along with a timestamp of the current time.
     * @param description The line of text to add to the logs.
     */
    private void addLog(String description) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        logsTextView.append(sdf.format(Calendar.getInstance().getTime()) + " " + description + "\n");
    }

    /**
     * Whether the app has started.
     *
     * @return `true` if the app has been started.
     */
    public static boolean isStarted() {
        return isAppStarted;
    }

    /**
     * Starts the {@link WidgetsActivity}.
     */
    @OnClick(R.id.widget_button)
    public void onWidgetClick() {
        Intent intent = new Intent(this, WidgetsActivity.class);
        startActivity(intent);
    }

    /**
     * Displays a menu of map providers before launching the {@link MapWidgetActivity}. Disables
     * providers that are not supported by this device.
     *
     * @param view The view that was clicked.
     */
    @OnClick(R.id.map_button)
    public void onMapClick(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this);
        Menu popupMenu = popup.getMenu();
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.map_select_menu, popupMenu);
        popupMenu.findItem(R.id.here_map).setEnabled(isHereMapsSupported());
        popupMenu.findItem(R.id.google_map).setEnabled(isGoogleMapsSupported(this));
        popup.show();
    }

    /**
     * When one of the map providers is clicked, the {@link MapWidgetActivity} is launched with
     * the {@link dji.ux.beta.widget.map.MapWidget} initialized with the given provider.
     *
     * @param menuItem The menu item that was clicked.
     * @return `true` if the click has been consumed.
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Intent intent = new Intent(this, MapWidgetActivity.class);
        int mapBrand = 0;
        switch (menuItem.getItemId()) {
            case R.id.here_map:
                mapBrand = 0;
                break;
            case R.id.google_map:
                mapBrand = 1;
                break;
            case R.id.amap:
                mapBrand = 2;
                break;
            case R.id.mapbox:
                mapBrand = 3;
                break;
        }
        intent.putExtra(MapWidgetActivity.MAP_PROVIDER, mapBrand);
        startActivity(intent);
        return false;
    }

    /**
     * HERE Maps are supported by ARM V7 and AMR V8 devices. They are not supported by x86 or mips
     * devices.
     *
     * @return `true` if HERE Maps are supported by this device.
     */
    public static boolean isHereMapsSupported() {
        String abi;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            abi = Build.CPU_ABI;
        } else {
            abi = Build.SUPPORTED_ABIS[0];
        }
        DJILog.d(TAG, "abi=" + abi);

        //The possible values are armeabi, armeabi-v7a, arm64-v8a, x86, x86_64, mips, mips64.
        return abi.contains("arm");
    }

    /**
     * Google maps are supported only if Google Play Services are available on this device.
     *
     * @param context An instance of {@link Context}.
     * @return `true` if Google Maps are supported by this device.
     */
    public static boolean isGoogleMapsSupported(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }
}
