package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleableRes;
import androidx.core.graphics.drawable.DrawableCompat;
import dji.ux.beta.core.R;

import static android.view.View.GONE;

public class ViewUtils {

    /**
     * Is clickable which will not transfer the user click event to background view.
     */
    public static void setClickableView(@NonNull View view) {
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    /**
     * Show the dialog with Yes and No buttons and custom title.
     */
    public static SlidingDialogV4 showOperateDlg(Context context, final String title, final String content,
                                               SlidingDialogV4.OnEventListener listener) {
        SlidingDialogV4 tipDlg = new SlidingDialogV4(context);
        tipDlg.setType(SlidingDialogV4.TYPE_INFO);
        tipDlg.setOnEventListener(listener);
        tipDlg.setEtVisibility(GONE, 0).setCbVisibility(GONE);
        tipDlg.setLittleTitleStr(GONE, "");
        tipDlg.setTitleStr(title);
        tipDlg.setDesc(content);
        tipDlg.setButtonStyleYesNo();
        tipDlg.show();
        return tipDlg;
    }

    /**
     * Show the dialog with Yes and No buttons.
     */
    public static SlidingDialogV4 showOperateDlg(Context context, final String content, SlidingDialogV4.OnEventListener listener) {
        return showOperateDlg(context, context.getString(R.string.uxsdk_app_tip), content, listener);
    }

    /**
     * Show the dialog just with a Cancel button and show the warning info.
     */
    public static void showAlertDialog(Context context, @StringRes int title, String content) {
        showMessageDialog(context, SlidingDialogV4.TYPE_INFO, title, content);
    }

    public static void showMessageDialog(Context context, int type, @StringRes int title, String content) {
        showMessageDialog(context, type, context.getString(title), content);
    }

    public static void showMessageDialog(Context context, int type, String title, String content) {
        SlidingDialogV4 tipDlg = new SlidingDialogV4(context);
        tipDlg.setType(type);
        tipDlg.setOnEventListener(new SlidingDialogV4.OnEventListener() {

            @Override
            public void onRightBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
            }

            @Override
            public void onLeftBtnClick(final DialogInterface dialog, int arg) {
                dialog.dismiss();
            }

            @Override
            public void onCbChecked(final DialogInterface dialog, boolean checked, int arg) {

            }
        });
        tipDlg.setEtVisibility(GONE, 0).setCbVisibility(GONE);
        tipDlg.setLittleTitleStr(GONE, "");
        tipDlg.setTitleStr(context.getString(R.string.uxsdk_app_tip));
        tipDlg.setDesc(title + ". " + content);
        if (type == SlidingDialogV4.TYPE_TIP2) {
            tipDlg.setLeftBtnVisibility(GONE);
        } else {
            tipDlg.setRightBtnVisibility(GONE);
        }
        tipDlg.show();
    }

    /**
     * Set visibility based on boolean
     */
    public static void setVisibility(@NonNull View view, boolean visible) {
        if (visible) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Get int values of the custom attribute
     */
    public static int parseIntAttribute(Context context,
                                        AttributeSet attrs,
                                        @StyleableRes int[] styleRes,
                                        int index,
                                        int defaultValue) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, styleRes, 0, 0);

        int excluded = defaultValue;
        try {
            // Retrieve the values from the TypedArray and store into
            // fields of this class.
            excluded = a.getInteger(index, 0);
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }

        return excluded;
    }


    /**
     * Get value of the boolean type custom attribute.
     */
    public static boolean parseBooleanAttribute(Context context,
                                            AttributeSet attrs,
                                            @StyleableRes int[] styleRes,
                                            int index,
                                            boolean defaultValue) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, styleRes, 0, 0);

        boolean value = defaultValue;
        try {
            // Retrieve the values from the TypedArray and store into
            // fields of this class.
            value = a.getBoolean(index, defaultValue);
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }

        return value;
    }

    public static void tintImage(ImageView imageView, @ColorRes int colorId) {
        if (imageView == null || imageView.getDrawable() == null) {
            return;
        }
        Drawable wrapDrawable = DrawableCompat.wrap(imageView.getDrawable());
        DrawableCompat.setTintList(wrapDrawable, imageView.getResources().getColorStateList(colorId));
        DrawableCompat.setTintMode(wrapDrawable, PorterDuff.Mode.SRC_IN);
        imageView.setImageDrawable(wrapDrawable);
    }
}
