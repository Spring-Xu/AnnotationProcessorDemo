package com.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.spring.annotations.BindInt;
import com.spring.annotations.BindString;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(Processor.class)
public class BindingProcessor extends BaseProcessor {
    private static final String BIND_CLASS_SUFFIX = "$$AnnotationBinder";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        printLog(Diagnostic.Kind.OTHER, "BindingProcessor process.... ");

        Map<TypeElement, BindingClass> bindingClassMap = findAndParseTargets(roundEnv);
        for (Map.Entry<TypeElement, BindingClass> entry : bindingClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            BindingClass bindingClass = entry.getValue();

            try {
                printLog(Diagnostic.Kind.OTHER, "BindingProcessor code:" + bindingClass.brewJava().toString());
                bindingClass.brewJava().writeTo(filer);
            } catch (IOException e) {
                printLog(typeElement, "Unable to write view binder for type %s: %s", typeElement,
                        e.getMessage());
            }
        }
        return true;
    }

    private Map<TypeElement, BindingClass> findAndParseTargets(RoundEnvironment roundEnv) {
        printLog(Diagnostic.Kind.OTHER, "findAndParseTargets.... ");

        Map<TypeElement, BindingClass> bindingClassMap = new HashMap<>();
        Set<String> erasedTargetNames = new LinkedHashSet<>();
        Set<? extends Element> bindStringSet = roundEnv.getElementsAnnotatedWith(BindString.class);
        for (Element element : bindStringSet) {
            printLog(Diagnostic.Kind.NOTE, "findAndParseTargets BindString ");

            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                printLog(Diagnostic.Kind.OTHER, "findAndParseTargets parseResourceString:");
                parseResourceString(element, bindingClassMap, erasedTargetNames);
            } catch (Exception e) {
                printLog(Diagnostic.Kind.OTHER, e.getMessage());
            }
        }

        Set<? extends Element> bindIntSet = roundEnv.getElementsAnnotatedWith(BindInt.class);
        for (Element element : bindIntSet) {
            printLog(Diagnostic.Kind.NOTE, "findAndParseTargets BindInt ");

            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                printLog(Diagnostic.Kind.OTHER, "findAndParseTargets BindInt:");
                parseInt(element, bindingClassMap, erasedTargetNames);
            } catch (Exception e) {
                printLog(Diagnostic.Kind.OTHER, e.getMessage());
            }
        }

        // Try to find a parent binder for each.
        for (Map.Entry<TypeElement, BindingClass> entry : bindingClassMap.entrySet()) {
            String parentClassFqcn = findParentFqcn(entry.getKey(), erasedTargetNames);
            if (parentClassFqcn != null) {
                entry.getValue().setParentViewBinder(parentClassFqcn + BIND_CLASS_SUFFIX);
            }
        }


        return bindingClassMap;
    }

    /**
     * Finds the parent binder type in the supplied set, if any.
     */
    private String findParentFqcn(TypeElement typeElement, Set<String> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (parents.contains(typeElement.toString())) {
                String packageName = getPackageName(typeElement);
                return packageName + "." + getClassName(typeElement, packageName);
            }
        }
    }

    private void parseResourceString(Element element, Map<TypeElement, BindingClass> targetClassMap,
                                     Set<String> erasedTargetNames) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify that the target type is String.
        printLog(Diagnostic.Kind.OTHER, "findAndParseTargets parseResourceString:" + element.asType().toString());
        if (!String.class.getCanonicalName().equals(element.asType().toString())) {
            printLog(element, "@%s field type must be 'String'. (%s.%s)",
                    BindString.class.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify common generated code restrictions.
        hasError |= isInaccessibleViaGeneratedCode(BindString.class, "fields", element);
        hasError |= isBindingInWrongPackage(BindString.class, element);

        if (hasError) {
            return;
        }

        // Assemble information on the field.
        String value = element.getAnnotation(BindString.class).value();
        String name = element.getSimpleName().toString();

        BindingClass bindingClass = getOrCreateTargetClass(targetClassMap, enclosingElement);
        StringBinding binding = new StringBinding(name, value);
        bindingClass.addString(binding);
        printLog(Diagnostic.Kind.OTHER, "findAndParseTargets parseResourceString:" + enclosingElement.toString());
        erasedTargetNames.add(enclosingElement.toString());
    }

    private void parseInt(Element element, Map<TypeElement, BindingClass> targetClassMap,
                          Set<String> erasedTargetNames) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify that the target type is String.
        printLog(Diagnostic.Kind.OTHER, "findAndParseTargets BindInt:" + element.asType().toString());
        printLog(Diagnostic.Kind.OTHER, "findAndParseTargets BindInt enclosingElement:" + enclosingElement.asType().toString());
        if (!int.class.getCanonicalName().equals(element.asType().toString())) {
            printLog(element, "@%s field type must be 'Int'. (%s.%s)",
                    BindInt.class.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify common generated code restrictions.
        hasError |= isInaccessibleViaGeneratedCode(BindInt.class, "fields", element);
        hasError |= isBindingInWrongPackage(BindInt.class, element);

        if (hasError) {
            return;
        }

        // Assemble information on the field.
        int value = element.getAnnotation(BindInt.class).value();
        String name = element.getSimpleName().toString();

        BindingClass bindingClass = getOrCreateTargetClass(targetClassMap, enclosingElement);
        IntBinding binding = new IntBinding(name, value);
        bindingClass.addInt(binding);
        printLog(Diagnostic.Kind.OTHER, "findAndParseTargets BindInt end:" + enclosingElement.toString());
        erasedTargetNames.add(enclosingElement.toString());
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            printLog(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            printLog(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        if (qualifiedName.startsWith("javax.")) {
            printLog(element, "@%s-annotated class incorrectly in Javax framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            printLog(element, "@%s %s must not be priva te or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            printLog(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            printLog(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private BindingClass getOrCreateTargetClass(Map<TypeElement, BindingClass> targetClassMap,
                                                TypeElement enclosingElement) {
        BindingClass bindingClass = targetClassMap.get(enclosingElement);
        if (bindingClass == null) {
            String targetType = enclosingElement.getQualifiedName().toString();
            String classPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, classPackage) + BIND_CLASS_SUFFIX;

            bindingClass = new BindingClass(classPackage, className, targetType);
            targetClassMap.put(enclosingElement, bindingClass);
        }
        return bindingClass;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private String getPackageName(TypeElement type) {
        return elements.getPackageOf(type).getQualifiedName().toString();
    }

}
