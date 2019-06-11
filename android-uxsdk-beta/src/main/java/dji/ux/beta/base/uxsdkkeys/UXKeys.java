/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.base.uxsdkkeys;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import dji.log.DJILog;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to create valid UX keys from given String UXParamKeys and indices.
 * This class can be extended by any subclasses to define custom UX keys.
 */
public class UXKeys {
    private static final String TAG = "UXKeys";
    private final static int DEFAULT_INDEX = 0;
    private static Map<String, UXKey> keysPathMap = new ConcurrentHashMap<>();
    private static Map<String, Class> keyValueMap = new ConcurrentHashMap<>();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.PARAMETER })
    protected @interface UXParamKey {
        /**
         * The type of param that the method associated to this key will take or return
         */
        @NonNull Class type();
    }

    protected UXKeys() {
        //Do Nothing
    }

    private static void initializeKeyValueTypes(Class<? extends UXKeys> clazz) {
        if (clazz == null) return;
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (field.getType() == String.class && isStatic(field.getModifiers()) && (field.isAnnotationPresent(
                UXParamKey.class))) {
                try {
                    String paramKey = (String) field.get(null);
                    UXParamKey paramKeyAnnotation = field.getAnnotation(UXParamKey.class);
                    addKeyValueTypeToMap(paramKey, paramKeyAnnotation.type());
                } catch (Exception e) {
                    DJILog.e(TAG, e.getMessage());
                }
            }
        }
    }
    /**
     * Use this function to initialize any classes containing UXParamKeys
     *
     * @param componentClass Class which extends the `UXKeys` class and contains UXParamKeys
     */
    public static void addNewKeyClass(@NonNull Class<? extends UXKeys> componentClass) {
        initializeKeyValueTypes(componentClass);
    }

    /**
     * This functions allows creation of a UXKey using a param key (String)
     *
     * @param key String param key with UXParamKey annotation defined in class UXKeys or its children
     * @return UXKey if value-type of key has been registered - null otherwise
     */
    public static UXKey create(@NonNull String key) {
        return create(key, DEFAULT_INDEX);
    }

    /**
     * This functions allows creation of a UXKey using a param key (String) and an index (int)
     *
     * @param key String param key with UXParamKey annotation defined in class UXKeys or its children
     * @param index Index of the component the key is being created for - default is 0
     * @return UXKey if value-type of key has been registered - null otherwise
     */
    @CheckResult
    public static UXKey create(@NonNull String key, int index) {
        String keyPath = producePathFromElements(key, index);
        UXKey uxKey = getCache(keyPath);
        if (uxKey == null) {
            Class valueType = keyValueMap.get(key);
            if (valueType != null) {
                uxKey = new UXKey(key, valueType, keyPath);
                putCache(keyPath, uxKey);
            }
        }
        return uxKey;
    }

    /**
     * Use this function to initialize any custom keys created
     *
     * @param key String key with UXParamKey annotation to be initialized
     * @param valueType Non-primitive class value-type of the key to be initialized (eg. Integer, Boolean etc)
     */
    private static void addKeyValueTypeToMap(@NonNull String key, @NonNull Class valueType) {
        if (!keyValueMap.containsKey(key)) {
            keyValueMap.put(key, valueType);
        }
    }

    private static String producePathFromElements(@NonNull String param, int index) {
        return param + "/" + Integer.toString(index);
    }

    private static UXKey getCache(String keyStr) {
        if (keyStr != null) {
            return keysPathMap.get(keyStr);
        } else {
            return null;
        }
    }

    private static void putCache(String keyStr, UXKey key) {
        if (keyStr != null && key != null && !keysPathMap.containsKey(keyStr)) {
            keysPathMap.put(keyStr, key);
        }
    }

    private static boolean isStatic(int modifiers) {
        return ((modifiers & Modifier.STATIC) != 0);
    }
}
