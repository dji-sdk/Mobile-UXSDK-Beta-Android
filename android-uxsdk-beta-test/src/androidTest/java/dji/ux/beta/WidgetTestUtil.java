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

package dji.ux.beta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.ProductKey;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.Single;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.WidgetModel;
import dji.ux.beta.core.communication.BroadcastValues;
import dji.ux.beta.core.communication.CameraKeys;
import dji.ux.beta.core.communication.GlobalPreferenceKeys;
import dji.ux.beta.core.communication.MessagingKeys;
import dji.ux.beta.core.communication.ModelValue;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.communication.UXKey;
import dji.ux.beta.core.communication.UXKeys;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

public final class WidgetTestUtil {
    private WidgetTestUtil() {
        // Do nothing
    }

    /**
     * Initialize the mocking functionality and the keys in the base widget model.
     *
     * @param djiSdkModel Mocked DJISDKModel
     */
    public static void initialize(@NonNull DJISDKModel djiSdkModel) {
        //Mock the communication with the underlying layers and return true for availability
        Mockito.when(djiSdkModel.isAvailable()).thenReturn(true);
        Mockito.when(djiSdkModel.createLensKey(anyString(), anyInt(), anyInt()))
                .thenAnswer((Answer<CameraKey>) invocation -> {
                    String keyName = invocation.getArgument(0, String.class);
                    int componentIndex = invocation.getArgument(1, Integer.class);
                    return CameraKey.create(keyName, componentIndex);
                });
        //Initialize the UXKey classes
        UXKeys.addNewKeyClass(GlobalPreferenceKeys.class);
        UXKeys.addNewKeyClass(CameraKeys.class);
        UXKeys.addNewKeyClass(MessagingKeys.class);
    }

    /**
     * Initialize the mocking functionality and the keys in the base widget model.
     *
     * @param djiSdkModel     Mocked DJISDKModel
     * @param widgetModel     the widgetModel being tested
     * @param connectionValue product connection
     */
    public static void initialize(@NonNull DJISDKModel djiSdkModel,
                                  @NonNull WidgetModel widgetModel,
                                  boolean connectionValue) {
        //Mock the communication with the underlying layers and return true for availability
        initialize(djiSdkModel);
        // Initialize connection key used by base widget model and mock to return true
        ProductKey productKey = ProductKey.create(ProductKey.CONNECTION);
        Mockito.when(djiSdkModel.addListener(productKey, widgetModel)).thenReturn(Flowable.just(connectionValue));
        //Initialize the UXKey classes
        UXKeys.addNewKeyClass(GlobalPreferenceKeys.class);
        UXKeys.addNewKeyClass(CameraKeys.class);
        UXKeys.addNewKeyClass(MessagingKeys.class);
    }

    /**
     * Use this method to set a test value to be emitted after given delay for given DJIKey and widget model.
     *
     * @param widgetModel The widgetModel being tested
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param testValue   Object value to be emitted after given delay
     * @param delay       Integer delay
     * @param timeUnit    TimeUnit for the delay
     */
    public static void setEmittedValue(@NonNull WidgetModel widgetModel,
                                       @NonNull DJISDKModel djiSdkModel,
                                       @NonNull DJIKey key,
                                       @NonNull Object testValue,
                                       int delay,
                                       @NonNull TimeUnit timeUnit) {
        // Create a flowable that emits required item after the given delay
        Flowable<Object> flowable = Flowable.timer(delay, timeUnit, SchedulerProvider.computation()).map(mapValue -> testValue);
        // Mock the flowable returned on binding of the widget model's keys
        Mockito.when(djiSdkModel.addListener(key, widgetModel)).thenReturn(flowable);
    }

