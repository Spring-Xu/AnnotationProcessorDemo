package com.compiler;

import com.spring.annotations.BindInt;
import com.spring.annotations.BindString;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import static javax.tools.Diagnostic.Kind.ERROR;

public abstract class BaseProcessor extends AbstractProcessor {
    protected Messager messager;
    protected Elements elements;
    protected Filer filer;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supports = new LinkedHashSet<>();
        supports.add(BindString.class.getCanonicalName());
        supports.add(BindInt.class.getCanonicalName());

        return supports;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    protected void printLog(Diagnostic.Kind kind, String message) {
        messager.printMessage(kind, message);
    }

    protected void printLog(Element element, Diagnostic.Kind kind, String message) {
        messager.printMessage(kind, message, element);
    }

    protected void printLog(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        messager.printMessage(ERROR, message, element);
    }
}
