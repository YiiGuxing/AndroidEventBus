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

package cn.yiiguxing.event.compiler;

import java.util.Arrays;

import javax.lang.model.type.TypeMirror;

import cn.yiiguxing.event.ThreadMode;

/**
 * BindingMethod
 * <p/>
 * Created by Yii.Guxing on 16/1/29.
 */
class BindingMethod {

    final String name;
    final String tagParamName;
    final TypeMirror[] parameterTypes;
    final String signature;
    final String[] tags;
    final ThreadMode mode;

    public BindingMethod(String name,
                         String tagParamName,
                         TypeMirror[] parameterTypes,
                         String signature,
                         String[] tags,
                         ThreadMode mode) {
        this.name = name;
        this.tagParamName = tagParamName;
        this.parameterTypes = parameterTypes;
        this.signature = signature;
        this.tags = tags;
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "BindingMethod{" +
                "name='" + name + '\'' +
                ", tagParamName='" + tagParamName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", signature='" + signature + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", mode=" + mode +
                '}';
    }
}
