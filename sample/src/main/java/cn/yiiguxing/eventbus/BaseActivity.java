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

import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;

import cn.yiiguxing.event.Event;
import cn.yiiguxing.event.EventBus;
import cn.yiiguxing.event.ThreadMode;
import cn.yiiguxing.event.annotation.Subscribe;

/**
 * BaseActivity
 * <p/>
 * Created by tinkling on 16/2/1.
 */
public class BaseActivity extends AppCompatActivity {

    // Default tag & default thread mode.
    @Subscribe
    void handleEventBase(Event event) {
        LogUtil.d("handleEventBase - %s", event);
    }

    // Multi-tag & async thread mode.
    @Subscribe(tag = {EventBus.DEFAULT_TAG, "tag"}, mode = ThreadMode.ASYNC)
    void handleMultidataEventBase(int i, boolean b, String s, int[] arr) {
        LogUtil.d("handleMultidataEventBase - Thread:%s, int:%d, boolean:%s, string:%s, array:%s",
                Thread.currentThread().getName(), i, b, s, Arrays.toString(arr));
    }

}
