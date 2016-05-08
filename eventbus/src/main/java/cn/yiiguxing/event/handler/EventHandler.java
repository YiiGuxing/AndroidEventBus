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

import android.support.annotation.NonNull;

import cn.yiiguxing.event.Event;
import cn.yiiguxing.event.Subscriber;

/**
 * EventHandler
 * <p/>
 * Created by Yii.Guxing on 16/1/27.
 */
public interface EventHandler {
    void handlerEvent(@NonNull Subscriber subscriber, @NonNull Event event);
}
