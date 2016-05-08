/*
 * Copyright (C) 2016 Yii.Guxing <yii.guxing@gmail.com>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.yiiguxing.eventbus;

import android.util.Log;

/**
 * LogUtil
 * <p/>
 * Created by tinkling on 16/2/1.
 */
public class LogUtil {

    private static final String TAG = "EventBus";

    public static int v(String msg, Object... args) {
        return Log.v(TAG, String.format(msg, args));
    }

    public static int d(String msg, Object... args) {
        return Log.d(TAG, String.format(msg, args));
    }

    public static int i(String msg, Object... args) {
        return Log.i(TAG, String.format(msg, args));
    }

    public static int w(String msg, Object... args) {
        return Log.w(TAG, String.format(msg, args));
    }

    public static int w(String msg, Throwable tr, Object... args) {
        return Log.w(TAG, String.format(msg, args), tr);
    }

    public static int e(String msg, Object... args) {
        return Log.e(TAG, String.format(msg, args));
    }

    public static int e(String msg, Throwable tr, Object... args) {
        return Log.e(TAG, String.format(msg, args), tr);
    }
}
