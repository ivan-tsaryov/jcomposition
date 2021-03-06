/*
 * Copyright 2017 TrollSoftware (a.shitikov73@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.javapoet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

import static com.squareup.javapoet.Util.checkNotNull;
import static jcomposition.processor.utils.Util.modifiersToArray;

public final class MethodSpecUtils {

    /**
     * This is a copy of {@link MethodSpec#overriding(ExecutableElement, DeclaredType, Types)}
     * but without adding an {@link Override} annotation
     * @param method method
     * @param enclosing enclosing type
     * @param env annotation processor environment
     * @return Builder
     */
    public static MethodSpec.Builder getBuilder(ExecutableElement method, DeclaredType enclosing, ProcessingEnvironment env) {
        checkNotNull(method, "method == null");

        Types types = env.getTypeUtils();
        Elements elements = env.getElementUtils();

        ExecutableType executableType = (ExecutableType) types.asMemberOf(enclosing, method);
        List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();
        TypeMirror resolvedReturnType = executableType.getReturnType();

        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)
                || modifiers.contains(Modifier.FINAL)
                || modifiers.contains(Modifier.STATIC)) {
            throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
        }

        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        modifiers = new LinkedHashSet<Modifier>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        modifiers.remove(Util.DEFAULT); // LinkedHashSet permits null as element for Java 7
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        methodBuilder.returns(TypeName.get(resolvedReturnType));

        for (int i = 0; i < resolvedParameterTypes.size(); i++) {
            TypeMirror paramTypeMirror = resolvedParameterTypes.get(i);
            VariableElement parameter = method.getParameters().get(i);

            TypeName name = TypeName.get(paramTypeMirror);
            ParameterSpec spec = ParameterSpec.builder(name, parameter.getSimpleName().toString())
                    // Compiler clashes with addModifiers(Modifiers...) and addModifiers(Iterable<Modifiers>)
                    // Convert it to array
                    .addModifiers(modifiersToArray(parameter.getModifiers()))
                    .build();

            methodBuilder.addParameter(spec);
        }
        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        String javaDoc = elements.getDocComment(method);
        if (javaDoc != null) {
            methodBuilder.addJavadoc(javaDoc, "");
        }

        return methodBuilder;
    }
}
