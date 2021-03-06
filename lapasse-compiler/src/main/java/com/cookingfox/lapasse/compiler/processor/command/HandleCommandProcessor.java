package com.cookingfox.lapasse.compiler.processor.command;

import com.cookingfox.lapasse.annotation.HandleCommand;
import com.cookingfox.lapasse.api.command.Command;
import com.cookingfox.lapasse.api.event.Event;
import com.cookingfox.lapasse.api.state.State;
import com.cookingfox.lapasse.compiler.processor.ProcessorHelper;
import rx.Observable;
import rx.Single;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.cookingfox.lapasse.compiler.processor.command.HandleCommandAnnotationParams.*;
import static com.cookingfox.lapasse.compiler.processor.command.HandleCommandMethodParams.*;
import static com.cookingfox.lapasse.compiler.processor.command.HandleCommandReturnValue.*;
import static com.cookingfox.lapasse.compiler.utils.TypeUtils.*;

/**
 * Processes a {@link HandleCommand} annotated handler method.
 */
public class HandleCommandProcessor {

    //----------------------------------------------------------------------------------------------
    // CONSTANTS
    //----------------------------------------------------------------------------------------------

    /**
     * String representation of the allowed return types for a command handler method.
     */
    public static final String ALLOWED_RETURN_TYPES = String.format("`void`, " +
                    "`%1$s`, " +
                    "`%2$s<%1$s>`, " +
                    "`%3$s<%1$s>`, " +
                    "`%3$s<%2$s<%1$s>>`, " +
                    "`%4$s<%1$s>`, " +
                    "`%4$s<%2$s<%1$s>>`",
            Event.class.getSimpleName(),
            Collection.class.getSimpleName(),
            Callable.class.getSimpleName(),
            Observable.class.getSimpleName());

    //----------------------------------------------------------------------------------------------
    // PROPERTIES
    //----------------------------------------------------------------------------------------------

    /**
     * The annotated element.
     */
    protected final Element element;

    /**
     * Value Object containing the processing results.
     */
    protected final HandleCommandResult result = new HandleCommandResult();

    /**
     * Utility methods for operating on types.
     */
    protected final Types types;

    //----------------------------------------------------------------------------------------------
    // CONSTRUCTOR
    //----------------------------------------------------------------------------------------------

    public HandleCommandProcessor(Element element, Types types) {
        this.element = element;
        this.types = types;
    }

    //----------------------------------------------------------------------------------------------
    // PUBLIC METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * Process the {@link HandleCommand} annotated handler method and create a result object with
     * the extracted values.
     *
     * @return The result object of process operation.
     * @throws Exception when the handler method is invalid.
     */
    public HandleCommandResult process() throws Exception {
        ExecutableElement method = ProcessorHelper.validateAndGetAnnotatedMethod(element);
        HandleCommand annotation = method.getAnnotation(HandleCommand.class);
        List<? extends VariableElement> parameters = method.getParameters();
        TypeMirror returnType = method.getReturnType();

        // populate result
        result.methodName = element.getSimpleName();
        result.parameters = parameters;
        result.returnType = returnType;
        result.annotationParams = determineAnnotationParams(annotation);
        result.methodParams = determineMethodParams(parameters);
        result.returnValue = determineReturnValue(returnType);
        result.stateType = determineStateType();
        result.commandType = determineCommandType();

        detectAnnotationMethodParamsConflict();

        return result;
    }

    //----------------------------------------------------------------------------------------------
    // PROTECTED METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * Creates an exception for when the handler method's parameters are invalid.
     *
     * @param parameters The handler method's parameters.
     * @return The exception with the formatted error message.
     */
    protected Exception createInvalidMethodParamsException(List<? extends VariableElement> parameters) {
        List<TypeMirror> types = new LinkedList<>();

        for (VariableElement parameter : parameters) {
            types.add(parameter.asType());
        }

        return new Exception(String.format("Method parameters are invalid (expected State and " +
                "Command implementations): %s", types));
    }

    /**
     * Creates an exception for when the handler method's return type is invalid.
     *
     * @param returnType The handler method's return type.
     * @return The exception with the formatted error message.
     */
    protected Exception createInvalidReturnTypeException(TypeMirror returnType) {
        return new Exception(String.format("Command handler has an invalid return type: `%s`. " +
                "Allowed return types: %s", returnType, ALLOWED_RETURN_TYPES));
    }