    /**
     * Use this method to set a list of values to be emitted at a certain interval for given
     * DJIKey and widget model.
     *
     * @param widgetModel The widgetModel being tested
     * @param djisdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param testValues  List of values to emit
     * @param delay       Integer delay before emitting the first item
     * @param interval    Integer interval for emitting each item
     * @param timeUnit    TimeUnit for the delay
     */
    public static void setEmittedValues(@NonNull WidgetModel widgetModel,
                                        @NonNull DJISDKModel djisdkModel,
                                        @NonNull DJIKey key,
                                        @NonNull List<?> testValues,
                                        int delay,
                                        int interval,
                                        @NonNull TimeUnit timeUnit) {
        Flowable<Object> testValuesFlowable = Flowable.fromIterable(testValues);
        Flowable<Long> timer = Flowable.interval(delay, interval, timeUnit, SchedulerProvider.computation());
        Flowable<Object> valuesInterval = Flowable.zip(testValuesFlowable, timer, (values, count) -> values);

        Mockito.when(djisdkModel.addListener(key, widgetModel)).thenReturn(valuesInterval);
    }

    /**
     * Use this method to set a test value to be emitted after given delay for given UXKey.
     *
     * @param keyedStore Mocked ObservableInMemoryKeyedStore
     * @param key        UXKey to be tested
     * @param testValue  Object value to be emitted after given delay
     * @param delay      Integer delay
     * @param timeUnit   TimeUnit for the delay
     */
    public static void setEmittedValue(@NonNull ObservableInMemoryKeyedStore keyedStore,
                                       @NonNull UXKey key,
                                       @NonNull Object testValue,
                                       int delay,
                                       @NonNull TimeUnit timeUnit) {
        // Create a flowable that emits required item after the given delay
        BroadcastValues broadcastValues = new BroadcastValues(null, new ModelValue(testValue));
        Flowable<BroadcastValues> flowable = Flowable.timer(delay, timeUnit, SchedulerProvider.computation()).map(mapValue -> broadcastValues);
        // Mock the flowable returned on binding of the widget model's keys
        Mockito.when(keyedStore.addObserver(key)).thenReturn(flowable);
    }

    /**
     * Use this method to return empty flowables for DJIKey bindings that need to
     * be ignored for a given test.
     *
     * @param widgetModel The widgetModel being tested
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be ignored
     */
    public static void setEmptyValue(@NonNull WidgetModel widgetModel,
                                     @NonNull DJISDKModel djiSdkModel,
                                     @NonNull DJIKey key) {
        // Return an empty flowable on binding of the widget model's keys
        Mockito.when(djiSdkModel.addListener(key, widgetModel)).thenReturn(Flowable.empty());
    }

    /**
     * Use this method to return empty flowables for UXKey bindings that need to
     * be ignored for a given test.
     *
     * @param keyedStore Mocked ObservableInMemoryKeyedStore
     * @param key        UXKey to be ignored
     */
    public static void setEmptyValue(@NonNull ObservableInMemoryKeyedStore keyedStore, @NonNull UXKey key) {
        // Return an empty flowable on binding of the UX key
        Mockito.when(keyedStore.addObserver(key)).thenReturn(Flowable.empty());
    }

    /**
     * Use this method to simulate the {@link DJISDKModel#isKeySupported(DJIKey)} method
     *
     * @param djisdkModel    Mocked DJISDKModel
     * @param djiKey         DJIKey for which it should be checked
     * @param isKeySupported simulated value true - for supported false - for not supported
     */
    public static void setKeySupported(@NonNull DJISDKModel djisdkModel, @NonNull DJIKey djiKey, boolean isKeySupported) {
        Mockito.when(djisdkModel.isKeySupported(djiKey)).thenReturn(isKeySupported);
    }

    /**
     * Use this method to set value for the expected action completable
     *
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be ignored
     * @param object      Data object to set to action key
     * @param error       Throwable error object
     */
    public static void setEmittedAction(@NonNull DJISDKModel djiSdkModel, @NonNull DJIKey key,
                                        @NonNull Object object, @Nullable Throwable error) {
        Completable completable = Completable.create(emitter -> {
            if (error != null) {
                emitter.onError(error);
            } else {
                emitter.onComplete();
            }

        }).subscribeOn(SchedulerProvider.computation());
        Mockito.when(djiSdkModel.performAction(key, object)).thenReturn(completable);
    }

