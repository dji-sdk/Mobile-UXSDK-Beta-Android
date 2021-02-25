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
 *
 */

package dji.ux.beta.accessory.widget.speaker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dji.common.accessory.SettingsDefinitions;
import dji.common.accessory.SpeakerState;
import dji.sdk.media.AudioMediaFile;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.accessory.R;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.OnStateChangeCallback;
import dji.ux.beta.core.util.AudioRecorderHandler;

import static dji.ux.beta.core.extension.TypedArrayExtensions.INVALID_RESOURCE;

/**
 * Widget can be used to control of state of Speaker accessory.
 * The widget provides a way to record audio file.
 * It also displays a list of playable audio files to choose from.
 */
public class SpeakerControlWidget extends ConstraintLayoutWidget implements OnClickListener, OnCheckedChangeListener, AppCompatSeekBar.OnSeekBarChangeListener, OnStateChangeCallback {

    //region Fields
    private static final String TAG = "SpeakerCtlWidget";
    private TextView titleTextView;
    private TextView speakerVolumeLabel;
    private TextView instantPlayLabel;
    private TextView playInLoopLabel;
    private TextView persistFileLabel;
    private static final Format DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.US);
    private TextView recordTabTextView;
    private TextView fileListTabTextView;
    private ConstraintLayout recordButtonContainer;
    private ConstraintLayout mediaFileListContainer;
    private ImageView recordImageView;
    private TextView recordStatusTextView;
    private Switch playInLoopSwitch;
    private Switch persistFileSwitch;
    private Switch instantPlayFileSwitch;
    private AppCompatSeekBar volumeSeekbar;
    private SpeakerControlWidgetModel widgetModel;
    private AudioFileListAdapter audioFileListAdapter;
    private int currentPlayingFileIndex;
    private SpeakerWidgetState speakerWidgetState = SpeakerWidgetState.BROADCAST;
    private TimerRunnable recordingTimerTracker;
    private Animation animation;
    private float listTextSize;
    @ColorInt
    private int listTextColor;
    private Drawable listTextBackground;
    private Drawable listImageBackground;
    private Drawable listDeleteIcon;
    private Drawable listStartPlayIcon;
    private Drawable listStopPlayIcon;


    //endregion

    //region Lifecycle
    public SpeakerControlWidget(Context context) {
        super(context);
    }

    public SpeakerControlWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpeakerControlWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_speaker_control, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        titleTextView = findViewById(R.id.text_view_speaker_title);
        speakerVolumeLabel = findViewById(R.id.text_view_speaker_volume);
        instantPlayLabel = findViewById(R.id.text_view_instant_play);
        playInLoopLabel = findViewById(R.id.text_view_loop_mode);
        persistFileLabel = findViewById(R.id.text_view_persist_file);
        recordTabTextView = findViewById(R.id.tab_instant_broadcast);
        fileListTabTextView = findViewById(R.id.tab_local_file);
        recordButtonContainer = findViewById(R.id.record_button_container);
        mediaFileListContainer = findViewById(R.id.file_list_container);
        recordImageView = findViewById(R.id.start_broadcast_button);
        recordStatusTextView = findViewById(R.id.audio_record_status_text_view);
        playInLoopSwitch = findViewById(R.id.loop_play_switch);
        persistFileSwitch = findViewById(R.id.audio_temporary_switch);
        instantPlayFileSwitch = findViewById(R.id.instant_play_switch);
        volumeSeekbar = findViewById(R.id.volume_seek_bar);
        RecyclerView recyclerView = findViewById(R.id.audio_file_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        audioFileListAdapter = new AudioFileListAdapter();
        recyclerView.setAdapter(audioFileListAdapter);
        recordTabTextView.setOnClickListener(this);
        fileListTabTextView.setOnClickListener(this);
        recordImageView.setOnClickListener(this);
        playInLoopSwitch.setOnCheckedChangeListener(this);
        persistFileSwitch.setOnCheckedChangeListener(this);
        instantPlayFileSwitch.setOnCheckedChangeListener(this);
        volumeSeekbar.setOnSeekBarChangeListener(this);

        AudioRecorderHandler audioRecorderHandler = new AudioRecorderHandler(context);
        if (!isInEditMode()) {
            widgetModel = new SpeakerControlWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    audioRecorderHandler);
        }
        speakerWidgetState = SpeakerWidgetState.BROADCAST;
        setPanelState();

        if (attrs != null) {
            initAttributes(context, attrs);
        }

    }


    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getSpeakerVolume()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onSpeakerVolumeChanged));

        addReaction(widgetModel.getMediaFileList()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onMediaListChanged));

        addReaction(widgetModel.getSpeakerState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onSpeakerStateChanged));

        addReaction(widgetModel.isRecording()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::onRecording));
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_speaker_control_ratio);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.start_broadcast_button) {
            checkAndStartRecording();
        } else if (id == R.id.audio_file_play_image_view) {
            playAudioInList((int) v.getTag());
        } else if (id == R.id.audio_file_delete_image_view) {
            deleteOneFileByIndex((int) v.getTag());
        } else if (id == R.id.tab_instant_broadcast) {
            speakerWidgetState = SpeakerWidgetState.BROADCAST;
            setPanelState();
        } else if (id == R.id.tab_local_file) {
            speakerWidgetState = SpeakerWidgetState.LOCAL_FILE_LIST;
            setPanelState();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.equals(playInLoopSwitch)) {
            addDisposable(widgetModel.setLoopModeEnabled(isChecked).subscribe(() -> {
            }, logErrorConsumer(TAG, "set loop enabled ")));
        } else if (buttonView.equals(instantPlayFileSwitch)) {
            widgetModel.setInstantPlayEnabled(isChecked);
        } else if (buttonView.equals(persistFileSwitch)) {
            widgetModel.setPersistFileEnabled(isChecked);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.equals(volumeSeekbar) && fromUser) {
            handleSeekbarChanged(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //empty method
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //empty method
    }


    @Override
    public void onStateChange(@Nullable Object state) {
        toggleVisibility();
    }
    //endregion

    //region private helpers

    private void toggleVisibility() {
        if (getVisibility() == VISIBLE) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    private void onSpeakerVolumeChanged(int integer) {
        volumeSeekbar.setProgress(integer);
    }


    private void onRecording(Boolean isRecording) {
        if (isRecording) {

            if (animation != null) {
                recordImageView.startAnimation(animation);
            }

            if (recordingTimerTracker != null) {
                recordingTimerTracker = null;
            }
            recordingTimerTracker = new TimerRunnable(SystemClock.uptimeMillis(), true);
            getHandler().post(recordingTimerTracker);

        } else {
            if (recordingTimerTracker != null) {
                recordingTimerTracker.setRecording(false);
                recordImageView.clearAnimation();
            }

        }

    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeakerControlWidget);

        int textAppearance = typedArray.getResourceId(R.styleable.SpeakerControlWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(textAppearance);
        }
        setTitleTextColor(typedArray.getColor(R.styleable.SpeakerControlWidget_uxsdk_widgetTitleTextColor, Color.WHITE));
        setTitleBackground(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_widgetTitleBackground));
        setTitleTextSize(typedArray.getDimension(R.styleable.SpeakerControlWidget_uxsdk_widgetTitleTextSize, 14));
        textAppearance = typedArray.getResourceId(R.styleable.SpeakerControlWidget_uxsdk_labelsTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setLabelsTextAppearance(textAppearance);
        }
        setLabelsTextColor(typedArray.getColor(R.styleable.SpeakerControlWidget_uxsdk_labelsTextColor, Color.WHITE));
        setLabelsBackground(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_labelsBackground));
        setLabelsTextSize(typedArray.getDimension(R.styleable.SpeakerControlWidget_uxsdk_labelsTextSize, 12));
        textAppearance = typedArray.getResourceId(R.styleable.SpeakerControlWidget_uxsdk_recordTimerTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setRecordTimerTextAppearance(textAppearance);
        }
        setRecordTimerTextColor(typedArray.getColor(R.styleable.SpeakerControlWidget_uxsdk_recordTimerTextColor, Color.WHITE));
        setRecordTimerTextBackground(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_recordTimerTextBackground));
        setRecordTimerTextSize(typedArray.getDimension(R.styleable.SpeakerControlWidget_uxsdk_recordTimerTextSize, 12));
        textAppearance = typedArray.getResourceId(R.styleable.SpeakerControlWidget_uxsdk_tabTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTabTextAppearance(textAppearance);
        }
        ColorStateList colorStateList = typedArray.getColorStateList(R.styleable.SpeakerControlWidget_uxsdk_tabTextColor);
        if (colorStateList != null) {
            setTabTextColors(colorStateList);
        }
        setBroadcastTabBackgroundSelector((typedArray.getResourceId(R.styleable.SpeakerControlWidget_uxsdk_tabBroadcastBackgroundSelector, R.drawable.uxsdk_selector_speaker_widget_broadcast_tab)));
        setFileListTabBackgroundSelector((typedArray.getResourceId(R.styleable.SpeakerControlWidget_uxsdk_tabFileListBackgroundSelector, R.drawable.uxsdk_selector_speaker_widget_local_file_tab)));
        setTabTextSize(typedArray.getDimension(R.styleable.SpeakerControlWidget_uxsdk_tabTextSize, 12));

        setFileListTextColor(typedArray.getColor(R.styleable.SpeakerControlWidget_uxsdk_fileListTextColor, Color.WHITE));
        setFileListTextBackground(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListTextBackground));
        setFileListTextSize(typedArray.getDimension(R.styleable.SpeakerControlWidget_uxsdk_fileListTextSize, 8));
        if (typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListDeleteIcon) != null) {
            setFileListDeleteIcon(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListDeleteIcon));
        }
        if (typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListPlayIcon) != null) {
            setFileListPlayIcon(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListPlayIcon));
        }
        if (typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListStopIcon) != null) {
            setFileListStopPlayIcon(typedArray.getDrawable(R.styleable.SpeakerControlWidget_uxsdk_fileListStopIcon));
        }

        animation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_blink);
        typedArray.recycle();
    }

    private void setPanelState() {
        if (speakerWidgetState == SpeakerWidgetState.BROADCAST) {
            recordTabTextView.setSelected(true);

            fileListTabTextView.setSelected(false);

            mediaFileListContainer.setVisibility(View.GONE);
            recordButtonContainer.setVisibility(VISIBLE);

        } else if (speakerWidgetState == SpeakerWidgetState.LOCAL_FILE_LIST) {
            fileListTabTextView.setSelected(true);

            recordTabTextView.setSelected(false);
            mediaFileListContainer.setVisibility(View.VISIBLE);
            recordButtonContainer.setVisibility(GONE);
        }
    }


    private void onSpeakerStateChanged(SpeakerState speakerState) {
        currentPlayingFileIndex = speakerState.getPlayingIndex();
        if (speakerState.getPlayingMode() == SettingsDefinitions.PlayMode.SINGLE_ONCE) {
            playInLoopSwitch.setChecked(false);
        } else if (speakerState.getPlayingMode() == SettingsDefinitions.PlayMode.REPEAT_SINGLE) {
            playInLoopSwitch.setChecked(true);
        }
        if (audioFileListAdapter != null) {
            audioFileListAdapter.notifyDataSetChanged();
        }
    }


    private void onMediaListChanged(List<AudioMediaFile> audioMediaFiles) {
        audioFileListAdapter.setMediaFileList(audioMediaFiles);
        audioFileListAdapter.notifyDataSetChanged();

    }

    private void playAudioInList(int index) {
        addDisposable(widgetModel.playFile(index).subscribe(() -> {
        }, logErrorConsumer(TAG, "Play file ")));

    }

    private void deleteOneFileByIndex(int index) {
        addDisposable(widgetModel.deleteOneFileByIndex(index).subscribe(() -> {
        }, logErrorConsumer(TAG, "Delete file ")));
    }


    private void handleSeekbarChanged(int progress) {
        addDisposable(widgetModel.setSpeakerVolume(progress).subscribe(() -> {
        }, logErrorConsumer(TAG, "Set Volume ")));
    }

    private void checkAndStartRecording() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(),
                    getResources().getString(R.string.uxsdk_speaker_record_permission_required),
                    Toast.LENGTH_LONG).show();
            return;
        }

        addDisposable(widgetModel.isRecording().firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        isRecording -> {
                            if (isRecording) {
                                stopRecordAction();
                            } else {
                                startRecordAction();
                            }

                        }));

    }

    private void startRecordAction() {
        addDisposable(widgetModel.startRecording().subscribe(() -> {
        }, logErrorConsumer(TAG, "Start record ")));
    }

    private void stopRecordAction() {
        addDisposable(widgetModel.stopRecording().subscribe(() -> {
        }, logErrorConsumer(TAG, "Stop record ")));
    }


    private static class ItemHolder extends RecyclerView.ViewHolder {
        private ImageView playFileImageView;
        private ImageView deleteFileImageView;
        private TextView fileNameTextView;

        public ItemHolder(View convertView) {
            super(convertView);
            this.playFileImageView = convertView.findViewById(R.id.audio_file_play_image_view);
            this.deleteFileImageView = convertView.findViewById(R.id.audio_file_delete_image_view);
            this.fileNameTextView = convertView.findViewById(R.id.audio_file_name_text_view);

        }
    }


    private class AudioFileListAdapter extends RecyclerView.Adapter<ItemHolder> {
        @Nullable
        private List<AudioMediaFile> mediaFileList;

        public void setMediaFileList(@Nullable List<AudioMediaFile> mediaFileList) {
            this.mediaFileList = mediaFileList;
        }

        @Override
        @NonNull
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.uxsdk_item_audio_file, parent, false);
            ItemHolder itemHolder = new ItemHolder(view);
            listTextSize = itemHolder.fileNameTextView.getTextSize();
            listTextBackground = itemHolder.fileNameTextView.getBackground();
            listTextColor = itemHolder.fileNameTextView.getCurrentTextColor();
            listImageBackground = itemHolder.deleteFileImageView.getBackground();
            listStartPlayIcon = getResources().getDrawable(R.drawable.uxsdk_selector_speaker_start_play);
            listStopPlayIcon = getResources().getDrawable(R.drawable.uxsdk_selector_speaker_stop_play);
            listDeleteIcon = getResources().getDrawable(R.drawable.uxsdk_selector_icon_delete);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            if (mediaFileList != null) {
                final AudioMediaFile mediaFile = mediaFileList.get(position);
                if (mediaFile != null) {
                    holder.fileNameTextView.setTextSize(listTextSize);
                    holder.fileNameTextView.setBackground(listTextBackground);
                    holder.fileNameTextView.setTextColor(listTextColor);
                    holder.deleteFileImageView.setBackground(listImageBackground);
                    holder.playFileImageView.setBackground(listImageBackground);

                    if (mediaFile.getFileName() == null) {
                        Date date = new Date(mediaFile.getTimeCreated());
                        holder.fileNameTextView.setText(getResources().getString(R.string.uxsdk_speaker_panel_list_prefix, DATE_FORMAT.format(date)));
                    } else {
                        holder.fileNameTextView.setText(mediaFile.getFileName());
                    }

                    holder.playFileImageView.setOnClickListener(SpeakerControlWidget.this);
                    holder.deleteFileImageView.setOnClickListener(SpeakerControlWidget.this);
                    if (currentPlayingFileIndex == mediaFile.getIndex()) {
                        holder.playFileImageView.setImageDrawable(listStopPlayIcon);
                    } else {
                        holder.playFileImageView.setImageDrawable(listStartPlayIcon);
                    }
                    holder.deleteFileImageView.setImageDrawable(listDeleteIcon);
                    holder.playFileImageView.setTag(position);
                    holder.deleteFileImageView.setTag(position);

                }
            }
        }


        @Override
        public int getItemCount() {
            if (mediaFileList != null) {
                return mediaFileList.size();
            }
            return 0;
        }
    }

    private class TimerRunnable implements Runnable {
        private boolean isRecording;
        private long startTime;

        public TimerRunnable(long startTime, boolean isRecording) {
            this.startTime = startTime;
            this.isRecording = isRecording;
        }

        public void setRecording(boolean recording) {
            isRecording = recording;
        }

        @Override
        public void run() {
            long millisecondTime = SystemClock.uptimeMillis() - startTime;
            int seconds = (int) (millisecondTime / 1000);
            int minutes = seconds / 60;
            recordStatusTextView.setText(String.format(Locale.US, "%d:%02d", minutes, seconds));
            if (isRecording) {
                getHandler().postDelayed(this, 0);
            } else {
                recordStatusTextView.setText(getResources().getString(R.string.uxsdk_speaker_panel_tap_to_record));
            }
        }
    }

    private enum SpeakerWidgetState {
        BROADCAST(0),
        LOCAL_FILE_LIST(1),
        UNKNOWN(10);

        private final int mValue;

        SpeakerWidgetState(int value) {
            mValue = value;
        }

        private static SpeakerWidgetState[] values;

        public static SpeakerWidgetState[] getValues() {
            if (values == null) {
                values = values();
            }
            return values;
        }

        public static SpeakerWidgetState find(int b) {
            SpeakerWidgetState result = UNKNOWN;
            for (int i = 0; i < getValues().length; i++) {
                if (getValues()[i]._equals(b)) {
                    result = getValues()[i];
                    break;
                }
            }
            return result;
        }

        public int value() {
            return mValue;
        }

        private boolean _equals(int b) {
            return mValue == b;
        }
    }

    //endregion

    //region title customizations

    /**
     * Set the text color state list of the title
     *
     * @param colorStateList to be used for title
     */
    public void setTitleTextColor(@Nullable ColorStateList colorStateList) {
        titleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color of title
     *
     * @param color integer value
     */
    public void setTitleTextColor(@ColorInt int color) {
        titleTextView.setTextColor(color);
    }

    /**
     * Get the text color state list of the titles
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return titleTextView.getTextColors();
    }

    /**
     * Get the text color of title
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getTitleTextColor() {
        return titleTextView.getCurrentTextColor();
    }

    /**
     * Set the text appearance of title
     *
     * @param textAppearance to be used
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        titleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set the background of the title
     *
     * @param resourceId to be used
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of the title
     *
     * @param drawable to be used
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        titleTextView.setBackground(drawable);
    }

    /**
     * Get the background of the title
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getTitleBackground() {
        return titleTextView.getBackground();
    }

    /**
     * Set the size of the title text
     *
     * @param textSize float value
     */
    public void setTitleTextSize(@Dimension float textSize) {
        titleTextView.setTextSize(textSize);
    }

    /**
     * Get the size of title text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getTitleTextSize() {
        return titleTextView.getTextSize();
    }

    //endregion

    //region label customizations

    /**
     * Set the color state list for labels
     *
     * @param colorStateList to be used
     */
    public void setLabelsTextColor(@Nullable ColorStateList colorStateList) {
        speakerVolumeLabel.setTextColor(colorStateList);
        playInLoopLabel.setTextColor(colorStateList);
        persistFileLabel.setTextColor(colorStateList);
        instantPlayLabel.setTextColor(colorStateList);
    }

    /**
     * Set the text color to be used for labels
     *
     * @param color integer value representing color
     */
    public void setLabelsTextColor(@ColorInt int color) {
        speakerVolumeLabel.setTextColor(color);
        playInLoopLabel.setTextColor(color);
        persistFileLabel.setTextColor(color);
        instantPlayLabel.setTextColor(color);
    }

    /**
     * Get the text colors for labels
     *
     * @return the color state list being used as labels
     */
    @Nullable
    public ColorStateList getLabelsTextColors() {
        return speakerVolumeLabel.getTextColors();
    }

    /**
     * Get the text color of labels
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getLabelsTextColor() {
        return speakerVolumeLabel.getCurrentTextColor();
    }

    /**
     * Set the text appearance of labels
     *
     * @param textAppearance to be used
     */
    public void setLabelsTextAppearance(@StyleRes int textAppearance) {
        speakerVolumeLabel.setTextAppearance(getContext(), textAppearance);
        playInLoopLabel.setTextAppearance(getContext(), textAppearance);
        persistFileLabel.setTextAppearance(getContext(), textAppearance);
        instantPlayLabel.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set the background of labels
     *
     * @param resourceId to be used
     */
    public void setLabelsBackground(@DrawableRes int resourceId) {
        setLabelsBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of labels
     *
     * @param drawable to be used
     */
    public void setLabelsBackground(@Nullable Drawable drawable) {
        speakerVolumeLabel.setBackground(drawable);
        playInLoopLabel.setBackground(drawable);
        persistFileLabel.setBackground(drawable);
        instantPlayLabel.setBackground(drawable);
    }

    /**
     * Get the background of labels
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getLabelsBackground() {
        return speakerVolumeLabel.getBackground();
    }

    /**
     * Set the text size of labels
     *
     * @param textSize float value
     */
    public void setLabelsTextSize(@Dimension float textSize) {
        speakerVolumeLabel.setTextSize(textSize);
        playInLoopLabel.setTextSize(textSize);
        persistFileLabel.setTextSize(textSize);
        instantPlayLabel.setTextSize(textSize);
    }

    /**
     * Get the text size of labels
     *
     * @return float value representing text size
     */
    @Dimension
    public float getLabelsTextSize() {
        return speakerVolumeLabel.getTextSize();
    }

    //endregion

    //region  tab customizations

    /**
     * Set the broadcast tab background selector
     * <p>
     * android:state_selected="true" for selected state
     * android:state_selected="false" for not selected state
     *
     * @param resourceId to be used
     */
    public void setBroadcastTabBackgroundSelector(@DrawableRes int resourceId) {
        recordTabTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the file list tab background selector
     * <p>
     * android:state_selected="true" for selected state
     * android:state_selected="false" for not selected state
     *
     * @param resourceId to be used
     */
    public void setFileListTabBackgroundSelector(@DrawableRes int resourceId) {
        fileListTabTextView.setBackgroundResource(resourceId);
    }

    /**
     * Get the background of the broadcast tab
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getBroadcastTabBackground() {
        return recordTabTextView.getBackground();
    }

    /**
     * Get the background of the file list tab
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getFileListTabBackground() {
        return fileListTabTextView.getBackground();
    }

    /**
     * Set the text color state list for tab text
     *
     * @param colorStateList to be used
     */
    public void setTabTextColors(@NonNull ColorStateList colorStateList) {
        recordTabTextView.setTextColor(colorStateList);
        fileListTabTextView.setTextColor(colorStateList);
    }

    /**
     * Get the text color state list for tab text
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getTabTextColors() {
        return recordTabTextView.getTextColors();
    }

    /**
     * Set the text appearance of tab text
     *
     * @param textAppearance to be used
     */
    public void setTabTextAppearance(@StyleRes int textAppearance) {
        recordTabTextView.setTextAppearance(getContext(), textAppearance);
        fileListTabTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Set the text size of tab text
     *
     * @param textSize float value
     */
    public void setTabTextSize(@Dimension float textSize) {
        recordTabTextView.setTextSize(textSize);
        fileListTabTextView.setTextSize(textSize);
    }

    /**
     * Get the text size of tab text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getTabTextSize() {
        return recordTabTextView.getTextSize();
    }

    //endregion

    //region record button customizations

    /**
     * Set the icon for record audio button
     *
     * @param resourceId to be used
     */
    public void setRecordButtonIcon(@DrawableRes int resourceId) {
        recordImageView.setImageResource(resourceId);
    }

    /**
     * Set the icon for record audio button
     *
     * @param drawable to be used
     */
    public void setRecordButtonIcon(@Nullable Drawable drawable) {
        recordImageView.setImageDrawable(drawable);
    }

    /**
     * Get the icon of record audio button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getRecordButtonIcon() {
        return recordImageView.getDrawable();
    }

    /**
     * Set the background of the record audio button
     *
     * @param resourceId to be used
     */
    public void setRecordButtonBackground(@DrawableRes int resourceId) {
        recordImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the record audio button
     *
     * @param drawable to be used
     */
    public void setRecordButtonBackground(@Nullable Drawable drawable) {
        recordImageView.setBackground(drawable);
    }

    /**
     * Get the background of the record audio button
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getRecordButtonBackground() {
        return recordImageView.getBackground();
    }

    /**
     * Set the animation for audio recording
     *
     * @param animation to be used
     */
    public void setRecordingAnimation(@Nullable Animation animation) {
        this.animation = animation;
    }

    /**
     * Set the background of record duration timer text
     *
     * @param resourceId to be used
     */
    public void setRecordTimerTextBackground(@DrawableRes int resourceId) {
        recordStatusTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of record duration timer text
     *
     * @param drawable to be used
     */
    public void setRecordTimerTextBackground(@Nullable Drawable drawable) {
        recordStatusTextView.setBackground(drawable);
    }

    /**
     * Set the color state list of the record duration timer text
     *
     * @param colorStateList to be used
     */
    public void setRecordTimerTextColors(@Nullable ColorStateList colorStateList) {
        recordStatusTextView.setTextColor(colorStateList);
    }

    /**
     * Set the color of the record duration text
     *
     * @param color integer value
     */
    public void setRecordTimerTextColor(@ColorInt int color) {
        recordStatusTextView.setTextColor(color);
    }

    /**
     * Get the color state list of the record duration text
     *
     * @return ColorStateList
     */
    @Nullable
    public ColorStateList getRecordTimerTextColors() {
        return recordStatusTextView.getTextColors();
    }

    /**
     * Get the color of the record duration text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getRecordTimerTextColor() {
        return recordStatusTextView.getCurrentTextColor();
    }

    /**
     * Set the size of the record duration text
     *
     * @param textSize float value
     */
    public void setRecordTimerTextSize(@Dimension float textSize) {
        recordStatusTextView.setTextSize(textSize);
    }


    /**
     * Get the size of the record duration text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getRecordTimerTextSize() {
        return recordStatusTextView.getTextSize();
    }

    /**
     * Set the appearance of the record duration text
     *
     * @param textAppearance to be used
     */
    public void setRecordTimerTextAppearance(@StyleRes int textAppearance) {
        recordStatusTextView.setTextAppearance(getContext(), textAppearance);
    }

    //endregion

    //region Seekbar customizations

    /**
     * Set the volume seekbar thumb icon
     *
     * @param resourceId to be used
     */
    public void setVolumeSeekbarThumbDrawable(@DrawableRes int resourceId) {
        setVolumeSeekbarThumbDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the volume seekbar thumb icon
     *
     * @param drawable to be used
     */
    public void setVolumeSeekbarThumbDrawable(@NonNull Drawable drawable) {
        volumeSeekbar.setThumb(drawable);
    }

    /**
     * Get the drawable used as the volume seekbar thumb icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getVolumeSeekbarThumbDrawable() {
        return volumeSeekbar.getThumb();
    }

    /**
     * Get the tint list for volume seek bar background
     *
     * @return ColorStateList
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorStateList getVolumeSeekbarBackgroundTintList() {
        return volumeSeekbar.getBackgroundTintList();
    }

    /**
     * Set the tint list for the volume seek bar background
     *
     * @param colorStateList to be used
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void setVolumeSeekbarBackgroundTintList(@Nullable ColorStateList colorStateList) {
        volumeSeekbar.setBackgroundTintList(colorStateList);
    }

    /**
     * Set the tint list for the volume seek bar progress
     *
     * @param colorStateList to be used
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void setVolumeSeekbarProgressTintList(@Nullable ColorStateList colorStateList) {
        volumeSeekbar.setProgressTintList(colorStateList);
    }

    /**
     * Get the tint list for the volume seek bar progress
     *
     * @return ColorStateList
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorStateList getVolumeSeekbarProgressTintList() {
        return volumeSeekbar.getProgressTintList();
    }
    //endregion

    //region Switch customizations

    /**
     * Set the thumb icon used for the switches
     *
     * @param resourceId to be used
     */
    public void setSwitchThumb(@DrawableRes int resourceId) {
        playInLoopSwitch.setThumbResource(resourceId);
        persistFileSwitch.setThumbResource(resourceId);
        instantPlayFileSwitch.setThumbResource(resourceId);
    }

    /**
     * Set the thumb icon used for the switches
     *
     * @param drawable to be used
     */
    public void setSwitchThumb(@Nullable Drawable drawable) {
        playInLoopSwitch.setThumbDrawable(drawable);
        persistFileSwitch.setThumbDrawable(drawable);
        instantPlayFileSwitch.setThumbDrawable(drawable);
    }

    /**
     * Get the thumb icon used by the switches
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getSwitchThumb() {
        return playInLoopSwitch.getThumbDrawable();
    }

    /**
     * Set the switch thumb tint list
     *
     * @param colorStateList to be used
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public void setSwitchThumbTintList(@Nullable ColorStateList colorStateList) {
        playInLoopSwitch.setThumbTintList(colorStateList);
        persistFileSwitch.setThumbTintList(colorStateList);
        instantPlayFileSwitch.setThumbTintList(colorStateList);
    }

    /**
     * Get the switch thumb tint list
     *
     * @return ColorStateList
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.M)
    public ColorStateList getSwitchThumbTintList() {
        return playInLoopSwitch.getThumbTintList();
    }

    //endregion

    //region file list customizations

    /**
     * Set the color of the file name text in the audio file list
     *
     * @param color integer value representing color
     */
    public void setFileListTextColor(@ColorInt int color) {
        listTextColor = color;
    }

    /**
     * Get the color of the file name text in the audio file list
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getFileListTextColor() {
        return listTextColor;
    }

    /**
     * Set the background of file name text in the audio file list
     *
     * @param resourceId to be used
     */
    public void setFileListTextBackground(@DrawableRes int resourceId) {
        setFileListTextBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of file name text in the audio file list
     *
     * @param drawable to be used
     */
    public void setFileListTextBackground(@Nullable Drawable drawable) {
        listTextBackground = drawable;
    }


    /**
     * Get the file name text background in the audio file list
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getFileListTextBackground() {
        return listTextBackground;
    }

    /**
     * Set the size of the file name text in the audio file list
     *
     * @param textSize float value
     */
    public void setFileListTextSize(@Dimension float textSize) {
        listTextSize = textSize;
    }

    /**
     * Get the size of file name text in the audio file list
     *
     * @return float representing text size
     */
    @Dimension
    public float getFileListTextSize() {
        return listTextSize;
    }

    /**
     * Set the icon for the delete button in the audio file list
     *
     * @param resourceId to be used
     */
    public void setFileListDeleteIcon(@DrawableRes int resourceId) {
        setFileListDeleteIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for the delete button in the audio file list
     *
     * @param drawable to be used
     */
    public void setFileListDeleteIcon(@Nullable Drawable drawable) {
        listDeleteIcon = drawable;
    }

    /**
     * Get the icon for the delete button displayed in the audio file list
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getFileListDeleteIcon() {
        return listDeleteIcon;
    }

    /**
     * Set the icon for the start play button in the audio file list
     *
     * @param resourceId to be used
     */
    public void setFileListPlayIcon(@DrawableRes int resourceId) {
        setFileListPlayIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for the start play button in the audio file list
     *
     * @param drawable to be used
     */
    public void setFileListPlayIcon(@Nullable Drawable drawable) {
        listStartPlayIcon = drawable;
    }

    /**
     * Get the icon for the start play button displayed in the audio file list
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getFileListPlayIcon() {
        return listStartPlayIcon;
    }

    /**
     * Set the icon for the stop play button in the audio file list
     *
     * @param resourceId to be used
     */
    public void setFileListStopPlayIcon(@DrawableRes int resourceId) {
        setFileListStopPlayIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for the stop play button in the audio file list
     *
     * @param drawable to be used
     */
    public void setFileListStopPlayIcon(@Nullable Drawable drawable) {
        listStopPlayIcon = drawable;
    }

    /**
     * Get the stop play icon displayed in the audio file list
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getFileListStopPlayIcon() {
        return listStopPlayIcon;
    }

    //endregion


}
