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

package dji.ux.beta.core.base;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import dji.common.error.DJIError;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.KeyListener;
import dji.keysdk.callback.SetCallback;
import dji.log.DJILog;
import dji.sdk.camera.Camera;
import dji.thirdparty.io.reactivex.BackpressureStrategy;
import dji.thirdparty.io.reactivex.Completable;
import dji.thirdparty.io.reactivex.Flowable;
import dji.thirdparty.io.reactivex.FlowableEmitter;
import dji.thirdparty.io.reactivex.Single;

/**
 * Encapsulates communication with SDK KeyManager for SDKKeys.
 */
public class DJISDKModel {

    //region Fields
    private static final String TAG = "DJISDKModel";
    private static final int MAX_COMPONENT_INDEX = 10;
    private Map<Object, List<KeyListener>> keyListeners;
    //endregion

    private DJISDKModel() {
        keyListeners = new ConcurrentHashMap<>();
    }

    public static DJISDKModel getInstance() {
        return DJISDKModel.SingletonHolder.instance;
    }

    /**
     * Check if DJISDKModel is available
     *
     * @return Boolean value for availability
     */
    public boolean isAvailable() {
        return isKeyManagerAvailable();
    }
    //endregion

    //region DependentKeys Control

    /**
     * Stops observing changes of all keys registered for the given listener.
     *
     * @param listener The listener to unregister.
     */
    public void removeListener(@NonNull final Object listener) {
        // Stop listening to cache
        removeKeyListeners(listener).subscribe();
    }

