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

package com.dji.ux.beta.sample.development.panel;

import android.os.Bundle;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dji.ux.beta.sample.R;

import butterknife.ButterKnife;

public class PanelActivity extends AppCompatActivity {

//    @BindView(R.id.radio_group_panels)
//    protected RadioGroup panelsRadioGroup;
//    @BindView(R.id.panel_top_bar)
//    protected TopBarPanelWidget topBarPanel;
//    @BindView(R.id.panel_list_example)
//    protected SingleListPanelWidget exampleListPanel;
//    @BindView(R.id.panel_list_example_custom)
//    protected SingleListPanelWidget customExampleListPanel;
//    @BindView(R.id.panel_list__nav_example)
//    protected SampleNavigationView navigationListPanel;
//    @BindView(R.id.panel_list__nav_example_custom)
//    protected SampleNavigationView customNavigationListPanel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel);
        ButterKnife.bind(this);
//
//        panelsRadioGroup.check(R.id.radio_button_sample_list);
//        findViewById(R.id.radio_button_hor_top_bar).performClick();
//
//        SingleListPanelWidget singleListPanelWidget = findViewById(R.id.panel_list_example_custom);
//        CustomSingleListSmartListModel customTestSmartModel = new CustomSingleListSmartListModel(this);
//        singleListPanelWidget.setSmartListModel(customTestSmartModel);
//
//        customNavigationListPanel.viewAdded().subscribeOn(AndroidSchedulers.mainThread())
//                .subscribe(view -> {
//                    if (view instanceof PanelWidget) {
//                        ((PanelWidget) view).setTitleBarBackgroundColor(Color.WHITE);
//                    }
//                });
    }


    public void onPanelChecked(RadioButton radioButton) {
//        int checkedId = radioButton.getId();
//        topBarPanel.setVisibility(View.GONE);
//        exampleListPanel.setVisibility(View.GONE);
//        customExampleListPanel.setVisibility(View.GONE);
//        navigationListPanel.setVisibility((View.GONE));
//        customNavigationListPanel.setVisibility((View.GONE));
//
//        if (checkedId == R.id.radio_button_hor_top_bar) {
//            topBarPanel.setVisibility(View.VISIBLE);
//        } else if (checkedId == R.id.radio_button_sample_list) {
//            exampleListPanel.setVisibility(View.VISIBLE);
//        } else if (checkedId == R.id.radio_button_sample_list_custom) {
//            customExampleListPanel.setVisibility(View.VISIBLE);
//        } else if (checkedId == R.id.radio_button_sample_list_navigation) {
//            navigationListPanel.setVisibility((View.VISIBLE));
//        } else if (checkedId == R.id.radio_button_sample_list_navigation_custom) {
//            customNavigationListPanel.setVisibility((View.VISIBLE));
//        }
    }

}