package com.spring.annotationbind;

public interface Binder<T> {
  void bind(T target, Object source);
}
