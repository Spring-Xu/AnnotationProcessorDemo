package com.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

public class BindingClass {

    private static final ClassName BINDER = ClassName.get("com.spring.annotationbind", "Binder");
    private final List<StringBinding> stringBindings = new ArrayList<>();
    private final List<IntBinding> intBindings = new ArrayList<>();
    private final String classPackage;
    private final String className;
    private final String targetClass;
    private String parentViewBinder;

    BindingClass(String classPackage, String className, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClass = targetClass;
    }

    void setParentViewBinder(String parentViewBinder) {
        this.parentViewBinder = parentViewBinder;
    }

    void addString(StringBinding bindString) {
        stringBindings.add(bindString);
    }
    void addInt(IntBinding bindInt) {
        intBindings.add(bindInt);
    }


    JavaFile brewJava() {
        TypeSpec.Builder result = TypeSpec.classBuilder(className)
                .addModifiers(PUBLIC)
                .addTypeVariable(TypeVariableName.get("T", ClassName.bestGuess(targetClass)));

        if (parentViewBinder != null) {
            result.superclass(ParameterizedTypeName.get(ClassName.bestGuess(parentViewBinder),
                    TypeVariableName.get("T")));
        } else {
            result.addSuperinterface(ParameterizedTypeName.get(BINDER, TypeVariableName.get("T")));
        }

        result.addMethod(createBindMethod());

        return JavaFile.builder(classPackage, result.build())
                .addFileComment("This is a class, create by annotation processor!")
                .build();
    }

    private MethodSpec createBindMethod() {
        MethodSpec.Builder result = MethodSpec.methodBuilder("bind")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(TypeVariableName.get("T"), "target", FINAL)
                .addParameter(Object.class, "source");

        // Emit a call to the superclass binder, if any.
        if (parentViewBinder != null) {
            result.addStatement("super.bind(target, source)");
        }

        for (StringBinding binding : stringBindings) {
            result.addStatement("target.$L = \"$L\"", binding.getName(), binding.getValue());
        }

        for (IntBinding binding : intBindings) {
            result.addStatement("target.$L = $L", binding.getName(), binding.getValue());
        }

        return result.build();
    }
}
