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

/**
 * SubscriberWrapper
 * <p/>
 * Created by Yii.Guxing on 16/1/28.
 */
class SubscriberWrapper implements Subscriber {

    private Subscriber mSubscriber;

    public SubscriberWrapper(@NonNull Subscriber subscriber) {
        mSubscriber = subscriber;
    }

    @Override
    public void onEvent(@NonNull Event event) {
        mSubscriber.onEvent(event);
        event.requestRecycle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriberWrapper)) return false;

        SubscriberWrapper that = (SubscriberWrapper) o;

        return mSubscriber.equals(that.mSubscriber);
    }

    @Override
    public int hashCode() {
        return mSubscriber.hashCode();
    }

    @Override
    public String toString() {
        return "Subscriber:{ " + mSubscriber.getClass().getSimpleName() + " }";
    }
}
