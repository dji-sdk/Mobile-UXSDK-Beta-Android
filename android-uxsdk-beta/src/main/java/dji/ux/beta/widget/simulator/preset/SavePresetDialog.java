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
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.ux.beta.R;
import dji.ux.beta.util.SimulatorPresetUtils;
import dji.ux.beta.widget.simulator.SimulatorControlWidget;

/**
 * Save Preset Dialog
 * <p>
 * The values entered in {@link SimulatorControlWidget} can be saved for future simulation as preset.
 * This dialog provides a user interface to enter the name to be used for saving the preset.
 */
public class SavePresetDialog extends Dialog implements View.OnClickListener {

    //region fields
    private SimulatorPresetData simulatorPresetData;
    private TextView titleTextView;
    private TextView saveTextView;
    private TextView cancelTextView;
    private EditText presetEditText;
    private Context context;
    //endregion

    //region lifecycle
    public SavePresetDialog(@NonNull Context context,
                            boolean cancelable,
                            @NonNull SimulatorPresetData simulatorPresetData) {
        super(context);
        setCancelable(cancelable);
        this.simulatorPresetData = simulatorPresetData;
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_dialog_simulator_save_preset);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(R.drawable.uxsdk_background_dialog_rounded_corners);
            getWindow().setLayout((int) context.getResources().getDimension(R.dimen.uxsdk_simulator_dialog_width),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        titleTextView = findViewById(R.id.textview_save_preset_header);
        presetEditText = findViewById(R.id.edit_text_preset_name);
        saveTextView = findViewById(R.id.textview_save_preset);
        saveTextView.setOnClickListener(this);
        cancelTextView = findViewById(R.id.textview_cancel_simulator_dialog);
        cancelTextView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.textview_save_preset) {
            savePreset(presetEditText.getText().toString());
        } else if (id == R.id.textview_cancel_simulator_dialog) {
            dismiss();
        }
    }

    private void savePreset(String name) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context,
                    context.getResources().getString(R.string.uxsdk_simulator_preset_name_empty_error),
                    Toast.LENGTH_SHORT).show();
        } else {
            SimulatorPresetUtils.getInstance().savePreset(name, simulatorPresetData);
            dismiss();
        }
    }

    //endregion

    //region customizations


    /**
     * Set text color state list to the widget title
     *
     * @param colorStateList to be used
     */
    public void setTitleTextColor(@Nullable ColorStateList colorStateList) {
        titleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the color of title text
     *
     * @param color integer value
     */
    public void setTitleTextColor(@ColorInt int color) {
        titleTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of widget title
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return titleTextView.getTextColors();
    }

    @ColorInt
    public int getTitleTextColor() {
        return titleTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the widget title
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        titleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set background to title text
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(context.getResources().getDrawable(resourceId));
    }

    /**
     * Set background to title text
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        titleTextView.setBackground(drawable);
    }

    /**
     * Get current background of title text
     */
    public Drawable getTitleBackground() {
        return titleTextView.getBackground();
    }

    /**
     * Set background to button text
     */
    public void setButtonBackground(@DrawableRes int resourceId) {
        setButtonBackground(context.getResources().getDrawable(resourceId));
    }

    /**
     * Set background to button text
     */
    public void setButtonBackground(@Nullable Drawable drawable) {
        saveTextView.setBackground(drawable);
        cancelTextView.setBackground(drawable);
    }

    /**
     * Get current background of button text
     */
    public Drawable getButtonBackground() {
        return cancelTextView.getBackground();
    }

    /**
     * Set text color state list to the button
     */
    public void setButtonTextColor(@Nullable ColorStateList colorStateList) {
        saveTextView.setTextColor(colorStateList);
        cancelTextView.setTextColor(colorStateList);
    }

    public void setButtonTextColor(@ColorInt int color) {
        saveTextView.setTextColor(color);
        cancelTextView.setTextColor(color);
    }

    /**
     * Get current text color state list of button
     */
    @Nullable
    public ColorStateList getButtonTextColors() {
        return cancelTextView.getTextColors();
    }

    /**
     * Get the current text color of the button
     *
     * @return integer color value
     */
    @ColorInt
    public int getButtonTextColor() {
        return cancelTextView.getCurrentTextColor();
    }

    /**
     * Set text appearance of the button
     */
    public void setButtonTextAppearance(@StyleRes int textAppearance) {
        saveTextView.setTextAppearance(getContext(), textAppearance);
        cancelTextView.setTextAppearance(getContext(), textAppearance);
    }
    //endregion
}
