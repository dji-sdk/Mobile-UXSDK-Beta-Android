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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dji.common.accessory.SettingsDefinitions;
import dji.common.accessory.SpeakerState;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.util.CommonCallbacks;
import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.DJIKey;
import dji.log.DJILog;
import dji.sdk.accessory.speaker.AudioFileInfo;
import dji.sdk.accessory.speaker.Speaker;
import dji.sdk.accessory.speaker.TransmissionListener;
import dji.sdk.media.AudioMediaFile;
import dji.sdk.media.MediaManager;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.UXSDKError;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.AudioRecorderHandler;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.ProductUtil;

/**
 * Speaker Control Widget Model
 * <p>
 * Widget Model for the {@link SpeakerControlWidget} used to define the
 * underlying logic and communication
 */
public class SpeakerControlWidgetModel extends WidgetModel implements SpeakerState.Callback,
        MediaManager.FileListStateListener, AudioRecorderHandler.AudioRecordingCallback, TransmissionListener {
    //region Fields
    private static final String TAG = "SpeakerCtlWidgetModel";
    private static final int DEFAULT_VOLUME = 0;
    @Nullable
    private Speaker speaker;
    @Nullable
    private AudioRecorderHandler audioRecorderHandler;
    private final DataProcessor<Boolean> speakerConnectedDataProcessor;
    private final DataProcessor<Integer> speakerVolumeDataProcessor;
    private final DataProcessor<SpeakerState> speakerStateDataProcessor;
    private final DataProcessor<Boolean> isRecordingDataProcessor;
    private final DataProcessor<List<AudioMediaFile>> audioMediaFilesDataProcessor;
    private DJIKey speakerVolumeKey;

    private boolean isPersistFileEnabled;
    private boolean isInstantPlayEnabled;


    //endregion

    //region Lifecycle
    public SpeakerControlWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                     @NonNull ObservableInMemoryKeyedStore keyedStore,
                                     @NonNull AudioRecorderHandler audioRecorderHandler) {
        super(djiSdkModel, keyedStore);
        this.audioRecorderHandler = audioRecorderHandler;
        speakerConnectedDataProcessor = DataProcessor.create(false);
        isRecordingDataProcessor = DataProcessor.create(false);
        speakerVolumeDataProcessor = DataProcessor.create(DEFAULT_VOLUME);
        speakerStateDataProcessor = DataProcessor.create(new SpeakerState.Builder()
                .index(0)
                .playingMode(SettingsDefinitions.PlayMode.SINGLE_ONCE)
                .playingState(SettingsDefinitions.SpeakerPlayingState.STOPPED)
                .storageLocation(SettingsDefinitions.AudioStorageLocation.TEMPORARY)
                .volume(DEFAULT_VOLUME)
                .build());
        audioMediaFilesDataProcessor = DataProcessor.create(new ArrayList<>());

    }

    @Override
    protected void inSetup() {
        DJIKey speakerConnectedKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.CONNECTION);
        bindDataProcessor(speakerConnectedKey, speakerConnectedDataProcessor, speakerConnected -> onSpeakerConnected((boolean) speakerConnected));
        speakerVolumeKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.SPEAKER_VOLUME);
        bindDataProcessor(speakerVolumeKey, speakerVolumeDataProcessor);
    }


    @Override
    protected void inCleanup() {
        // No clean up
    }

    @Override
    protected void updateStates() {
        // no states
    }

    //endregion

    //region Speaker state callback
    @Override
    public void onUpdate(SpeakerState state) {
        speakerStateDataProcessor.onNext(state);
    }
    //endregion

    //region file list state callback
    @Override
    public void onFileListStateChange(MediaManager.FileListState state) {
        getPlaylist();
    }
    //endregion

    //region recording callback


    @Override
    public void onRecording(byte[] data) {
        if (speaker != null) {
            speaker.paceData(data);
        }


    }

    @Override
    public void onStopRecord(String savedPath) {
        if (speaker != null) {
            speaker.markEOF();
        }
        if (audioRecorderHandler != null) {
            audioRecorderHandler.deleteLastRecordFile();
        }
    }

    //endregion

    //region Upload file callback
    @Override
    public void onStart() {
        startRecordUsingMic();
    }

    @Override
    public void onProgress(int dataSize) {
    }

    @Override
    public void onFinish(int index) {
        if (isInstantPlayEnabled) {
            addDisposable(playFile(index).subscribe(() -> {
            }, error -> DJILog.d(TAG, "PLAY FILE " + error)));
        }
        isRecordingDataProcessor.onNext(false);

    }

    @Override
    public void onFailure(DJIError error) {
        isRecordingDataProcessor.onNext(false);
    }
    //endregion

    //region actions

    /**
     * Start recording the audio that the speaker should play
     *
     * @return Completable representing success or failure of action
     */
    public Completable startRecording() {
        return Completable.create(emitter -> {
            if (speaker == null) {
                emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
            }
            Format format = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.US);
            AudioFileInfo uploadInfo = new AudioFileInfo("AUD" + format.format(Calendar.getInstance().getTime()),
                    SettingsDefinitions.AudioStorageLocation.TEMPORARY);
            if (isPersistFileEnabled) {
                uploadInfo.setStorageLocation(SettingsDefinitions.AudioStorageLocation.PERSISTENT);
            }
            speaker.startTransmission(uploadInfo, SpeakerControlWidgetModel.this);
            emitter.onComplete();
        });
    }

    /**
     * Stop recording the audio to complete recording
     *
     * @return Completable representing success or failure of action
     */
    public Completable stopRecording() {
        return Completable.create(emitter -> {
            if (speaker == null) {
                emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
            }
            if (audioRecorderHandler == null) {
                emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
            }
            audioRecorderHandler.stopRecord();
            emitter.onComplete();
        });
    }

    /**
     * Play file on speaker
     *
     * @param index of file that should be played
     * @return Completable representing success or failure of action
     */
    public Completable playFile(final int index) {
        if (speakerStateDataProcessor.getValue().getPlayingState() == SettingsDefinitions.SpeakerPlayingState.PLAYING) {

            return Completable.create(emitter -> {
                if (speaker == null) {
                    emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
                }
                speaker.stop(error -> {
                    if (error == null) {
                        emitter.onComplete();

                    } else {
                        emitter.onError(new UXSDKError(error));
                    }
                });
            }).andThen(play(index));

        } else {
            return play(index);
        }
    }

    /**
     * Delete file from speaker
     *
     * @param index of file that should be deleted
     * @return Completable representing success or failure of action
     */
    public Completable deleteOneFileByIndex(final int index) {
        return Completable.create(emitter -> {
            if (speaker == null || audioMediaFilesDataProcessor.getValue().size() < index) {
                emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
            }
            ArrayList<Integer> fileToDelete = new ArrayList<>();
            fileToDelete.add(audioMediaFilesDataProcessor.getValue().get(index).getIndex());
            speaker.delete(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<Integer>, DJIError>() {
                @Override
                public void onSuccess(List<Integer> x, DJIError y) {
                    emitter.onComplete();
                }

                @Override
                public void onFailure(DJIError error) {
                    emitter.onError(new UXSDKError(error));
                }
            });
        });

    }

    /**
     * Set play in loop enabled.
     * This will play the same file on repeat
     *
     * @param isEnabled boolean true - loop mode false - not loop mode
     * @return Completable representing success or failure of action
     */
    public Completable setLoopModeEnabled(boolean isEnabled) {
        return Completable.create(emitter -> {
            if (speaker == null) {
                emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
            }
            SettingsDefinitions.PlayMode playMode = SettingsDefinitions.PlayMode.SINGLE_ONCE;
            if (isEnabled) {
                playMode = SettingsDefinitions.PlayMode.REPEAT_SINGLE;
            }
            speaker.setPlayMode(playMode, error -> {
                if (error == null) {
                    emitter.onComplete();
                } else {
                    emitter.onError(new UXSDKError(error));
                }
            });

        });

    }

    /**
     * Set speaker volume
     *
     * @param volume integer value representing volume
     * @return Completable representing success or failure of action
     */
    public Completable setSpeakerVolume(@IntRange(from = 0, to = 100) int volume) {
        return djiSdkModel.setValue(speakerVolumeKey, volume).subscribeOn(SchedulerProvider.io());
    }

    /**
     * Set persist file enabled.
     * If enabled the file will be saved on device to be played again later.
     * If disabled the recorded file will be one time broadcast
     *
     * @param isPersistFileEnabled boolean true - enabled  false - disabled
     */
    public void setPersistFileEnabled(boolean isPersistFileEnabled) {
        this.isPersistFileEnabled = isPersistFileEnabled;
    }

    /**
     * Set instant play enabled.
     * If enabled the speaker will play immediately after the recording is complete
     * If disabled the file will not be played immediately.
     *
     * @param isInstantPlayEnabled Completable representing success or failure of action
     */
    public void setInstantPlayEnabled(boolean isInstantPlayEnabled) {
        this.isInstantPlayEnabled = isInstantPlayEnabled;
    }

    //endregion

    //region Data

    /**
     * Get the speaker volume
     *
     * @return Flowable with integer value representing volume
     */
    public Flowable<Integer> getSpeakerVolume() {
        return speakerVolumeDataProcessor.toFlowable();
    }

    /**
     * Get the current speaker state
     *
     * @return Flowable with instance of {@link SpeakerState}
     */
    public Flowable<SpeakerState> getSpeakerState() {
        return speakerStateDataProcessor.toFlowable();
    }

    /**
     * Get the list of audio files from the device
     *
     * @return Flowable with list of {@link AudioMediaFile}
     */
    public Flowable<List<AudioMediaFile>> getMediaFileList() {
        return audioMediaFilesDataProcessor.toFlowable();
    }

    /**
     * Check if recording is in progress
     *
     * @return Flowable with boolean value true - recording false - not recording
     */
    public Flowable<Boolean> isRecording() {
        return isRecordingDataProcessor.toFlowable();
    }
    //endregion

    //region private helpers
    private Completable play(final int listPositionIndex) {
        return Completable.create(emitter -> {
            if (speaker == null) {
                emitter.onError(new UXSDKError(DJISDKError.COMMAND_EXECUTION_FAILED));
            }
            speaker.play(audioMediaFilesDataProcessor.getValue().get(listPositionIndex).getIndex(), error -> {
                if (error == null) {
                    emitter.onComplete();
                } else {
                    emitter.onError(new UXSDKError(error));
                }
            });
        });
    }

    private void getPlaylist() {
        if (speaker != null) {
            speaker.refreshFileList(error -> {
                if (null == error) {
                    List<AudioMediaFile> audioMediaFiles = speaker.getFileListSnapshot();
                    if (audioMediaFiles != null) {
                        audioMediaFilesDataProcessor.onNext(audioMediaFiles);
                    }

                }
            });
        }
    }

    private void onSpeakerConnected(boolean speakerConnected) {
        if (speakerConnected) {
            speaker = ProductUtil.getSpeaker();
            if (speaker != null) {
                speaker.setStateCallback(this);
                speaker.addFileListStateListener(this);
            }

        }

    }

    private void startRecordUsingMic() {
        if (audioRecorderHandler != null) {
            audioRecorderHandler.startRecord(this);
            isRecordingDataProcessor.onNext(true);
        }
    }

    //endregion
}
