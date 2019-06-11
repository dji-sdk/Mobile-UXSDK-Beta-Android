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
 */

package dji.ux.beta.widget.simulator.preset;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import dji.ux.beta.R;
import dji.ux.beta.util.SimulatorPresetUtils;

public class PresetListDialog extends Dialog implements View.OnClickListener {

    //region fields
    private LinearLayout presetListContainerLinearLayout;
    private TextView emptyPresetListTextView;
    private TextView confirmDeleteTextView;
    private TextView cancelDialogTextView;
    private TextView deletePresetYesTextView;
    private TextView deletePresetNoTextView;
    private Context context;
    private OnLoadPresetListener loadPresetListener;
    private String keyToDelete;

    //endregion

    //region lifecycle
    public PresetListDialog(@NonNull Context context, @NonNull OnLoadPresetListener loadPresetListener) {
        super(context);
        this.context = context;
        setCancelable(true);
        this.loadPresetListener = loadPresetListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_dialog_simulator_load_preset);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(R.drawable.uxsdk_background_dialog_rounded_corners);
        }
        presetListContainerLinearLayout = findViewById(R.id.linear_layout_preset_list_container);
        emptyPresetListTextView = findViewById(R.id.textview_empty_list);
        confirmDeleteTextView = findViewById(R.id.textview_confirmation_delete);
        cancelDialogTextView = findViewById(R.id.textview_cancel_simulator_dialog);
        deletePresetYesTextView = findViewById(R.id.textview_delete_yes);
        deletePresetNoTextView = findViewById(R.id.textview_delete_no);
        emptyPresetListTextView = findViewById(R.id.textview_empty_list);

        cancelDialogTextView.setOnClickListener(this);
        deletePresetYesTextView.setOnClickListener(this);
        deletePresetNoTextView.setOnClickListener(this);

        resetListUI();
    }

    @Override
    public void onClick(View v) {
        if (v instanceof TextView) {
            if (v.getId() == R.id.textview_cancel_simulator_dialog) {
                dismiss();
            } else if (v.getId() == R.id.textview_delete_yes) {
                deletePreset();
            } else if (v.getId() == R.id.textview_delete_no) {
                resetListUI();
            } else {
                sendPresetEvent((String) v.getTag());
            }
        } else {
            showDeleteConfirmation((String) v.getTag());
        }
    }
    //endregion

    //region private methods
    private void populatePresetList() {
        presetListContainerLinearLayout.removeAllViews();
        Map<String, ?> simulatorPresetList = SimulatorPresetUtils.getInstance().getPresetList();
        if (simulatorPresetList != null && !simulatorPresetList.isEmpty()) {
            emptyPresetListTextView.setVisibility(View.GONE);
            presetListContainerLinearLayout.setVisibility(View.VISIBLE);
            for (Map.Entry<String, ?> entry : simulatorPresetList.entrySet()) {
                insertView(entry.getKey(), (String) entry.getValue());
            }
        } else {
            emptyPresetListTextView.setVisibility(View.VISIBLE);
            presetListContainerLinearLayout.setVisibility(View.GONE);
        }
        getWindow().setLayout((int) context.getResources().getDimension(R.dimen.uxsdk_simulator_dialog_width),
                              ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void resetListUI() {
        keyToDelete = null;
        confirmDeleteTextView.setVisibility(View.GONE);
        deletePresetYesTextView.setVisibility(View.GONE);
        presetListContainerLinearLayout.setVisibility(View.VISIBLE);
        deletePresetNoTextView.setVisibility(View.GONE);
        cancelDialogTextView.setVisibility(View.VISIBLE);
        populatePresetList();
    }

    private void deletePreset() {
        SimulatorPresetUtils.getInstance().deletePreset(keyToDelete);
        resetListUI();
    }

    private void showDeleteConfirmation(String key) {
        keyToDelete = key;
        confirmDeleteTextView.setVisibility(View.VISIBLE);
        deletePresetYesTextView.setVisibility(View.VISIBLE);
        presetListContainerLinearLayout.setVisibility(View.GONE);
        deletePresetNoTextView.setVisibility(View.VISIBLE);
        cancelDialogTextView.setVisibility(View.GONE);
        confirmDeleteTextView.setText(context.getResources()
                                             .getString(R.string.uxsdk_simulator_save_preset_delete, key));
        if (getWindow() != null) {
            getWindow().setLayout((int) context.getResources().getDimension(R.dimen.uxsdk_simulator_dialog_width),
                                  ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void sendPresetEvent(String data) {
        if (data != null) {
            String[] dataParts = data.split(" ");
            double latitude = Double.parseDouble(dataParts[0]);
            double longitude = Double.parseDouble(dataParts[1]);
            int satelliteCount = Integer.parseInt(dataParts[2]);
            int frequency = Integer.parseInt(dataParts[3]);

            if (loadPresetListener != null) {
                loadPresetListener.onLoadPreset(new SimulatorPresetData(latitude,
                                                                        longitude,
                                                                        satelliteCount,
                                                                        frequency));
            }
        } else {
            Toast.makeText(context,
                           context.getResources().getString(R.string.uxsdk_simulator_preset_error),
                           Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }

    private void insertView(String key, String value) {
        View presetRow = LayoutInflater.from(context).inflate(R.layout.uxsdk_layout_simulator_preset_row, null);
        presetListContainerLinearLayout.addView(presetRow, presetListContainerLinearLayout.getChildCount());
        TextView presetNameTextView = presetRow.findViewById(R.id.textview_preset_name);
        presetNameTextView.setText(key);
        presetNameTextView.setTag(value);
        presetNameTextView.setOnClickListener(this);
        ImageView deleteImage = presetRow.findViewById(R.id.imageview_preset_delete);
        deleteImage.setTag(key);
        deleteImage.setOnClickListener(this);
    }
    //endregion
}
