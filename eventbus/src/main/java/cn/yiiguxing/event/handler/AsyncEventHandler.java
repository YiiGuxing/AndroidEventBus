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

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.concurrent.Executor;

import cn.yiiguxing.event.Event;
import cn.yiiguxing.event.Subscriber;

/**
 * AsyncEventHandler
 * <p/>
 * Created by Yii.Guxing on 16/1/27.
 */
public class AsyncEventHandler extends SimpleEventHandler {

    private Executor mExecutor;

    public AsyncEventHandler() {
        this(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public AsyncEventHandler(@NonNull Executor mExecutor) {
        this.mExecutor = mExecutor;
    }

    @Override
    public void handlerEvent(@NonNull final Subscriber subscriber, @NonNull final Event event) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                onHandlerEvent(subscriber, event);
                return null;
            }
        }.executeOnExecutor(mExecutor);
    }

    @WorkerThread
    protected void onHandlerEvent(@NonNull final Subscriber subscriber, @NonNull final Event event) {
        super.handlerEvent(subscriber, event);
    }

}
