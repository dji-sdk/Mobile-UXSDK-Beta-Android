/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

package dji.ux.beta.core.v4;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import dji.ux.beta.core.R;


public class SlidingDialogV4 extends BaseDialog implements View.OnClickListener{

    //region Properties
    public static final int TYPE_NOTIFY = 0;
    public static final int TYPE_INFO = 1;
    public static final int TYPE_WARNING = 2;
    public static final int TYPE_ERROR = 3;
    public static final int TYPE_TIP2 = 4; //Beginner mode
    public static final int TYPE_ALERT = 5;
    public static final int TYPE_NONE = 6;

    private ImageView imgIcon = null;
    private TextView textViewTitle = null;
    private TextView tvLittleTitle = null;
    private TextView tvDesc = null;
    private LinearLayout lyEt = null;
    private EditText editText = null;
    private TextView tvUnit = null;
    private RelativeLayout lyCb = null;
    private TextView tvCb = null;
    private SeekBar seekbarCb = null;
    private TextView tvLeftBtn = null;
    private TextView tvRightBtn = null;
    private ImageView imgBtnDivider = null;

    private SeekBar.OnSeekBarChangeListener sbChangeListener = null;
    protected static DJIUnitUtil.UnitType value_Unit_Type = DJIUnitUtil.UnitType.METRIC;
    public static void setWidgetUnitType(DJIUnitUtil.UnitType type) {
        value_Unit_Type = type;
    }

    public interface OnEventListener {
        void onLeftBtnClick(final DialogInterface dialog, final int arg);

        void onRightBtnClick(final DialogInterface dialog, final int arg);

        void onCbChecked(final DialogInterface dialog, final boolean checked, final int arg);
    }
    private OnEventListener onEventListener = null;
    private int widthId;
    //endregion

    //region Constructors
    public SlidingDialogV4(Context context) {
        this(context, R.dimen.uxsdk_sliding_dialog_width);
    }

    public SlidingDialogV4(Context context, int widthId) {
        super(context);
        this.widthId = widthId;
        init();
    }
    //endregion

    //region UI Logic
    public SlidingDialogV4 setOnEventListener(final OnEventListener listener) {
        onEventListener = listener;
        return this;
    }

