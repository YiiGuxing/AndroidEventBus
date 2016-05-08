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

package cn.yiiguxing.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a method to receive event:
 * <pre><code>
 * {@literal @}Subscribe(tag = "tag") void onEvent(...) {
 *     // TODO handle event.
 * }
 * </code></pre>or
 * <pre><code>
 * {@literal @}Subscribe(tag = {"tag1", "tag2", "tag3"}, mode = ThreadMode.MAIN) void onEvent(...) {
 *     // TODO handle event.
 * }
 * </code></pre>
 * <p/>
 * Created by Yii.Guxing on 16/1/28.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Subscribe {
    /**
     * Tag for event.
     */
    String[] tag() default {};

    /**
     * Thread mode.
     */
    cn.yiiguxing.event.ThreadMode mode() default cn.yiiguxing.event.ThreadMode.DEFAULT;
}