    /**
     * Subscribes the listener object to all changes of value on the given  key.
     *
     * @param key      A valid value-based key (get, set and/or action)
     * @param listener Listener that is subscribing.
     * @return A flowable that emits objects based on a key.
     */
    @NonNull
    public Flowable<Object> addListener(@NonNull final DJIKey key, @NonNull final Object listener) {
        return Flowable.create(emitter -> {
            if (!isKeyManagerAvailable()) {
                emitter.onError(getKeyManagerException());
                emitter.onComplete();
                return;
            }

            DJILog.d(TAG, "Registering key " + key.toString() + " for " + listener.getClass().getName());
            registerKey(emitter, key, listener);
        }, BackpressureStrategy.LATEST)
                .subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Performs a get on a gettable key, pulling the information from the product if
     * necessary.
     *
     * @param key A valid gettable key.
     * @return A single that emits one object based on a key.
     */
    @NonNull
    public Single<Object> getValue(@NonNull final DJIKey key) {
        return Single.create(emitter -> {
            if (!isKeyManagerAvailable()) {
                emitter.onError(getKeyManagerException());
                return;
            }

            getKeyManager().getValue(key, new GetCallback() {
                @Override
                public void onSuccess(@NonNull Object value) {
                    DJILog.d(TAG, "Got current value for  key " + key.toString());
                    emitter.onSuccess(value);
                }

                @Override
                public void onFailure(@NonNull DJIError djiError) {
                    DJILog.e(TAG, "Failure getting key " + key.toString() + ". " + djiError.getDescription());
                }
            });
        }).subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Returns the latest known value if available for the key. Does not pull it from
     * the product if unavailable.
     *
     * @param key An instance of DJIKey.
     * @return The value associated with the key.
     */
    @Nullable
    public Object getCacheValue(@NonNull final DJIKey key) {
        if (!isKeyManagerAvailable()) {
            return null;
        }

        return getKeyManager().getValue(key);
    }

    /**
     * Performs a set on a settable key, changing attributes on the connected product.
     *
     * @param key   A valid settable key.
     * @param value A value object relevant to the given key.
     * @return A completable indicating success/error setting the value.
     */
    @NonNull
    public Completable setValue(@NonNull final DJIKey key, @NonNull final Object value) {
        return Completable.create(
                emitter -> {
                    if (!isKeyManagerAvailable()) {
                        emitter.onError(getKeyManagerException());
                        return;
                    }

                    getKeyManager().setValue(key, value, new SetCallback() {
                        @Override
                        public void onSuccess() {
                            emitter.onComplete();
                        }

                        @Override
                        public void onFailure(@NonNull DJIError djiError) {
                            emitter.onError(new UXSDKError(djiError));
                        }
                    });
                }).subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Performs an action on an actionable key.
     *
     * @param key       A valid actionable key.
     * @param arguments Optional arguments relevant to the specific key.
     * @return A completable indicating success/error performing the action.
     */
    @NonNull
    public Completable performAction(@NonNull final DJIKey key, final Object... arguments) {
        return Completable.create(emitter -> {
            if (!isKeyManagerAvailable()) {
                emitter.onError(getKeyManagerException());
                return;
            }

            getKeyManager().performAction(key, new ActionCallback() {
                @Override
                public void onSuccess() {
                    DJILog.d(TAG, "Performed action for  key " + key.toString());
                    emitter.onComplete();
                }

                @Override
                public void onFailure(@NonNull DJIError djiError) {
                    DJILog.e(TAG, "Failure performing action key " + key.toString() + ". " + djiError.getDescription());
                    emitter.onError(new UXSDKError(djiError));
                }
            }, arguments);
        }).subscribeOn(SchedulerProvider.computation());
    }

    /**
     * Determines if a key is supported by the connected product.
     *
     * @param key Key to be check on current product.
     * @return `true` if the key is supported.
     */
    public boolean isKeySupported(DJIKey key) {
        if (isKeyManagerAvailable()) {
            return getKeyManager().isKeySupported(key);
        }

        return false;
    }

    /**
     * Create a lens key or camera key, depending on whether the product has multi lens support.
     *
     * @param keyName           A valid CameraKey name
     * @param componentIndex    The index of the camera component.
     * @param subComponentIndex The index of the camera sub-component.
     * @return A camera key.
     */
    @NonNull
    public CameraKey createLensKey(@NonNull String keyName,
                                   @IntRange(from = 0, to = MAX_COMPONENT_INDEX) int componentIndex,
                                   @IntRange(from = 0, to = MAX_COMPONENT_INDEX) int subComponentIndex) {
        boolean isMultiLensCameraSupported = false;
        if (getCacheValue(CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, componentIndex)) != null) {
            isMultiLensCameraSupported = (boolean) getCacheValue(CameraKey.create(CameraKey.IS_MULTI_LENS_CAMERA_SUPPORTED, componentIndex));
        }
        if (!isMultiLensCameraSupported) {
            String displayName = "";
            if (getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME)) != null) {
                displayName = (String) getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME));
            }
            if (Camera.DisplayNameXT2_VL.equals(displayName) ||
                    Camera.DisplayNameMavic2EnterpriseDual_VL.equals(displayName)) {
                if (subComponentIndex == Camera.XT2_IR_CAMERA_INDEX) {
                    return CameraKey.create(keyName, subComponentIndex);
                } else {
                    return CameraKey.create(keyName, componentIndex);
                }
            } else {
                return CameraKey.create(keyName, componentIndex);
            }
        } else {
            return CameraKey.createLensKey(keyName, componentIndex, subComponentIndex);
        }
    }
    //endregion

    //region Class Helpers
    @Nullable
    private KeyManager getKeyManager() {
        return KeyManager.getInstance();
    }
    //endregion

    private void registerKey(@NonNull final FlowableEmitter<Object> emitter,
                             @NonNull final DJIKey key,
                             @NonNull final Object listener) {
        // Get current value
        getKeyManager().getValue(key, new GetCallback() {
            @Override
            public void onSuccess(@NonNull Object value) {
                DJILog.d(TAG, "Got current value for  key " + key.toString());
                emitter.onNext(value);
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                DJILog.d(TAG, "Failure getting key " + key.toString() + ". " + djiError.getDescription());
            }
        });
        // Start listening to changes
        KeyListener keyListener = (oldValue, newValue) -> {
            DJILog.d(TAG, "Update on key " + key.toString());
            if (newValue != null && !emitter.isCancelled()) {
                emitter.onNext(newValue);
            }
        };
        getKeyManager().addListener(key, keyListener);
        List<KeyListener> currentList = this.keyListeners.get(listener);
        if (currentList == null) {
            currentList = new CopyOnWriteArrayList<>();
        }
        currentList.add(keyListener);
        keyListeners.put(listener, currentList);
    }

    private Flowable<Boolean> removeKeyListeners(final Object listener) {
        if (listener == null) {
            return Flowable.just(true);
        }
        return Flowable.just(true)
                .doOnSubscribe(subscription -> {
                    List<KeyListener> currentList = keyListeners.remove(listener);
                    if (currentList != null) {
                        for (KeyListener eachListener : currentList) {
                            if (KeyManager.getInstance() != null) {
                                KeyManager.getInstance().removeListener(eachListener);
                            }
                        }
                    }
                });
    }

    private boolean isKeyManagerAvailable() {
        return KeyManager.getInstance() != null;
    }

    private IllegalStateException getKeyManagerException() {
        return new IllegalStateException("KeyManager is not available yet");
    }

    //region Constructor
    private static class SingletonHolder {
        private static DJISDKModel instance = new DJISDKModel();
    }
    //endregion
}