    public SlidingDialogV4 setType(final int type) {
        if (TYPE_NOTIFY == type) {
            imgIcon.setBackgroundResource(R.drawable.uxsdk_leftmenu_popup_alert);
            textViewTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_green));
            tvLittleTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_green));
        } else if (TYPE_INFO == type) {
            imgIcon.setBackgroundResource(R.drawable.uxsdk_leftmenu_popup_alert);
            textViewTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_yellow_medium));
            tvLittleTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_yellow_medium));
        } else if (TYPE_WARNING == type) {
            imgIcon.setBackgroundResource(R.drawable.uxsdk_leftmenu_popup_warning);
            textViewTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_red_light));
            tvLittleTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_red_light));
        } else if (TYPE_ERROR == type) {
            imgIcon.setBackgroundResource(R.drawable.uxsdk_leftmenu_popup_warning);
            textViewTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_red_light));
            tvLittleTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_red_light));
        } else if (TYPE_TIP2 == type) {
            imgIcon.setBackgroundResource(R.drawable.uxsdk_leftmenu_popup_greencheck);
            textViewTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_green));
            tvLittleTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_green));
        } else if (TYPE_NONE == type) {
            imgIcon.setVisibility(View.GONE);
            textViewTitle.setTextColor(getContext().getResources().getColor(R.color.uxsdk_white));
            tvLittleTitle.setVisibility(View.GONE);
        }
        return this;
    }

    public SlidingDialogV4 setTopIcon(final int resId) {
        imgIcon.setBackgroundResource(resId);
        return this;
    }
    
    public SlidingDialogV4 setTitleStr(final String title) {
        textViewTitle.setText(title);
        return this;
    }

    public SlidingDialogV4 setLittleTitleStr(final int visibility, final String txt) {
        tvLittleTitle.setVisibility(visibility);
        tvLittleTitle.setText(txt);
        return this;
    }

    public SlidingDialogV4 setDesc(final String desc) {
        tvDesc.setText(desc);
        return this;
    }

    public SlidingDialogV4 setLeftBtnVisibility(final int visibility) {
        tvLeftBtn.setVisibility(visibility);
        imgBtnDivider.setVisibility(visibility);
        return this;
    }

    public SlidingDialogV4 setRightBtnVisibility(final int visibility) {
        tvRightBtn.setVisibility(visibility);
        imgBtnDivider.setVisibility(visibility);
        return this;
    }

    public SlidingDialogV4 setEtVisibility(final int visibility, int value) {
        lyEt.setVisibility(visibility);

        if (value_Unit_Type == DJIUnitUtil.UnitType.IMPERIAL) {
            tvUnit.setText(R.string.uxsdk_setting_foot);
            value = (int) DJIUnitUtil.imperialToMetricByLength(value);
        } else if (value_Unit_Type == DJIUnitUtil.UnitType.METRIC) {
            tvUnit.setText(R.string.uxsdk_setting_metric);
        }
        editText.setText(String.valueOf(value));
        return this;
    }

    public SlidingDialogV4 setCbTxt(final String cbTxt) {
        tvCb.setText(cbTxt);
        return this;
    }

    public SlidingDialogV4 setCbVisibility(final int visibility) {
        seekbarCb.setProgress(0);
        lyCb.setVisibility(visibility);
        return this;
    }
    
    private void initMember() {
        sbChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handleSbStopTrack();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }
        };
    }

    @Override
    public void show() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                             WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        super.show();
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initMember();
        setContentView(R.layout.uxsdk_widget_sliding_dlg);



        imgIcon = (ImageView) findViewById(R.id.imageview_dlg_title_icon);
        textViewTitle = (TextView) findViewById(R.id.textview_dlg_title);
        tvLittleTitle = (TextView) findViewById(R.id.textview_dlg_little_title);
        tvDesc = (TextView) findViewById(R.id.textview_dlg_desc);

        lyEt = (LinearLayout) findViewById(R.id.linearlayout_dlg_edit_text);
        editText = (EditText) findViewById(R.id.edittext_value);
        tvUnit = (TextView) findViewById(R.id.textview_value_unit);

        lyCb = (RelativeLayout) findViewById(R.id.relativelayout_slidebar);
        tvCb = (TextView) findViewById(R.id.textview_slidertitle);
        seekbarCb = (SeekBar) findViewById(R.id.seekbar_slider);

        tvLeftBtn = (TextView) findViewById(R.id.textview_button_cancel);
        tvRightBtn = (TextView) findViewById(R.id.textview_button_ok);
        imgBtnDivider = (ImageView) findViewById(R.id.imageview_divider);

        tvLeftBtn.setOnClickListener(this);
        tvRightBtn.setOnClickListener(this);

        seekbarCb.setOnSeekBarChangeListener(sbChangeListener);

        seekbarCb.setPadding(0, 0, 0, 0);
    }

    private void handleSbStopTrack() {
        final int progress = seekbarCb.getProgress();
        if (progress >= 95) {
            seekbarCb.setProgress(100);
            cbCbChecked(true);
        } else {
            seekbarCb.setProgress(0);
            cbCbChecked(false);
        }
    }

    private void cbCbChecked(final boolean checked) {
        if (null != onEventListener) {
            onEventListener.onCbChecked(this, checked, 0);
        }
    }

    private void cbLeftBtnClick() {
        if (null != onEventListener) {
            onEventListener.onLeftBtnClick(this, 0);
        }
    }

    private void cbRightBtnClick() {
        if (null != onEventListener) {
            onEventListener.onRightBtnClick(this, 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        adjustAttrs((int)getContext().getResources().getDimension(widthId), WindowManager.LayoutParams.WRAP_CONTENT, 0, Gravity.CENTER,
                true, false);
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.textview_button_cancel) {
            cbLeftBtnClick();
        } else if (id == R.id.textview_button_ok) {
            cbRightBtnClick();
        }
    }

    public void setButtonStyleYesNo(){
        tvLeftBtn.setText(R.string.uxsdk_app_no);
        tvRightBtn.setText(R.string.uxsdk_app_yes);
    }

    //endregion

}
