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

package cn.yiiguxing.event.handler;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import cn.yiiguxing.event.Event;
import cn.yiiguxing.event.Subscriber;

/**
 * UIThreadEventHandler
 * <p/>
 * Created by Yii.Guxing on 16/1/27.
 */
public class UIThreadEventHandler extends SimpleEventHandler {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public final void handlerEvent(@NonNull final Subscriber subscriber, @NonNull final Event event) {
        if (mHandler.getLooper() != Looper.myLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onHandlerEvent(subscriber, event);
                }
            });
        } else {
            onHandlerEvent(subscriber, event);
        }
    }

    @MainThread
    protected void onHandlerEvent(@NonNull final Subscriber subscriber, @NonNull final Event event) {
        super.handlerEvent(subscriber, event);
    }

}
