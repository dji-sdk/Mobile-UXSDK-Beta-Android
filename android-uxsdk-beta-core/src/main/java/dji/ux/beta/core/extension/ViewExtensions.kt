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
 */

@file:JvmName("ViewExtensions")

package dji.ux.beta.core.extension

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import dji.ux.beta.R

/**
 * Get the [String] for the given [stringRes].
 */
fun View.getString(@StringRes stringRes: Int): String = context.resources.getString(stringRes)

/**
 * Get the [Drawable] for the given [drawableRes].
 */
fun View.getDrawable(@DrawableRes drawableRes: Int): Drawable = context.resources.getDrawable(drawableRes)

/**
 * The the color int for the given [colorRes].
 */
@ColorInt
fun View.getColor(@ColorRes colorRes: Int): Int = context.resources.getColor(colorRes)

/**
 * The the px size for the given [dimenRes].
 */
@Px
fun View.getDimension(@DimenRes dimenRes: Int): Float = context.resources.getDimension(dimenRes)

/**
 * Set the view [View.VISIBLE].
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Set the view [View.GONE].
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Toggle the view between [View.GONE] and [View.VISIBLE]
 */
fun View.toggleVisibility() {
    if (visibility == View.VISIBLE) hide()
    else show()
}

/**
 * Show a short length toast with the given [messageResId].
 */
fun View.showShortToast(@StringRes messageResId: Int) {
    Toast.makeText(
                    context,
                    messageResId,
                    Toast.LENGTH_SHORT)
            .show()
}

/**
 * Show a long length toast with the given [messageResId].
 */
fun View.showLongToast(@StringRes messageResId: Int) {
    Toast.makeText(
                    context,
                    messageResId,
                    Toast.LENGTH_LONG)
            .show()
}

/**
 * Show a short length toast with the given [String].
 */
fun View.showShortToast(message: String?) {
    Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT)
            .show()
}

/**
 * Show a long length toast with the given [String].
 */
fun View.showLongToast(message: String?) {
    Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_LONG)
            .show()
}

/**
 * The [TextView]'s text color int.
 */
var TextView.textColor: Int
    @ColorInt
    get() = this.currentTextColor
    set(@ColorInt value) {
        this.setTextColor(value)
    }

/**
 * The [TextView]'s text color state list.
 */
var TextView.textColorStateList: ColorStateList?
    get() = this.textColors
    set(value) {
        this.setTextColor(value)
    }

/**
 * The [ImageView]'s drawable.
 */
var ImageView.imageDrawable: Drawable?
    get() = this.drawable
    set(value) {
        this.setImageDrawable(value)
    }

/**
 * Show an alert dialog.
 * @param dialogTheme The style of the dialog
 * @param isCancellable Dismiss the dialog when touch outside its bounds
 * @param title Dialog title text
 * @param icon  Dialog title icon
 * @param message Dialog message
 * @param dismissButton Dismiss button text
 * @param dialogClickListener
 */
fun View.showAlertDialog(@StyleRes dialogTheme: Int = R.style.Theme_AppCompat_Dialog,
                         isCancellable: Boolean = true,
                         title: String? = getString(R.string.uxsdk_alert),
                         icon: Drawable? = null,
                         message: String? = null,
                         dismissButton: String? = getString(R.string.uxsdk_app_ok),
                         dialogClickListener: DialogInterface.OnClickListener? = null) {
    val builder = AlertDialog.Builder(context, dialogTheme)
    builder.setTitle(title)
    builder.setIcon(icon)
    builder.setCancelable(isCancellable)
    builder.setMessage(message)
    builder.setPositiveButton(dismissButton, dialogClickListener)
    val dialog = builder.create()
    dialog.show()
}

/**
 * Show an alert dialog.
 * @param dialogTheme The style of the dialog
 * @param isCancellable Dismiss the dialog when touch outside its bounds
 * @param title Dialog title text
 * @param icon  Dialog title icon
 * @param message Dialog message
 * @param positiveButton Positive button text
 * @param negativeButton Negative button text
 * @param dialogClickListener
 */
fun View.showConfirmationDialog(@StyleRes dialogTheme: Int = R.style.Theme_AppCompat_Dialog,
                                isCancellable: Boolean = true,
                                title: String? = getString(R.string.uxsdk_alert),
                                icon: Drawable? = null,
                                message: String? = null,
                                positiveButton: String? = getString(R.string.uxsdk_app_ok),
                                negativeButton: String? = getString(R.string.uxsdk_app_cancel),
                                dialogClickListener: DialogInterface.OnClickListener? = null) {
    val builder = AlertDialog.Builder(context, dialogTheme)
    builder.setIcon(icon)
    builder.setTitle(title)
    builder.setCancelable(isCancellable)
    builder.setMessage(message)
    builder.setPositiveButton(positiveButton, dialogClickListener)
    builder.setNegativeButton(negativeButton, dialogClickListener)
    val dialog = builder.create()
    dialog.show()
}

