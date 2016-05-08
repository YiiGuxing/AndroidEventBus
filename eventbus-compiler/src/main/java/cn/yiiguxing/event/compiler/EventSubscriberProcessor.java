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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import cn.yiiguxing.event.ThreadMode;
import cn.yiiguxing.event.annotation.Subscribe;
import cn.yiiguxing.event.annotation.Tag;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * EventSubscriberProcessor
 * <p/>
 * Created by Yii.Guxing on 16/1/28.
 */
public class EventSubscriberProcessor extends AbstractProcessor {

    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";
    public static final String SUFFIX = "$$SubRegister";

    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Subscribe.class.getCanonicalName());

        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, BindingClass> targetClassMap = findSubscribeAnnotation(roundEnv);
        for (Map.Entry<TypeElement, BindingClass> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            BindingClass bindingClass = entry.getValue();

            Writer writer = null;
            try {
                JavaFileObject jfo =
                        filer.createSourceFile(bindingClass.getRegisterClassName(), typeElement);
                writer = jfo.openWriter();
                bindingClass.brewJava().writeTo(writer);
                writer.flush();
            } catch (IOException e) {
                error(typeElement, "Unable to write subscribe register for type %s: %s",
                        typeElement, e.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return true;
    }

    private Map<TypeElement, BindingClass> findSubscribeAnnotation(RoundEnvironment roundEnv) {
        Map<TypeElement, BindingClass> targetClassMap = new LinkedHashMap<>();
        Set<String> erasedTargetNames = new LinkedHashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Subscribe.class)) {
            try {
                parseSubscribeAnnotation(element, targetClassMap, erasedTargetNames);
            } catch (Exception e) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));

                error(element, "Unable to generate Subscriber for @%s.\n\n%s",
                        Subscribe.class.getSimpleName(), stackTrace.toString());
            }
        }

        for (Map.Entry<TypeElement, BindingClass> entry : targetClassMap.entrySet()) {
            String parentClass = findParent(entry.getKey(), erasedTargetNames);
            if (parentClass != null) {
                entry.getValue().setParentBindingClass(parentClass + SUFFIX);
            }
        }

        return targetClassMap;
    }

    private void parseSubscribeAnnotation(Element element,
                                          Map<TypeElement, BindingClass> targetClassMap,
                                          Set<String> erasedTargetNames)
            throws Exception {
        if (!(element instanceof ExecutableElement) || element.getKind() != ElementKind.METHOD) {
            throw new IllegalStateException("@Subscribe annotation must be on a method.");
        }

        ExecutableElement executableElement = (ExecutableElement) element;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Assemble information on the method.
        Annotation annotation = element.getAnnotation(Subscribe.class);
        Method tagValue = Subscribe.class.getDeclaredMethod("tag");
        if (tagValue.getReturnType() != String[].class) {
            throw new IllegalStateException("@Subscribe annotation tag() type not String[].");
        }

        String[] tags = (String[]) tagValue.invoke(annotation);
        String duplicateId = findDuplicate(tags);
        if (duplicateId != null) {
            error(element,
                    "@Subscribe annotation for method contains duplicate tag: \"%s\". (%s.%s)",
                    duplicateId, enclosingElement.getQualifiedName(), element.getSimpleName());
            return;
        }

        Method modeValue = Subscribe.class.getDeclaredMethod("mode");
        if (modeValue.getReturnType() != ThreadMode.class) {
            throw new IllegalStateException("@Subscribe annotation mode() type not ThreadMode.");
        }

        ThreadMode mode = (ThreadMode) modeValue.invoke(annotation);

        // Verify that the method and its containing class are accessible via generated code.
        if (isInaccessibleViaGeneratedCode(Subscribe.class, "methods", element))
            return;
        if (isBindingInWrongPackage(Subscribe.class, element))
            return;

        List<? extends VariableElement> methodParameters = executableElement.getParameters();
        ArrayList<TypeMirror> parameters = new ArrayList<>();
        String tagParam = null;
        if (!methodParameters.isEmpty()) {
            for (int i = 0; i < methodParameters.size(); i++) {
                VariableElement methodParameter = methodParameters.get(i);
                TypeMirror methodParameterType = methodParameter.asType();
                if (isUnreachable(methodParameterType)) {
                    error(element, "Parameter \"%s\" are unreachable.", methodParameter);
                    return;
                }

                boolean isTag = methodParameter.getAnnotation(Tag.class) != null;
                if (i == 0 && isTag) {
                    if (!String.class.getCanonicalName().equals(methodParameterType.toString())) {
                        error(element, "Tag parameter tye not String.");
                        return;
                    }

                    tagParam = methodParameter.getSimpleName().toString();
                    continue;
                } else if (isTag) {
                    error(element, "@Tag annotation must be on the first parameter.");
                    return;
                }

                parameters.add(methodParameterType);
                if (methodParameterType.toString().contains("<")) {
                    error(element, "Generic type is not support:\"%s\".", methodParameterType);
                    return;
                }

            }
        }

        TypeMirror[] parametersType = new TypeMirror[parameters.size()];
        parameters.toArray(parametersType);

        BindingClass bindingClass = getOrCreateTargetClass(targetClassMap, enclosingElement);
        BindingMethod method = new BindingMethod(executableElement.getSimpleName().toString(),
                tagParam, parametersType, executableElement.toString(), tags, mode);
        bindingClass.addBindingMethod(method);

        erasedTargetNames.add(enclosingElement.toString());
    }

    private BindingClass getOrCreateTargetClass(Map<TypeElement, BindingClass> targetClassMap,
                                                                            TypeElement enclosingElement) {
        BindingClass bindingClass = targetClassMap.get(enclosingElement);
        if (bindingClass == null) {
            String targetClass = enclosingElement.getQualifiedName().toString();
            String classPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, classPackage) + SUFFIX;

            bindingClass = new BindingClass(classPackage, className, targetClass);
            targetClassMap.put(enclosingElement, bindingClass);
        }
        return bindingClass;
    }

    private String findParent(TypeElement typeElement, Set<String> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            String packageName = getPackageName(typeElement);
            if (packageName.startsWith(ANDROID_PREFIX) || packageName.startsWith(JAVA_PREFIX))
                return null;

            if (parents.contains(typeElement.toString()) || hasEventBusAnnotation(typeElement)) {
                return packageName + "." + getClassName(typeElement, packageName);
            }
        }
    }

    private boolean hasEventBusAnnotation(TypeElement typeElement) {
        List<? extends Element> members = elementUtils.getAllMembers(typeElement);
        for (Element element : members) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }
            if (element.getAnnotation(Subscribe.class) != null) {
                return true;
            }
        }

        return false;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private boolean isUnreachable(TypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            Set<Modifier> modifiers = declaredType.asElement().getModifiers();
            if (modifiers.contains(PRIVATE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first duplicate element inside an array, null if there are no duplicates.
     */
    private static String findDuplicate(String[] array) {
        Set<String> seenElements = new LinkedHashSet<>();

        for (String element : array) {
            if (!seenElements.add(element)) {
                return element;
            }
        }

        return null;
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing,
                    enclosingElement.getQualifiedName(),
                    element.getSimpleName());

            return true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing,
                    enclosingElement.getQualifiedName(),
                    element.getSimpleName());

            return true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing,
                    enclosingElement.getQualifiedName(),
                    element.getSimpleName());

            return true;
        }

        return false;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith(ANDROID_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith(JAVA_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

    private void warning(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(WARNING, message, element);
    }
}
