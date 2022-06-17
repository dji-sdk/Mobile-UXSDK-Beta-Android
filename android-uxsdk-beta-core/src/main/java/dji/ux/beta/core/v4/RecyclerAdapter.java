package dji.ux.beta.core.v4;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import dji.ux.beta.core.R;


/**
 * Adapter based on RecyclerAdapter for ListItem
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    //region Properties
    private static final String TAG = "RecyclerAdapter";
    private static final int INVALID_INDEX = -1;
    private ChangeListener changeListener;
    private ArrayList<ListItem> dataList;
    private int currentSelectedPosition = INVALID_INDEX;
    private boolean disableItemSelection;
    private KeyboardActionListener keyboardActionListener;
    private OnSeekBarChangeListener seekBarChangeListener;
    private OnEditTextFocusListener edittextFocusListener;
    //endregion

    /**
     * Interface to notify that an item in the adapter has changed
     */
    public interface ChangeListener {
        void updateSelectedItem(ListItem item, View stateView);
    }

    public interface KeyboardActionListener {
        boolean handleKeyboardAction(View stateView, int position, int actionId, KeyEvent keyEvent);
    }

    public interface OnSeekBarChangeListener {
        void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser, int position);

        void onStartTrackingTouch(SeekBar seekBar, int progress, int position);

        void onStopTrackingTouch(SeekBar seekBar, int progress, int position);
    }

    public interface OnEditTextFocusListener {
        void onEditTextFocus(EditText editText, int position);
    }

    public void setDisableItemSelection(boolean disableItemSelection) {
        this.disableItemSelection = disableItemSelection;
    }

    //region Adapter constructor
    public RecyclerAdapter(@NonNull ChangeListener listener) {
        this(listener, null);
    }

    public RecyclerAdapter(@NonNull ChangeListener listener, @Nullable KeyboardActionListener keyboardActionListener) {
        super();
        dataList = new ArrayList<>();
        changeListener = listener;
        this.keyboardActionListener = keyboardActionListener;
    }

    public void clear() {
        if (dataList != null) {
            dataList.clear();
        }
    }

    // TODO: Avoid calling notifyItemChanged
    public void onItemClick(int position) {
        if (position != INVALID_INDEX) {

            int oldSelectedPosition = currentSelectedPosition;

            if (position != currentSelectedPosition) {
                currentSelectedPosition = position;

                if (oldSelectedPosition != INVALID_INDEX && oldSelectedPosition < dataList.size()) {
                    ListItem item = dataList.get(oldSelectedPosition);
                    item.setSelected(false);
                    notifyItemChanged(oldSelectedPosition);
                }

                ListItem item = dataList.get(position);
                item.setSelected(true);
                notifyItemChanged(position);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseFrameLayout convertView =
                ListItemViewFactory.createListItemView(parent.getContext(), ListItem.ItemType.find(viewType));
        convertView.setBackgroundResource(R.drawable.uxsdk_selector_list_item);

        ViewHolder viewHolder = new ViewHolder(convertView, this);
        convertView.setTag(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListItem item = dataList.get(position);
        holder.draw(item, disableItemSelection);
    }
    //endregion

    @Override
    public int getItemViewType(int position) {
        if (position < dataList.size()) {
            // TODO : why needed?
            ListItem item = getItemByPos(position);
            if (item.itemType != null) {
                return item.itemType.value;
            }
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void add(ListItem item) {
        dataList.add(item);
        notifyItemInserted(dataList.size());
    }

    public void setAllItemsEnabled(boolean enabled) {
        if (dataList != null) {
            for (ListItem it : dataList) {
                it.setEnabled(enabled);
            }
        }
        notifyDataSetChanged();
    }

    public int findIndexByItem(ListItem item) {
        return dataList.indexOf(item);
    }

    public int findIndexByValueID(int valueId) {
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).valueId == valueId) {
                return i;
            }
        }
        return INVALID_INDEX;
    }

    public ListItem getItemByPos(int position) {
        return dataList.get(position);
    }

    public KeyboardActionListener getKeyboardActionListener() {
        return keyboardActionListener;
    }

    public void setKeyboardActionListener(KeyboardActionListener keyboardActionListener) {
        this.keyboardActionListener = keyboardActionListener;
    }

    public OnSeekBarChangeListener getSeekBarChangeListener() {
        return seekBarChangeListener;
    }

    public void setSeekBarChangeListener(OnSeekBarChangeListener seekBarChangeListener) {
        this.seekBarChangeListener = seekBarChangeListener;
    }

    public OnEditTextFocusListener getEdittextFocusListener() {
        return edittextFocusListener;
    }

    public void setEdittextFocusListener(OnEditTextFocusListener edittextFocusListener) {
        this.edittextFocusListener = edittextFocusListener;
    }

    //region Holder class
    static final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SwitchButton.OnCheckedChangeListener,
            TextWatcher, EditText.OnEditorActionListener, SegmentedView.OnSegmentSelectedListener, SeekBar.OnSeekBarChangeListener

    {
        private ListItem model;
        private ImageView titleImg;
        private TextView textViewTitle;
        private SwitchButton valueSwitchButton;
        private ImageView valueImg;
        private TextView textViewValue;
        private ImageView imgArrow;
        private RecyclerAdapter recyclerAdapter;
        private Button actionButton;
        private EditText editTextValue;
        private EditText editText;
        private Button fullActionButton;
        private TextView rangeText;
        private TextView rangeUnitText;
        private TextView bigRangeText;
        private SegmentedView segmentedView;
        private SeekBar seekBar;
        private CheckBox checkBox;

        ViewHolder(View itemView, RecyclerAdapter adapter) {
            super(itemView);

            titleImg = (ImageView) itemView.findViewById(R.id.list_item_title_icon);
            textViewTitle = (TextView) itemView.findViewById(R.id.list_item_title);
            valueImg = (ImageView) itemView.findViewById(R.id.list_item_value_icon);
            valueSwitchButton = (SwitchButton) itemView.findViewById(R.id.list_item_value_switch_button);
            checkBox = (CheckBox)itemView.findViewById(R.id.list_item_checkbox);
            textViewValue = (TextView) itemView.findViewById(R.id.list_item_value);
            imgArrow = (ImageView) itemView.findViewById(R.id.list_item_arrow);
            actionButton = (Button) itemView.findViewById(R.id.action_button);
            editText = (EditText) itemView.findViewById(R.id.list_item_edittext);
            editTextValue = (EditText) itemView.findViewById(R.id.list_item_value_editable);
            fullActionButton = (Button) itemView.findViewById(R.id.list_item_full_button);
            rangeText = (TextView) itemView.findViewById(R.id.list_item_range);
            rangeUnitText = (TextView) itemView.findViewById(R.id.list_item_unit);
            bigRangeText = (TextView) itemView.findViewById(R.id.list_item_range_big);
            segmentedView = (SegmentedView) itemView.findViewById(R.id.list_item_segmented);
            seekBar = (SeekBar) itemView.findViewById(R.id.list_item_seek_bar);
            recyclerAdapter = adapter;
        }

        private void draw(final ListItem model, boolean disableItemSelection) {
            if (model == null) {
                return;
            }
            this.model = model;
            setListeners(disableItemSelection);

            updateTitleImage(model);
            updateTitleText(model);
            updateValueImage(model);
            updateValueTextView(model);
            updateValueEditTextView(model);

            updateArrowImage(model);

            updateSwitchButtonState(model);
            updateActionButtonState(model);
            updateFullActionButtonState(model);

            updateEditText(model);
            updateSeekBar(model);
            updateRangeValue(model);
            updateSectionType(model);
            updateTips(model);
            updateSegmentedView(model);
            updateCheckBox(model);

            itemView.setEnabled(model.isEnabled());
            itemView.setSelected(model.getSelected());
            itemView.setVisibility(model.isVisible() ? View.VISIBLE : View.GONE);
            if (model.isVisible()) {
                itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            }
        }

        private void updateCheckBox(ListItem model) {
            if (model.itemType == ListItem.ItemType.CHECK_BOX_TYPE) {
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(model.isChecked());
                checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (checkBox == compoundButton) {
                        onViewClicked(checkBox);
                    }
                });

                if (model.isEnabled()) {
                    checkBox.setEnabled(true);
                    checkBox.setAlpha(1.0f);
                    textViewTitle.setAlpha(1.0f);
                } else {
                    checkBox.setEnabled(false);
                    checkBox.setAlpha(0.5f);
                    textViewTitle.setAlpha(0.5f);
                }
            }
        }
        private void updateTitleImage(ListItem model) {
            if (titleImg != null) {
                titleImg.setEnabled(model.isEnabled());
                if (model.titleImgResId == 0) {
                    titleImg.setVisibility(View.GONE);
                } else {
                    titleImg.setVisibility(View.VISIBLE);
                    titleImg.setImageResource(model.titleImgResId);
                    ViewUtils.tintImage(titleImg, R.color.uxsdk_camera_settings_text_color);
                }
            }
            if (editText != null) {
                editText.addTextChangedListener(this);
                editText.setOnEditorActionListener(this);
            }

            if (segmentedView != null) {
                segmentedView.setOnSegmentSelectedListener(this);
            }
        }

        private void updateTitleText(ListItem model) {
            if (textViewTitle != null) {
                textViewTitle.setEnabled(model.isEnabled());
                if (model.getTitle().isEmpty()) {
                    textViewTitle.setVisibility(View.GONE);
                } else {
                    textViewTitle.setVisibility(View.VISIBLE);
                    textViewTitle.setText(model.getTitle());
                }
            }
        }

        private void updateValueImage(ListItem model) {
            if (valueImg != null) {
                valueImg.setEnabled(model.isEnabled());
                if (model.valueImgResId == 0) {
                    valueImg.setVisibility(View.GONE);
                } else {
                    valueImg.setVisibility(View.VISIBLE);
                    valueImg.setImageResource(model.valueImgResId);
                    ViewUtils.tintImage(valueImg, R.color.uxsdk_camera_settings_text_color);
                }
            }
        }


        private void updateValueTextView(ListItem model) {
            if (textViewValue != null) {
                textViewValue.setEnabled(model.isEnabled());
                if (TextUtils.isEmpty(model.getValue())) {
                    textViewValue.setVisibility(View.GONE);
                } else {
                    textViewValue.setVisibility(View.VISIBLE);
                    textViewValue.setText(model.getValue());
                    if (model.getValueColorID() != 0) {
                        textViewValue.setTextColor(model.getValueColorID());
                    }
                }
            }
        }

        private void updateValueEditTextView(ListItem model) {
            if (editTextValue != null) {
                editTextValue.setEnabled(model.isEnabled());
                if (model.itemType == ListItem.ItemType.PRE_CHECK_VALUE_TYPE && model.isInputVisible()) {
                    editTextValue.setVisibility(View.VISIBLE);
                    editTextValue.setEnabled(model.isInputEditable());
                    editTextValue.setText(model.getInputString());
                    if (model.getValueColorID() != 0) {
                        editTextValue.setTextColor(model.getInputColorID());
                    }

                } else {
                    editTextValue.setVisibility(View.GONE);
                }
            }
        }

        private void updateArrowImage(ListItem model) {
            if (imgArrow != null) {
                if (model.itemType == ListItem.ItemType.PARENT_TYPE && model.isEnabled()) {
                    imgArrow.setVisibility(View.VISIBLE);
                } else {
                    imgArrow.setVisibility(View.GONE);
                }
            }
        }

        private void updateSwitchButtonState(ListItem model) {
            if (valueSwitchButton != null) {
                if (model.itemType == ListItem.ItemType.SWITCH_BUTTON_TYPE && model.isEnabled()) {
                    valueSwitchButton.setVisibility(View.VISIBLE);
                    valueSwitchButton.setChecked(model.valueId != 0);
                } else {
                    valueSwitchButton.setVisibility(View.GONE);
                }
            }
        }

        private void updateActionButtonState(ListItem model) {
            if (actionButton != null) {
                actionButton.setEnabled(model.isEnabled());
                String buttonTitle = model.getButtonTitle();
                if (buttonTitle == null || !model.isButtonTitleVisible()) {
                    actionButton.setVisibility(View.GONE);
                } else {
                    actionButton.setVisibility(View.VISIBLE);
                    actionButton.setText(buttonTitle);
                }
            }
        }

        private void updateEditText(ListItem model) {
            if (editText != null) {
                editText.setEnabled(model.isEnabled());
                editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (recyclerAdapter.edittextFocusListener != null) {
                            recyclerAdapter.edittextFocusListener.onEditTextFocus(editText, getAdapterPosition());
                        }
                    }
                });
                editText.setInputType(model.getInputType());
            }
        }

        private void updateFullActionButtonState(ListItem model) {
            if (fullActionButton != null) {
                String fullButtonTitle = model.getFullButtonTitle();
                if (fullButtonTitle == null) {
                    fullActionButton.setVisibility(View.GONE);
                } else {
                    fullActionButton.setVisibility(View.VISIBLE);
                    fullActionButton.setText(fullButtonTitle);
                }
                fullActionButton.setEnabled(model.isFullButtonEnabled());
            }
        }

        private void updateSeekBar(ListItem model) {
            if (model.itemType == ListItem.ItemType.SEEK_BAR_TYPE && seekBar != null) {
                seekBar.setVisibility(View.VISIBLE);
                seekBar.enable(true);
                seekBar.setMax(model.getMaxProgress());
                seekBar.setProgress(model.getProgress());
                seekBar.setText("");
                seekBar.setOnSeekBarChangeListener(this);
                seekBar.setMinValueVisibility(false);
                seekBar.setMaxValueVisibility(false);
                // update seekbar text
                onViewClicked(model, seekBar);
            }
        }

        private void updateSectionType(ListItem model) {
            if (model.itemType == ListItem.ItemType.SECTION_TYPE) {
                textViewTitle.setText(model.getTitle());
                textViewTitle.setTextColor(textViewTitle.getContext().getResources().getColor(R.color.uxsdk_white_60));
                itemView.setBackgroundResource(R.color.uxsdk_black_light);
            }
        }

        private void updateTips(ListItem model) {
            if (model.itemType == ListItem.ItemType.TIPS_TYPE) {
                textViewTitle.setText(model.getTitle());
                textViewTitle.setTextColor(textViewTitle.getContext().getResources().getColor(R.color.uxsdk_white_60));
            }
        }

        private void updateSegmentedView(ListItem model) {
            if (model.getSegmentedTitles() != null && segmentedView != null) {
                segmentedView.setSegmentStrings(model.getSegmentedTitles());
                segmentedView.setSelectedIndex(model.getSelectedIndex());
            }
        }

        private void updateRangeValue(ListItem model) {
            if (rangeUnitText != null) {
                rangeUnitText.setText(model.getRangeUnit());
            }

            if (model.itemType == ListItem.ItemType.SINGLE_EDIT_TEXT_TYPE) {
                rangeText.setText(model.getRangeStr());
                editText.setText(model.getValue());
            }

            if (model.itemType == ListItem.ItemType.SINGLE_EDIT_TEXT_BIG_TYPE) {
                bigRangeText.setText(model.getRangeStr());
                editText.setText(model.getValue());
            }
        }

        private void setListeners(boolean disableItemSelection) {
            // Set listener on view and switches
            if (!disableItemSelection) {
                itemView.setOnClickListener(this);
                if (valueSwitchButton != null) {
                    valueSwitchButton.setOnCheckedListener(this);
                }
            }
            if (actionButton != null) {
                actionButton.setOnClickListener(this);
            }

            if (editTextValue != null) {
                editTextValue.setOnClickListener(this);
                editTextValue.setOnEditorActionListener(this);
            }

            if (fullActionButton != null) {
                fullActionButton.setOnClickListener(this);
            }
        }

        @Override
        public void onCheckedChanged(boolean isChecked) {
            onViewClicked(model, valueSwitchButton);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onViewClicked(model, editText);
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (recyclerAdapter.keyboardActionListener != null) {
                return recyclerAdapter.keyboardActionListener.handleKeyboardAction(v, getAdapterPosition(), actionId, event);
            } else {
                return false;
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress) {
            if (recyclerAdapter.seekBarChangeListener != null) {
                recyclerAdapter.seekBarChangeListener.onProgressChanged(seekBar, progress, false, getAdapterPosition());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar, int progress) {
            if (recyclerAdapter.seekBarChangeListener != null) {
                recyclerAdapter.seekBarChangeListener.onStartTrackingTouch(seekBar, progress, getAdapterPosition());
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar, int progress) {
            if (recyclerAdapter.seekBarChangeListener != null) {
                recyclerAdapter.seekBarChangeListener.onStopTrackingTouch(seekBar, progress, getAdapterPosition());
            }
        }

        @Override
        public void onPlusClicked(SeekBar object) {
            onClick(object.findViewById(R.id.imageview_plus));
        }

        @Override
        public void onMinusClicked(SeekBar object) {
            onClick(object.findViewById(R.id.imageview_minus));
        }

        @Override
        public void onSelectIndex(int index) {
            onViewClicked(model, segmentedView);
        }

        @Override
        public void onClick(View view) {
            if (view == itemView && valueSwitchButton != null && valueSwitchButton.getVisibility() == View.VISIBLE) {
                valueSwitchButton.onClick(view);
            } else if (view == editTextValue) {
                editTextValue.setCursorVisible(true);
            } else {
                onViewClicked(model, view);
            }
        }

        private void onViewClicked(ListItem item, View view) {
            if (recyclerAdapter.changeListener != null) {
                recyclerAdapter.changeListener.updateSelectedItem(item, view);
            }
        }
        private void onViewClicked(View view) {
            if (recyclerAdapter.changeListener != null) {
                int position = getAdapterPosition();
                recyclerAdapter.changeListener.updateSelectedItem(recyclerAdapter.getItemByPos(position),view);
            }
        }
    }
    //endregion
}