    /**
     * Use this method to set value for the expected action completable
     *
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param error       Throwable error object
     */
    public static void setEmittedAction(@NonNull DJISDKModel djiSdkModel, @NonNull DJIKey key,
                                        @Nullable Throwable error) {
        Completable completable = Completable.create(emitter -> {
            if (error != null) {
                emitter.onError(error);
            } else {
                emitter.onComplete();
            }

        }).subscribeOn(SchedulerProvider.computation());
        Mockito.when(djiSdkModel.performAction(key)).thenReturn(completable);
    }

    /**
     * Use this method to set value for the expected action completable
     *
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param delay       Integer delay
     * @param error       Throwable error object
     */
    public static void setEmittedAction(@NonNull DJISDKModel djiSdkModel, @NonNull DJIKey key,
                                        int delay, @Nullable Throwable error) {
        Completable completable = Completable.timer(delay, TimeUnit.SECONDS).andThen(Completable.create(emitter -> {
            if (error != null) {
                emitter.onError(error);
            } else {
                emitter.onComplete();
            }

        })).subscribeOn(SchedulerProvider.computation());
        Mockito.when(djiSdkModel.performAction(key)).thenReturn(completable);
    }

    /**
     * Use this method to set value of the expected set value complete
     *
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param object      Data object to set to set key
     * @param error       Throwable error object
     */
    public static void setEmittedSetValue(@NonNull DJISDKModel djiSdkModel, @NonNull DJIKey key,
                                          @NonNull Object object, @Nullable Throwable error) {
        Completable completable = Completable.create(emitter -> {
            if (error != null) {
                emitter.onError(error);
            } else {
                emitter.onComplete();
            }

        }).subscribeOn(SchedulerProvider.computation());
        Mockito.when(djiSdkModel.setValue(key, object)).thenReturn(completable);
    }

    /**
     * Use this method to set value of the expected set value complete
     *
     * @param keyedStore Mocked ObservableInMemoryKeyedStore
     * @param uxKey      DJIKey to be tested
     * @param object     Data object to set to set key
     * @param error      Throwable error object
     */
    public static void setEmittedSetValue(@NonNull ObservableInMemoryKeyedStore keyedStore,
                                          @NonNull UXKey uxKey, @NonNull Object object,
                                          @Nullable Throwable error) {
        Completable completable = Completable.create(emitter -> {
            if (error != null) {
                emitter.onError(error);
            } else {
                emitter.onComplete();
            }

        }).subscribeOn(SchedulerProvider.computation());
        Mockito.when(keyedStore.setValue(uxKey, object)).thenReturn(completable);
    }

    /**
     * Use this method to set value of the expected get value single
     *
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param object      Data object to be returned by get key
     * @param error       Throwable error object
     */
    public static void setEmittedGetValue(@NonNull DJISDKModel djiSdkModel, @NonNull DJIKey key,
                                          @Nullable Object object, @Nullable Throwable error) {
        Single<Object> single = Single.create(emitter -> {
            if (error != null) {
                emitter.onError(error);
            } else {
                emitter.onSuccess(object);
            }

        }).subscribeOn(SchedulerProvider.computation());
        Mockito.when(djiSdkModel.getValue(key)).thenReturn(single);
    }

    /**
     * Use this method to set value of the expected get cached value object
     *
     * @param djiSdkModel Mocked DJISDKModel
     * @param key         DJIKey to be tested
     * @param object      Data object to be returned by get key
     */
    public static void setEmittedGetCachedValue(@NonNull DJISDKModel djiSdkModel, @NonNull DJIKey key,
                                                @Nullable Object object) {
        Mockito.when(djiSdkModel.getCacheValue(key)).thenReturn(object);
    }

    public static void setEmptyFlatCameraModeValues(WidgetModel widgetModel, DJISDKModel djiSdkModel, int cameraIndex) {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.MODE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.SHOOT_PHOTO_MODE, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED, cameraIndex));
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE, cameraIndex));
    }
}
