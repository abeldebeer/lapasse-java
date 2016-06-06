package com.cookingfox.lepasse.impl.message.bus;

import com.cookingfox.lepasse.api.message.Message;
import com.cookingfox.lepasse.api.message.bus.MessageBus;
import com.cookingfox.lepasse.api.message.exception.NoMessageHandlersException;
import com.cookingfox.lepasse.api.message.handler.MessageHandler;
import com.cookingfox.lepasse.api.message.store.MessageStore;
import com.cookingfox.lepasse.api.message.store.OnMessageAdded;

import java.util.*;

/**
 * Abstract message bus implementation.
 *
 * @param <M> The concrete message type.
 * @param <H> The concrete message handler type.
 */
public abstract class AbstractMessageBus<M extends Message, H extends MessageHandler<M>>
        implements MessageBus<M, H> {

    //----------------------------------------------------------------------------------------------
    // PROTECTED PROPERTIES
    //----------------------------------------------------------------------------------------------

    /**
     * A map of message types to a set of message handlers.
     */
    protected final Map<Class<M>, Set<H>> messageHandlerMap = new LinkedHashMap<>();

    /**
     * Stores messages.
     */
    protected final MessageStore messageStore;

    //----------------------------------------------------------------------------------------------
    // CONSTRUCTOR
    //----------------------------------------------------------------------------------------------

    public AbstractMessageBus(MessageStore messageStore) {
        messageStore.subscribe(onMessageAddedToStore);

        this.messageStore = messageStore;
    }

    //----------------------------------------------------------------------------------------------
    // ABSTRACT PROTECTED METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * Actually execute the message handler for this message.
     *
     * @param message        The message to handle.
     * @param messageHandler The message handler that is associated with this message.
     */
    protected abstract void executeHandler(M message, H messageHandler);

    /**
     * Returns whether this message bus implementation should handle the concrete message object.
     * Typically this returns the result of an `instanceof` check for the concrete message type `M`.
     *
     * @param message The concrete message object.
     * @return Whether the message should be handled by this message bus.
     * @see M
     */
    protected abstract boolean shouldHandleMessageType(Message message);

    //----------------------------------------------------------------------------------------------
    // PUBLIC METHODS
    //----------------------------------------------------------------------------------------------

    @Override
    public void handleMessage(M message) {
        Objects.requireNonNull(message, "Message can not be null");

        // throws if there are no mapped handlers
        getMessageHandlers(message.getClass());

        messageStore.addMessage(message);
    }

    @Override
    public void mapMessageHandler(Class<M> messageClass, H messageHandler) {
        Objects.requireNonNull(messageClass, "Message class can not be null");
        Objects.requireNonNull(messageHandler, "Message handler can not be null");

        Set<H> handlers = messageHandlerMap.get(messageClass);

        if (handlers == null) {
            handlers = new LinkedHashSet<>();
            messageHandlerMap.put(messageClass, handlers);
        }

        handlers.add(messageHandler);
    }

    //----------------------------------------------------------------------------------------------
    // PROTECTED METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * Get mapped handlers for this message class.
     *
     * @param messageClass The message class to get handlers for.
     * @return The handlers for this message class.
     * @throws NoMessageHandlersException
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    protected Set<H> getMessageHandlers(Class<? extends Message> messageClass) {
        Set<H> handlers = messageHandlerMap.get(messageClass);

        if (handlers == null) {
            for (Class<M> mClass : messageHandlerMap.keySet()) {
                if (mClass.isAssignableFrom(messageClass)) {
                    handlers = messageHandlerMap.get(mClass);
                    break;
                }
            }

            if (handlers == null) {
                throw new NoMessageHandlersException(messageClass);
            }
        }

        return handlers;
    }

    /**
     * Message store subscriber.
     */
    protected final OnMessageAdded onMessageAddedToStore = new OnMessageAdded() {
        @Override
        @SuppressWarnings("unchecked")
        public void onMessageAdded(Message message) {
            if (!shouldHandleMessageType(message)) {
                // this message bus can should not handle messages of this type
                return;
            }

            Set<H> handlers = getMessageHandlers(message.getClass());

            // execute message handlers
            for (H handler : handlers) {
                executeHandler((M) message, handler);
            }
        }
    };

}