    /**
     * Checks whether there is a conflict between the annotation and method parameters.
     *
     * @throws Exception when there is a conflict between the annotation and method parameters.
     */
    protected void detectAnnotationMethodParamsConflict() throws Exception {
        TypeMirror annotationCommandType = result.getAnnotationCommandType();
        TypeMirror annotationStateType = result.getAnnotationStateType();
        TypeMirror commandType = result.getCommandType();
        TypeMirror stateType = result.getStateType();

        // command type mismatch
        if (annotationCommandType != null && !types.isSameType(commandType, annotationCommandType)) {
            throw new Exception(String.format("Annotation parameter for command (`%s`) has " +
                    "different type than method parameter (`%s`)", annotationCommandType, commandType));
        }

        // state type mismatch
        if (annotationStateType != null && !types.isSameType(stateType, annotationStateType)) {
            throw new Exception(String.format("Annotation parameter for state (`%s`) has " +
                    "different type than method parameter (`%s`)", annotationStateType, stateType));
        }
    }

    /**
     * Determines the type of parameters for the {@link HandleCommand} annotation. The annotation
     * can hold references to concrete command and state classes that this method should handle.
     *
     * @param annotation The annotation object.
     * @return An enum which indicates the annotation parameters.
     * @throws Exception when the annotation parameters could not be determined.
     */
    protected HandleCommandAnnotationParams determineAnnotationParams(HandleCommand annotation) throws Exception {
        TypeMirror annotationCommandType = null;
        TypeMirror annotationStateType = null;

        try {
            annotation.command(); // this should throw
        } catch (MirroredTypeException e) {
            annotationCommandType = e.getTypeMirror();
        }

        try {
            annotation.state(); // this should throw
        } catch (MirroredTypeException e) {
            annotationStateType = e.getTypeMirror();
        }

        // this should never happen
        if (annotationCommandType == null || annotationStateType == null) {
            throw new Exception("Could not extract command or state type from annotation");
        }

        // should not be base type
        boolean hasCommand = !equalsType(annotationCommandType, Command.class);
        boolean hasState = !equalsType(annotationStateType, State.class);

        if (hasCommand) {
            result.annotationCommandType = annotationCommandType;
        }
        if (hasState) {
            result.annotationStateType = annotationStateType;
        }

        if (hasCommand && hasState) {
            return ANNOTATION_TWO_PARAMS_COMMAND_STATE;
        } else if (hasCommand) {
            return ANNOTATION_ONE_PARAM_COMMAND;
        } else if (hasState) {
            return ANNOTATION_ONE_PARAM_STATE;
        }

        return ANNOTATION_NO_PARAMS;
    }

    /**
     * Determines the concrete command type of the handler method. The command type can be set by
     * both the method parameters (command object) or the annotation (command class).
     *
     * @return The concrete command type of the handler method.
     * @throws Exception when the concrete command type could not be determined.
     */
    protected TypeMirror determineCommandType() throws Exception {
        TypeMirror commandType = null;

        switch (result.getMethodParams()) {
            case METHOD_ONE_PARAM_COMMAND:
            case METHOD_TWO_PARAMS_COMMAND_STATE:
                // first param
                commandType = result.getParameters().get(0).asType();
                break;

            case METHOD_TWO_PARAMS_STATE_COMMAND:
                // second param
                commandType = result.getParameters().get(1).asType();
                break;
        }

        if (commandType == null) {
            switch (result.getAnnotationParams()) {
                case ANNOTATION_ONE_PARAM_COMMAND:
                case ANNOTATION_TWO_PARAMS_COMMAND_STATE:
                    commandType = result.getAnnotationCommandType();
                    break;
            }
        }

        if (commandType == null) {
            throw new Exception(String.format("Could not determine command type based on the " +
                    "method's parameters or annotation. Make sure at least the target command class " +
                    "is available as a method parameter or as an annotation value: " +
                    "`@%s(command = MyCommand.class)`", HandleCommand.class.getSimpleName()));
        }

        return extendsCommand(commandType);
    }

    /**
     * Validates and identifies the handler method parameters.
     *
     * @param parameters The method parameters.
     * @return An indication of the method parameters.
     * @throws Exception when the method parameters are invalid.
     */
    protected HandleCommandMethodParams determineMethodParams(List<? extends VariableElement> parameters) throws Exception {
        int numParams = parameters.size();

        if (numParams == 0) {
            return METHOD_NO_PARAMS;
        } else if (numParams > 2) {
            throw createInvalidMethodParamsException(parameters);
        }

        VariableElement firstParam = parameters.get(0);
        boolean firstIsCommand = isSubtype(firstParam, Command.class);
        boolean firstIsState = isSubtype(firstParam, State.class);

        if (!firstIsCommand && !firstIsState) {
            throw createInvalidMethodParamsException(parameters);
        }

        if (numParams == 1) {
            return firstIsCommand ? METHOD_ONE_PARAM_COMMAND : METHOD_ONE_PARAM_STATE;
        }

        VariableElement secondParam = parameters.get(1);

        if (firstIsCommand && isSubtype(secondParam, State.class)) {
            return METHOD_TWO_PARAMS_COMMAND_STATE;
        } else if (isSubtype(firstParam, State.class) && isSubtype(secondParam, Command.class)) {
            return METHOD_TWO_PARAMS_STATE_COMMAND;
        }

        throw createInvalidMethodParamsException(parameters);
    }

