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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import cn.yiiguxing.event.annotation.Generated;

/**
 * BindingClass
 * <p/>
 * Created by Yii.Guxing on 16/1/29.
 */

class BindingClass {

    private static final String CLASS_NAME_EVENT = "cn.yiiguxing.event.Event";

    private static final ClassName CLASS_EVENT_BUS =
            ClassName.get("cn.yiiguxing.event", "EventBus");
    private static final ClassName CLASS_EVENT =
            ClassName.get("cn.yiiguxing.event", "Event");
    private static final ClassName CLASS_SUBSCRIBE_REGISTER =
            ClassName.get("cn.yiiguxing.event.internal", "SubscribeRegister");
    private static final ClassName CLASS_METHOD_SUBSCRIBER =
            ClassName.get("cn.yiiguxing.event.internal", "MethodSubscriber");
    private static final ClassName CLASS_THREAD_MODE =
            ClassName.get("cn.yiiguxing.event", "ThreadMode");
    private static final ClassName CLASS_NONNULL =
            ClassName.get("android.support.annotation", "NonNull");

    private final String classPackage;
    private final String className;
    private final String targetClass;

    private final Set<BindingMethod> methods = new LinkedHashSet<>();

    private String parentBindingClass;

    BindingClass(String classPackage, String className, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClass = targetClass;
    }

    void setParentBindingClass(String parentBindingClass) {
        this.parentBindingClass = parentBindingClass;
    }

    void addBindingMethod(BindingMethod method) {
        methods.add(method);
    }

    String getRegisterClassName() {
        return classPackage + "." + className;
    }

    JavaFile brewJava() {
        AnnotationSpec as = AnnotationSpec
                .builder(Generated.class)
                .addMember("value", "$S", EventSubscriberProcessor.class.getCanonicalName())
                .build();

        TypeSpec.Builder register = TypeSpec.classBuilder(className)
                .addAnnotation(as)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T", ClassName.bestGuess(targetClass)));

        if (parentBindingClass != null) {
            register.superclass(ParameterizedTypeName.get(ClassName.bestGuess(parentBindingClass),
                    TypeVariableName.get("T")));
        } else {
            register.addSuperinterface(ParameterizedTypeName.get(CLASS_SUBSCRIBE_REGISTER,
                    TypeVariableName.get("T")));
        }

        register.addMethod(createRegisterMethod());

        return JavaFile.builder(classPackage, register.build())
                .addFileComment("Generated code from EventBus. Do not modify!")
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private MethodSpec createRegisterMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("register")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CLASS_EVENT_BUS, "eventBus", Modifier.FINAL)
                .addParameter(TypeVariableName.get("T"), "target", Modifier.FINAL);

        if (parentBindingClass != null) {
            method.addStatement("super.register(eventBus, target)");
        }
        method.addStatement("$T<T> subscriber", CLASS_METHOD_SUBSCRIBER);

        ParameterSpec target = ParameterSpec.builder(TypeVariableName.get("T"), "target")
                .addAnnotation(CLASS_NONNULL).build();
        ParameterSpec event = ParameterSpec.builder(CLASS_EVENT, "event")
                .addAnnotation(CLASS_NONNULL).build();
        for (BindingMethod binding : methods) {
            buildSubscriberRegisterCode(binding, method, target, event);
        }

