/*
 * Copyright (c) 2018-2021 DJI
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

package com.dji.ux.beta.sample.view

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import com.dji.ux.beta.sample.R
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.OnColorSelectedListener
import dji.ux.beta.core.extension.imageDrawable

class ColorPickerTileView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var tileColor: Int = 0
        set(color) {
            field = color
            setImageDrawable(ColorTileDrawable(context, color))
        }

    var onColorSelectedListener: OnColorSelectedListener? = null

    init {
        setOnClickListener { onColorClick() }
    }

    private fun onColorClick() {
        var selectedColor = tileColor
        val colorPickerDialogView = View.inflate(context, R.layout.dialog_color_picker, null)
        val colorPickerView = colorPickerDialogView.findViewById<ColorPickerView>(R.id.color_picker_view)
        colorPickerView.setInitialColor(tileColor, true)
        colorPickerView.addOnColorChangedListener { color -> selectedColor = color }

        AlertDialog.Builder(context, dji.ux.beta.core.R.style.Theme_AppCompat_Dialog)
                .setCancelable(true)
                .setPositiveButton(R.string.uxsdk_app_ok) { dialogInterface: DialogInterface, _: Int ->
                    onColorSelectedListener?.onColorSelected(selectedColor)
                    (imageDrawable as ColorTileDrawable).color = selectedColor
                    dialogInterface.dismiss()
                }
                .setNegativeButton(R.string.uxsdk_app_cancel) { _: DialogInterface, _: Int -> }
                .setView(colorPickerDialogView)
                .create()
                .show()
    }
}