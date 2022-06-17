/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

public abstract class BaseDialog extends Dialog {

    //region Constructors
    public BaseDialog(@NonNull Context context) {
        super(new WeakReference<>(context).get());
    }
    //endregion

    public void adjustAttrs(final int width,
                            final int height,
                            final int yOffset,
                            final int gravity,
                            final boolean cancelable,
                            final boolean cancelTouchOutside) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.width = width;
        attrs.height = height;
        attrs.y = yOffset;
        attrs.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        attrs.gravity = gravity;
        getWindow().setAttributes(attrs);
        getWindow().setWindowAnimations(R.style.uxsdk_dialogWindowAnim);
        setCancelable(cancelable);
        setCanceledOnTouchOutside(cancelTouchOutside);
    }

    //endregion
}
