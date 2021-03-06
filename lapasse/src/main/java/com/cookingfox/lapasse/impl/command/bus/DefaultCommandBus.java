package com.cookingfox.lapasse.impl.command.bus;

import com.cookingfox.lapasse.api.command.Command;
import com.cookingfox.lapasse.api.command.bus.CommandBus;
import com.cookingfox.lapasse.api.command.exception.UnsupportedCommandHandlerException;
import com.cookingfox.lapasse.api.command.handler.*;
import com.cookingfox.lapasse.api.command.logging.CommandLogger;
import com.cookingfox.lapasse.api.command.logging.CommandLoggerHelper;
import com.cookingfox.lapasse.api.event.Event;
import com.cookingfox.lapasse.api.event.bus.EventBus;
import com.cookingfox.lapasse.api.message.Message;
import com.cookingfox.lapasse.api.message.store.MessageStore;
import com.cookingfox.lapasse.api.state.State;
import com.cookingfox.lapasse.api.state.observer.StateObserver;
import com.cookingfox.lapasse.impl.message.bus.AbstractMessageBus;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of {@link CommandBus}.
 *
 * @param <S> The concrete type of the state object.
 */
public class DefaultCommandBus<S extends State>
        extends AbstractMessageBus<Command, CommandHandler<S, Command, Event>>
        implements CommandBus<S> {

    //----------------------------------------------------------------------------------------------
    // CONSTANTS
    //----------------------------------------------------------------------------------------------

    /**
     * Set of supported command handlers.
     */
    protected static final Set<Class<? extends CommandHandler>> SUPPORTED = new LinkedHashSet<>();

    static {
        // add supported command handler implementations
        SUPPORTED.add(VoidCommandHandler.class);
        SUPPORTED.add(SyncCommandHandler.class);
        SUPPORTED.add(SyncMultiCommandHandler.class);
        SUPPORTED.add(AsyncCommandHandler.class);
        SUPPORTED.add(AsyncMultiCommandHandler.class);
    }

    //----------------------------------------------------------------------------------------------
    // PROPERTIES
    //----------------------------------------------------------------------------------------------

    /**
     * Executor service that runs the async command handlers.
     */
    protected ExecutorService commandHandlerExecutor;

    /**
     * The event bus to pass generated events to.
     */
    protected final EventBus<S> eventBus;

    /**
     * Used for logging the command handler operations.
     */
    protected final CommandLoggerHelper loggerHelper;

    /**
     * Provides access to the current state.
     */
    protected final StateObserver<S> stateObserver;

    //----------------------------------------------------------------------------------------------
    // CONSTRUCTOR
    //----------------------------------------------------------------------------------------------

    public DefaultCommandBus(MessageStore messageStore,
                             EventBus<S> eventBus,
                             CommandLoggerHelper loggerHelper,
                             StateObserver<S> stateObserver) {
        super(messageStore);

        this.eventBus = Objects.requireNonNull(eventBus, "Event bus can not be null");
        this.loggerHelper = Objects.requireNonNull(loggerHelper, "Logger helper can not be null");
        this.stateObserver = Objects.requireNonNull(stateObserver, "State observer can not be null");
    }

    //----------------------------------------------------------------------------------------------
    // PUBLIC METHODS
    //----------------------------------------------------------------------------------------------

    @Override
    public void addCommandLogger(CommandLogger logger) {
        loggerHelper.addCommandLogger(logger);
    }

    @Override
    public void dispose() {
        super.dispose();

        // shutdown command handler executor
        if (commandHandlerExecutor != null && !commandHandlerExecutor.isShutdown()) {
            commandHandlerExecutor.shutdown();
        }
    }

    @Override
    public void handleCommand(Command command) {
        handleMessage(command);
    }

    @Override
    public <C extends Command, E extends Event> void mapCommandHandler(
            Class<C> commandClass, CommandHandler<S, C, E> commandHandler) {
        isCommandHandlerSupported(commandHandler);

        // noinspection unchecked
        mapMessageHandler((Class) commandClass, (CommandHandler) commandHandler);
    }

    @Override
    public void removeCommandLogger(CommandLogger logger) {
        loggerHelper.removeCommandLogger(logger);
    }

    /**
     * Sets the executor service to use for executing async command handlers.
     *
     * @param executor The executor service to use.
     */
    @Override
    public void setCommandHandlerExecutor(ExecutorService executor) {
        this.commandHandlerExecutor = Objects.requireNonNull(executor,
                "Command handler executor service can not be null");
    }

    //----------------------------------------------------------------------------------------------
    // OVERRIDDEN ABSTRACT METHODS
    //----------------------------------------------------------------------------------------------

    @Override
    protected void executeHandler(Command command, CommandHandler<S, Command, Event> commandHandler) {
        S currentState = stateObserver.getCurrentState();

        if (commandHandler instanceof MultiCommandHandler) {
            executeMultiCommandHandler(currentState, command, (MultiCommandHandler<S, Command, Event>) commandHandler);
        } else {
            executeCommandHandler(currentState, command, commandHandler);
        }
    }

    @Override
    protected boolean shouldHandleMessageType(Message message) {
        return message instanceof Command;
    }

    //----------------------------------------------------------------------------------------------
    // PROTECTED METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * Execute a command handler that produces 0 or 1 event.
     *
     * @param state   The current state object.
     * @param command The command object.
     * @param handler The handler to execute.
     */
    protected void executeCommandHandler(S state, Command command, CommandHandler<S, Command, Event> handler) {
        Event event = null;

        try {
            if (handler instanceof VoidCommandHandler) {
                // doesn't return anything
                ((VoidCommandHandler<S, Command>) handler).handle(state, command);
            } else if (handler instanceof SyncCommandHandler) {
                // returns an event or null
                event = ((SyncCommandHandler<S, Command, Event>) handler).handle(state, command);
            } else if (handler instanceof AsyncCommandHandler) {
                // returns a callable that is submitted to the executor service (async).
                Callable<Event> callable = ((AsyncCommandHandler<S, Command, Event>) handler).handle(state, command);
                event = getCommandHandlerExecutor().submit(callable).get();
            } else {
                // unsupported implementation
                throw new UnsupportedCommandHandlerException(handler);
            }
        } catch (Exception e) {
            handleResult(e, command, null);
            return;
        }

        handleResult(null, command, event);
    }

    /**
     * Execute a command handler that produces a collection of events.
     *
     * @param state   The current state object.
     * @param command The command object.
     * @param handler The handler to execute.
     */
    protected void executeMultiCommandHandler(S state, Command command, MultiCommandHandler<S, Command, Event> handler) {
        Collection<Event> events;

        try {
            if (handler instanceof SyncMultiCommandHandler) {
                // returns a collection of events
                events = ((SyncMultiCommandHandler<S, Command, Event>) handler).handle(state, command);
            } else if (handler instanceof AsyncMultiCommandHandler) {
                // returns a callable that is submitted to the executor service (async).
                Callable<Collection<Event>> callable = ((AsyncMultiCommandHandler<S, Command, Event>) handler).handle(state, command);
                events = getCommandHandlerExecutor().submit(callable).get();
            } else {
                // unsupported implementation
                throw new UnsupportedCommandHandlerException(handler);
            }
        } catch (Exception e) {
            handleMultiResult(e, command, null);
            return;
        }

        handleMultiResult(null, command, events);
    }

    /**
     * Returns the command handler executor. Creates a new single thread executor if it has not been
     * set explicitly.
     *
     * @return The command handler executor.
     */
    protected ExecutorService getCommandHandlerExecutor() {
        if (commandHandlerExecutor == null) {
            commandHandlerExecutor = Executors.newSingleThreadExecutor();
        }

        return commandHandlerExecutor;
    }

    /**
     * Handle result from a 'single' command handler.
     *
     * @param error   (Optional) An error that occurred.
     * @param command The command that was handled.
     * @param event   (Optional) The event that was produced by the command handler.
     */
    protected void handleResult(Throwable error, Command command, Event event) {
        if (event == null) {
            handleMultiResult(error, command, null);
        } else {
            handleMultiResult(error, command, Collections.singleton(event));
        }
    }

    /**
     * Handle result from a 'multi' command handler.
     *
     * @param error   (Optional) An error that occurred.
     * @param command The command that was handled.
     * @param events  (Optional) The events that were produced by the command handler.
     */
    protected void handleMultiResult(Throwable error, Command command, Collection<Event> events) {
        if (error != null) {
            loggerHelper.onCommandHandlerError(error, command);
            return;
        }

        loggerHelper.onCommandHandlerResult(command, events);

        if (events != null) {
            for (Event event : events) {
                eventBus.handleEvent(event);
            }
        }
    }

    /**
     * Checks whether this command handler implementation is supported.
     *
     * @param commandHandler The command handler to check.
     * @param <C>            Concrete type of the command.
     * @param <E>            Concrete type of the event.
     * @throws UnsupportedCommandHandlerException if the implementation is not supported.
     */
    protected <C extends Command, E extends Event> void isCommandHandlerSupported(
            CommandHandler<S, C, E> commandHandler) {
        Objects.requireNonNull(commandHandler, "Command handler can not be null");

        for (Class<? extends CommandHandler> supported : SUPPORTED) {
            if (supported.isInstance(commandHandler)) {
                return;
            }
        }

        throw new UnsupportedCommandHandlerException(commandHandler);
    }

}