        return method.build();
    }

    private MethodSpec.Builder handlerEventMethodBuilder(ParameterSpec target,
                                                         ParameterSpec event) {
        return MethodSpec.methodBuilder("handlerEvent")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addException(ClassName.get(Throwable.class))
                .addParameter(target)
                .addParameter(event);
    }

    private MethodSpec.Builder acceptBuilder(ParameterSpec event) {
        return MethodSpec.methodBuilder("accept")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(event);
    }

    private void buildSubscriberRegisterCode(BindingMethod binding,
                                             MethodSpec.Builder method,
                                             ParameterSpec target,
                                             ParameterSpec event) {
        MethodSpec.Builder callbackMethod = handlerEventMethodBuilder(target, event);
        MethodSpec.Builder acceptMethod = acceptBuilder(event);

        TypeMirror[] parameterTypes = binding.parameterTypes;
        boolean hasTagParam = binding.tagParamName != null;
        if (parameterTypes.length == 0) {
            if (hasTagParam) {
                callbackMethod.addStatement("$N." + binding.name + "($N.getTag())", target, event);
            } else {
                callbackMethod.addStatement("$N." + binding.name + "()", target);
            }
            acceptMethod.addStatement(
                    "return $N.getData() == null || $N.getData().length == 0", event, event);
        } else if (parameterTypes.length == 1
                && parameterTypes[0].toString().equals(CLASS_NAME_EVENT)) {
            if (hasTagParam) {
                callbackMethod.addStatement("$N." + binding.name + "($N.getTag(), $N)", target,
                        event, event);
            } else {
                callbackMethod.addStatement("$N." + binding.name + "($N)", target, event);
            }
            acceptMethod.addStatement("return true");
        } else {
            StringBuilder acceptCode = new StringBuilder("return data != null && ")
                    .append("data.length == ").append(parameterTypes.length);
            callbackMethod.addStatement("Object[] data = $N.getData()", event);
            StringBuilder call = new StringBuilder("$N.").append(binding.name).append("(\n    ");
            List<Object> types = new ArrayList<>();

            if (hasTagParam) {
                call.append("event.getTag()");
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (hasTagParam || i > 0) {
                    call.append(",\n    ");
                }
                call.append("(");
                acceptCode.append("\n&& data[").append(i);

                try {
                    call.append("$T");
                    acceptCode.append("] instanceof $T");
                    types.add(TypeName.get(parameterTypes[i]));
                } catch (Exception e) {
                    call.append(parameterTypes[i]);
                    acceptCode.append("] instanceof ").append(parameterTypes[i]);
                }

                call.append(") data[").append(i).append("]");
            }
            call.append("\n)");

            types.add(0, target);
            callbackMethod.addStatement(call.toString(), types.toArray());

            types.remove(0);
            for (int i = 0; i < types.size(); i++) {
                TypeName tn = (TypeName) types.get(i);
                if (tn.isPrimitive()) {
                    types.remove(i);
                    types.add(i, tn.box());
                }
            }
            acceptMethod.addStatement("Object[] data = $N.getData()", event);
            acceptMethod.addStatement(acceptCode.toString(), types.toArray());
        }

        TypeSpec subscriber =
                TypeSpec.anonymousClassBuilder("target,\n        $S", binding.signature)
                        .superclass(ParameterizedTypeName.get(CLASS_METHOD_SUBSCRIBER,
                                TypeVariableName.get("T")))
                        .addMethod(callbackMethod.build())
                        .addMethod(acceptMethod.build())
                        .build();
        method.addCode("\n");
        method.addStatement("subscriber = $L", subscriber);

        callRegister(binding, method);
    }

    private void callRegister(BindingMethod binding, MethodSpec.Builder method) {
        String[] tags = binding.tags;
        if (tags.length == 0) {
            method.addStatement("eventBus.register(subscriber, $T.DEFAULT_TAG, $T." +
                    binding.mode + ")", CLASS_EVENT_BUS, CLASS_THREAD_MODE);
        } else {
            for (String tag : tags) {
                method.addStatement("eventBus.register(subscriber, $S, $T." + binding.mode + ")",
                        tag, CLASS_THREAD_MODE);
            }
        }
    }

    @Override
    public String toString() {
        return "BindingClass{" +
                "classPackage='" + classPackage + '\'' +
                ", className='" + className + '\'' +
                ", targetClass='" + targetClass + '\'' +
                ", methods=" + methods +
                '}';
    }
}
