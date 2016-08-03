package com.spring.annotationbind;

import android.app.Activity;

/**
 * Created by xuchun on 16/7/27.
 */
public class AnnotationManager {
    private static final String BINDING_CLASS_SUFFIX = "$$AnnotationBinder";

    public static void bind(Activity activity) {
        String className = getClassName(activity.getClass().getName());
        try {
            Class<?> cl = Class.forName(className);
            Binder<Object> viewBinder = (Binder<Object>) cl.newInstance();
            viewBinder.bind(activity, activity);
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IllegalAccessException ille) {
            ille.printStackTrace();
        } catch (InstantiationException ie) {
            ie.printStackTrace();
        }
    }

    private static String getClassName(String calssName) {
        return calssName + BINDING_CLASS_SUFFIX;
    }
}
