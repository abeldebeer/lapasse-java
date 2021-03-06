package com.cookingfox.lapasse.impl.facade;

import com.cookingfox.lapasse.api.command.bus.CommandBus;
import com.cookingfox.lapasse.api.command.handler.SyncCommandHandler;
import com.cookingfox.lapasse.api.event.Event;
import com.cookingfox.lapasse.api.event.bus.EventBus;
import com.cookingfox.lapasse.api.event.handler.EventHandler;
import com.cookingfox.lapasse.api.logging.LoggersHelper;
import com.cookingfox.lapasse.api.message.store.MessageStore;
import com.cookingfox.lapasse.api.state.manager.StateManager;
import com.cookingfox.lapasse.api.state.observer.OnStateChanged;
import com.cookingfox.lapasse.impl.command.bus.DefaultCommandBus;
import com.cookingfox.lapasse.impl.event.bus.DefaultEventBus;
import com.cookingfox.lapasse.impl.logging.DefaultLogger;
import com.cookingfox.lapasse.impl.logging.DefaultLoggersHelper;
import com.cookingfox.lapasse.impl.message.store.NoStorageMessageStore;
import com.cookingfox.lapasse.impl.state.manager.DefaultStateManager;
import fixtures.example.command.IncrementCount;
import fixtures.example.event.CountIncremented;
import fixtures.example.state.CountState;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LaPasseFacade}.
 */
public class LaPasseFacadeTest {

    //----------------------------------------------------------------------------------------------
    // TESTS: public methods
    //----------------------------------------------------------------------------------------------

    @Test
    public void methods_should_not_throw() throws Exception {
        DefaultLogger<CountState> logger = new DefaultLogger<>();

        OnStateChanged<CountState> onStateChanged = new OnStateChanged<CountState>() {
            @Override
            public void onStateChanged(CountState state, Event event) {
                // ignore
            }
        };

        LaPasseFacade<CountState> facade = new LaPasseFacade.Builder<>(new CountState(0)).build();

        /* COMBINED LOGGER */

        facade.addLogger(logger);
        facade.removeLogger(logger);

        /* COMMAND */

        facade.addCommandLogger(logger);
        facade.removeCommandLogger(logger);
        facade.mapCommandHandler(IncrementCount.class, new SyncCommandHandler<CountState, IncrementCount, CountIncremented>() {
            @Override
            public CountIncremented handle(CountState state, IncrementCount command) {
                return null;
            }
        });
        facade.handleCommand(new IncrementCount(1));
        facade.setCommandHandlerExecutor(Executors.newSingleThreadExecutor());

        /* EVENT */

        facade.addEventLogger(logger);
        facade.removeEventLogger(logger);
        facade.mapEventHandler(CountIncremented.class, new EventHandler<CountState, CountIncremented>() {
            @Override
            public CountState handle(CountState previousState, CountIncremented event) {
                return new CountState(previousState.getCount() + event.getCount());
            }
        });
        facade.handleEvent(new CountIncremented(1));

        /* STATE */

        facade.getCurrentState();
        facade.addStateChangedListener(onStateChanged);
        facade.removeStateChangedListener(onStateChanged);

        /* DISPOSE */

        facade.dispose();
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: Builder
    //----------------------------------------------------------------------------------------------

    @Test
    public void builder_should_set_correct_defaults() throws Exception {
        LaPasseFacade<CountState> facade = new LaPasseFacade.Builder<>(new CountState(0)).build();

        assertTrue(facade.commandBus instanceof DefaultCommandBus);
        assertTrue(facade.eventBus instanceof DefaultEventBus);
        assertTrue(facade.loggersHelper instanceof DefaultLoggersHelper);
        assertTrue(facade.messageStore instanceof NoStorageMessageStore);
        assertTrue(facade.stateManager instanceof DefaultStateManager);
    }

    @Test
    public void builder_should_apply_custom_settings() throws Exception {
        CountState initialState = new CountState(0);
        LoggersHelper<CountState> loggers = new DefaultLoggersHelper<>();
        MessageStore messageStore = new NoStorageMessageStore();
        StateManager<CountState> stateManager = new DefaultStateManager<>(initialState);
        EventBus<CountState> eventBus = new DefaultEventBus<>(messageStore, loggers, stateManager);
        CommandBus<CountState> commandBus = new DefaultCommandBus<>(messageStore, eventBus, loggers, stateManager);

        LaPasseFacade<CountState> facade = new LaPasseFacade.Builder<>(initialState)
                .setCommandBus(commandBus)
                .setEventBus(eventBus)
                .setLoggersHelper(loggers)
                .setMessageStore(messageStore)
                .setStateManager(stateManager)
                .build();

        assertSame(commandBus, facade.commandBus);
        assertSame(eventBus, facade.eventBus);
        assertSame(loggers, facade.loggersHelper);
        assertSame(stateManager, facade.stateManager);
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: dispose
    //----------------------------------------------------------------------------------------------

    @Test
    public void dispose_should_dispose_components() throws Exception {
        final AtomicBoolean messageStoreCalled = new AtomicBoolean(false);
        final AtomicBoolean loggersCalled = new AtomicBoolean(false);
        final AtomicBoolean stateManagerCalled = new AtomicBoolean(false);
        final AtomicBoolean eventBusCalled = new AtomicBoolean(false);
        final AtomicBoolean commandBusCalled = new AtomicBoolean(false);

        CountState initialState = new CountState(0);

        MessageStore messageStore = new NoStorageMessageStore() {
            @Override
            public void dispose() {
                messageStoreCalled.set(true);
            }
        };

        LoggersHelper<CountState> loggers = new DefaultLoggersHelper<CountState>() {
            @Override
            public void dispose() {
                loggersCalled.set(true);
            }
        };

        StateManager<CountState> stateManager = new DefaultStateManager<CountState>(initialState) {
            @Override
            public void dispose() {
                stateManagerCalled.set(true);
            }
        };

        EventBus<CountState> eventBus = new DefaultEventBus<CountState>(messageStore, loggers, stateManager) {
            @Override
            public void dispose() {
                eventBusCalled.set(true);
            }
        };

        CommandBus<CountState> commandBus = new DefaultCommandBus<CountState>(messageStore, eventBus, loggers, stateManager) {
            @Override
            public void dispose() {
                commandBusCalled.set(true);
            }
        };

        LaPasseFacade<CountState> facade = new LaPasseFacade.Builder<>(initialState)
                .setCommandBus(commandBus)
                .setEventBus(eventBus)
                .setLoggersHelper(loggers)
                .setMessageStore(messageStore)
                .setStateManager(stateManager)
                .build();

        facade.dispose();

        assertTrue(commandBusCalled.get());
        assertTrue(eventBusCalled.get());
        assertTrue(loggersCalled.get());
        assertTrue(messageStoreCalled.get());
        assertTrue(stateManagerCalled.get());
    }

}
