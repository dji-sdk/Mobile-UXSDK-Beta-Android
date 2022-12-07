package dji.ux.beta.core.util;

import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

public class PopUtils {

    private static PopupWindow popupWindow;

    /**
     * 弹出对应的pp
     *
     * @param view 当前的view
     * @return 返回当前的view
     */
    public static void getPp(View view, View referenceView) {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        popupWindow = new PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setContentView(view);
        popupWindow.showAsDropDown(referenceView);
    }

    public static void closePP() {
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }
}