    /**
     * Determines the return value of the handler method. This also sets the event type on the
     * processor result.
     *
     * @param returnType The handler method return type.
     * @return An indication of the handler method's return value.
     * @throws Exception when the return value is invalid.
     */
    protected HandleCommandReturnValue determineReturnValue(TypeMirror returnType) throws Exception {
        TypeKind returnTypeKind = returnType.getKind();

        if (returnTypeKind == TypeKind.VOID) {
            return RETURNS_VOID;
        } else if (returnTypeKind != TypeKind.DECLARED) {
            throw createInvalidReturnTypeException(returnType);
        } else if (isSubtype(returnType, Event.class)) {
            result.eventType = returnType;

            return RETURNS_EVENT;
        }

        boolean returnsCallable = isSubtype(returnType, Callable.class);
        boolean returnsCollection = isSubtype(returnType, Collection.class);
        boolean returnsObservable = isSubtype(returnType, Observable.class);
        boolean returnsSingle = isSubtype(returnType, Single.class);

        if (!returnsCallable && !returnsCollection && !returnsObservable && !returnsSingle) {
            throw createInvalidReturnTypeException(returnType);
        }

        List<? extends TypeMirror> typeArguments = ((DeclaredType) returnType).getTypeArguments();
        DeclaredType firstArg = (DeclaredType) typeArguments.get(0);

        if (isSubtype(firstArg, Event.class)) {
            result.eventType = firstArg;

            if (returnsCallable) {
                return RETURNS_EVENT_CALLABLE;
            } else if (returnsCollection) {
                return RETURNS_EVENT_COLLECTION;
            } else if (returnsObservable) {
                return RETURNS_EVENT_OBSERVABLE;
            }

            return RETURNS_EVENT_SINGLE;
        } else if (returnsCollection || !firstArgIsSubType(returnType, Collection.class)) {
            // throw: below expects callable or observable of collection
            throw createInvalidReturnTypeException(returnType);
        }

        DeclaredType firstArgFirstArg = (DeclaredType) firstArg.getTypeArguments().get(0);

        // check whether the generic type of the callable / observable is `Collection<Event>`
        if (isSubtype(firstArgFirstArg, Event.class)) {
            result.eventType = firstArgFirstArg;

            if (returnsCallable) {
                return RETURNS_EVENT_COLLECTION_CALLABLE;
            } else if (returnsObservable) {
                return RETURNS_EVENT_COLLECTION_OBSERVABLE;
            } else {
                return RETURNS_EVENT_COLLECTION_SINGLE;
            }
        }

        throw createInvalidReturnTypeException(returnType);
    }

    /**
     * Determines the state type based on the handler method and annotation parameters.
     *
     * @return The concrete state type if available, or null.
     * @throws Exception when the state type can not be determined.
     */
    protected TypeMirror determineStateType() throws Exception {
        TypeMirror stateType = null;

        switch (result.getMethodParams()) {
            case METHOD_ONE_PARAM_STATE:
            case METHOD_TWO_PARAMS_STATE_COMMAND:
                // first param
                stateType = result.getParameters().get(0).asType();
                break;

            case METHOD_TWO_PARAMS_COMMAND_STATE:
                // second param
                stateType = result.getParameters().get(1).asType();
                break;
        }

        if (stateType == null) {
            switch (result.getAnnotationParams()) {
                case ANNOTATION_ONE_PARAM_STATE:
                case ANNOTATION_TWO_PARAMS_COMMAND_STATE:
                    return result.getAnnotationStateType();
            }
        }

        if (stateType != null) {
            return extendsState(stateType);
        }

        return null;
    }

    /**
     * Asserts the provided type extends command.
     *
     * @param type The type to validate.
     * @return The provided type, if valid.
     * @throws Exception when the provided is equal to the command base type.
     */
    protected TypeMirror extendsCommand(TypeMirror type) throws Exception {
        if (equalsType(type, Command.class)) {
            throw new Exception(String.format("Command parameter cannot be the base type `%s`",
                    Command.class.getName()));
        }

        return type;
    }

    /**
     * Asserts the provided type extends state.
     *
     * @param type The type to validate.
     * @return The provided type, if valid.
     * @throws Exception when the provided is equal to the state base type.
     */
    protected TypeMirror extendsState(TypeMirror type) throws Exception {
        if (equalsType(type, State.class)) {
            throw new Exception(String.format("State parameter cannot be the base type `%s`",
                    State.class.getName()));
        }

        return type;
    }

}
