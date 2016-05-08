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

package cn.yiiguxing.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * UnstableSubscriber
 * <p/>
 * Created by Yii.Guxing on 16/1/27.
 */
public abstract class UnstableSubscriber<T> implements Subscriber {

    private final Reference<T> mUnstableRef;
    private final int mHashCode;
    private final String mName;

    public UnstableSubscriber(@NonNull T unstable) {
        mUnstableRef = new WeakReference<>(unstable);
        mHashCode = unstable.hashCode();
        mName = unstable.getClass().getCanonicalName();
    }

    @Nullable
    public T getUnstable() {
        return mUnstableRef.get();
    }

    protected String getUnstableClassName() {
        return mName;
    }

    @Override
    public void onEvent(@NonNull Event event) {
        T unstable = getUnstable();
        if (unstable != null) {
            onEvent(unstable, event);
        }

        event.requestRecycle();
    }

    protected abstract void onEvent(@NonNull T unstable, @NonNull Event event);

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof UnstableSubscriber &&
                mHashCode == ((UnstableSubscriber<?>) o).mHashCode);
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public String toString() {
        return "Subscriber:{ [UNSTABLE]" + mName + " }";
    }
}
