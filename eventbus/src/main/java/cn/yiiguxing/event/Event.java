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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event
 * <p/>
 * Created by Yii.Guxing on 16/1/27.
 */
public class Event {

    private static final int MAX_POOL_SIZE = 50;
    private static final Object sPoolSync = new Object();
    private static Event sPool;
    private static int sPoolSize = 0;

    Event next;

    private EventBus mTarget;

    private String mTag;
    private Object[] mData;

    private volatile AtomicBoolean mRecycled = new AtomicBoolean(false);
    private volatile AtomicInteger mInUse = new AtomicInteger();

    @NonNull
    private static Event obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Event e = sPool;
                sPool = e.next;
                e.next = null;

                e.mInUse.set(0);
                e.mRecycled.set(false);
                e.mTarget = null;
                e.mTag = null;
                e.mData = null;

                sPoolSize--;
                return e;
            }
        }

        return new Event();
    }

    @NonNull
    public static Event obtain(Event orig) {
        return obtain(orig.mTarget, orig.mTag, orig.mData);
    }

    @NonNull
    public static Event obtain(EventBus target) {
        return obtain(target, EventBus.DEFAULT_TAG, (Object[]) null);
    }

    @NonNull
    public static Event obtain(EventBus target, Object... data) {
        return obtain(target, EventBus.DEFAULT_TAG, data);
    }

    @NonNull
    public static Event obtain(EventBus target, String tag, Object... data) {
        Event event = obtain();
        event.mTarget = target;
        event.mTag = tag;
        event.mData = data;

        return event;
    }

    private Event() {
    }

    public Event(@NonNull EventBus target) {
        this(target, null, (Object[]) null);
    }

    public Event(@NonNull EventBus target, String tag, Object... data) {
        this.mTarget = target;
        this.mTag = tag;
        this.mData = data;
    }

    public final EventBus getTarget() {
        return mTarget;
    }

    @Nullable
    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        if (isInUse()) {
            throw new IllegalStateException(
                    "Cannot change tag because this event is still in use.");
        }

        mTag = tag;
    }

    @Nullable
    public Object[] getData() {
        return mData;
    }

    public void setData(Object... data) {
        if (isInUse()) {
            throw new IllegalStateException(
                    "Cannot change data because this event is still in use.");
        }

        mData = data;
    }

    boolean isInUse() {
        return mInUse.get() > 0;
    }

    void requestUse() {
        mInUse.getAndIncrement();
    }

    public boolean isRecycled() {
        return mRecycled.get();
    }

    /**
     * Post this event.
     */
    public void post() {
        mTarget.post(this);
    }

    void requestRecycle() {
        if (isRecycled()) {
            throw new IllegalStateException("Event already recycled.");
        }

        int use = mInUse.decrementAndGet();
        if (use <= 0)
            recycle();
    }

    void recycle() {
        requestUse();
        mRecycled.set(true);
        mTarget = null;
        mTag = null;
        mData = null;

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                "mTarget=" + mTarget +
                ", mTag='" + mTag + '\'' +
                ", mData=" + Arrays.toString(mData) +
                ", mRecycled=" + mRecycled +
                ", mInUse=" + mInUse +
                '}';
    }
}
