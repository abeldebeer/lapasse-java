package com.cookingfox.lapasse.compiler.processor.command;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Created by abeldebeer on 15/08/16.
 */
public class HandleCommandResult {

    protected TypeMirror annotationCommandType;
    protected TypeMirror annotationStateType;
    protected HandleCommandAnnotationType annotationType;
    protected TypeMirror commandType;
    protected TypeMirror eventType;
    protected Name methodName;
    protected HandleCommandMethodType methodType;
    protected List<? extends VariableElement> parameters;
    protected HandleCommandReturnType returnType;
    protected TypeMirror stateType;
    protected TypeMirror returnTypeName;

    public TypeMirror getAnnotationCommandType() {
        return annotationCommandType;
    }

    public TypeMirror getAnnotationStateType() {
        return annotationStateType;
    }

    public HandleCommandAnnotationType getAnnotationType() {
        return annotationType;
    }

    public TypeMirror getCommandType() {
        return commandType;
    }

    public TypeMirror getEventType() {
        return eventType;
    }

    public TypeName getEventTypeName() {
        if (getReturnType() == HandleCommandReturnType.RETURNS_VOID) {
            return TypeName.VOID;
        }

        return ClassName.get(getEventType());
    }

    public Name getMethodName() {
        return methodName;
    }

    public HandleCommandMethodType getMethodType() {
        return methodType;
    }

    public List<? extends VariableElement> getParameters() {
        return parameters;
    }

    public HandleCommandReturnType getReturnType() {
        return returnType;
    }

    public TypeMirror getReturnTypeName() {
        return returnTypeName;
    }

    public TypeMirror getStateType() {
        return stateType;
    }

}
