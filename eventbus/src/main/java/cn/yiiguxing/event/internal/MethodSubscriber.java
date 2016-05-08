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

package cn.yiiguxing.event.internal;

import android.support.annotation.NonNull;

import cn.yiiguxing.event.Event;
import cn.yiiguxing.event.UnstableSubscriber;

/**
 * MethodSubscriber
 * <p/>
 * Created by Yii.Guxing on 16/1/29.
 */
public abstract class MethodSubscriber<T> extends UnstableSubscriber<T> {

    private final String method;

    public MethodSubscriber(@NonNull T unstable, @NonNull String method) {
        super(unstable);
        this.method = method;
    }

    public abstract boolean accept(@NonNull Event event);

    protected void onEvent(@NonNull T unstable, @NonNull Event event) {
        try {
            handlerEvent(unstable, event);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    protected abstract void handlerEvent(@NonNull T unstable, @NonNull Event event)
            throws Throwable;

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        MethodSubscriber<?> that = (MethodSubscriber<?>) o;
        return method.equals(that.method);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodSubscriber{ " + getUnstableClassName() + "." + method + " }";
    }

}
