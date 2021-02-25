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

package dji.ux.beta.visualcamera.widget.histogram;

import androidx.annotation.NonNull;

import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.ArrayUtil;
import dji.ux.beta.core.util.DataProcessor;
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex;

/**
 * Widget Model for the {@link HistogramWidget} used to define the
 * underlying logic and communication
 */
public class HistogramWidgetModel extends WidgetModel {

    //region Constants
    private static final int MAX_DATA_LENGTH = 64;
    private static final int INDEX_START = 3;
    private static final int INDEX_END = 3;
    private final float[] data = new float[MAX_DATA_LENGTH - INDEX_START - INDEX_END];
    //endregion

    //region Fields
    private final DataProcessor<Boolean> histogramEnabledProcessor;
    private final DataProcessor<Short[]> histogramLightValueProcessor;
    private final DataProcessor<Float[]> lightValueProcessor;
    private DJIKey histogramEnabledKey;
    private int cameraIndex = CameraIndex.CAMERA_INDEX_0.getIndex();
    //endregion

    //region Constructor
    public HistogramWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        histogramEnabledProcessor = DataProcessor.create(false);
        histogramLightValueProcessor = DataProcessor.create(new Short[MAX_DATA_LENGTH]);
        lightValueProcessor = DataProcessor.create(ArrayUtil.toObject(data));
    }
    //endregion

    //region Data

    /**
     * Get whether the histogram is enabled.
     *
     * @return Flowable with boolean representing whether the histogram is enabled.
     */
    public Flowable<Boolean> getHistogramEnabled() {
        return histogramEnabledProcessor.toFlowable();
    }
    //endregion

    /**
     * Get the light values.
     *
     * @return Flowable with an array of floats representing the light values.
     */
    public Flowable<Float[]> getLightValues() {
        return lightValueProcessor.toFlowable();
    }

    //region Lifecycle
    @Override
    protected void inSetup() {
        histogramEnabledKey = CameraKey.create(CameraKey.HISTOGRAM_ENABLED, cameraIndex);
        bindDataProcessor(histogramEnabledKey, histogramEnabledProcessor);
        DJIKey histogramLightValueKey = CameraKey.create(CameraKey.HISTOGRAM_LIGHT_VALUES, cameraIndex);
        bindDataProcessor(histogramLightValueKey, histogramLightValueProcessor);
    }

    @Override
    protected void inCleanup() {
        //nothing to clean
    }

    @Override
    protected void updateStates() {
        copyData(data, ArrayUtil.toPrimitive(histogramLightValueProcessor.getValue()));
        lightValueProcessor.onNext(ArrayUtil.toObject(data));
    }
    //endregion

    //region Customization
    /**
     * Get the current index of the camera the widget model is reacting to.
     *
     * @return current camera index.
     */
    @NonNull
    public CameraIndex getCameraIndex() {
        return CameraIndex.find(cameraIndex);
    }

    /**
     * Set the index of the camera for which the widget model should react.
     *
     * @param cameraIndex index of the camera.
     */
    public void setCameraIndex(@NonNull CameraIndex cameraIndex) {
        this.cameraIndex = cameraIndex.getIndex();
        restart();
    }
    //endregion

    //region Helpers

    /**
     * Copies the data from cache into data.
     *
     * @param data  destination array of length {@link HistogramWidgetModel#MAX_DATA_LENGTH}
     * @param cache source array of length {@link LineChartView#NUM_DATA_POINTS}
     */
    private static void copyData(final float[] data, final short[] cache) {
        for (int i = INDEX_START; i < MAX_DATA_LENGTH - INDEX_END; i++) {
            data[i - INDEX_START] = cache[i];
        }
    }

    //endregion

    //region User interaction

    /**
     * Set whether the histogram is enabled.
     *
     * @param enabled `true` to enable, `false` to disable.
     * @return Completable representing the success/failure of the set action.
     */
    public Completable setHistogramEnabled(boolean enabled) {
        return djiSdkModel.setValue(histogramEnabledKey, enabled);
    }
    //endregion
}
