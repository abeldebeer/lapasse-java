package com.cookingfox.lapasse.compiler.processor.command;

import com.cookingfox.lapasse.annotation.HandleCommand;
import com.cookingfox.lapasse.api.event.Event;
import rx.Observable;
import rx.Single;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Indicates the return type for a {@link HandleCommand} annotated handler method.
 */
public enum HandleCommandReturnValue {

    /**
     * The method returns an {@link Event}.
     */
    RETURNS_EVENT,

    /**
     * The method returns a {@link Callable} of an {@link Event}.
     */
    RETURNS_EVENT_CALLABLE,

    /**
     * The method returns a {@link Collection} of {@link Event}s.
     */
    RETURNS_EVENT_COLLECTION,

    /**
     * The method returns a {@link Callable} of a {@link Collection} of {@link Event}s.
     */
    RETURNS_EVENT_COLLECTION_CALLABLE,

    /**
     * The method returns an Rx {@link Observable} of a {@link Collection} of {@link Event}s.
     */
    RETURNS_EVENT_COLLECTION_OBSERVABLE,

    /**
     * The method returns an Rx {@link Single} of a {@link Collection} of {@link Event}s.
     */
    RETURNS_EVENT_COLLECTION_SINGLE,

    /**
     * The method returns an Rx {@link Observable} of an {@link Event}.
     */
    RETURNS_EVENT_OBSERVABLE,

    /**
     * The method returns an Rx {@link Single} of an {@link Event}.
     */
    RETURNS_EVENT_SINGLE,

    /**
     * The method returns void.
     */
    RETURNS_VOID

}
