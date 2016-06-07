package com.cookingfox.lepasse.impl.logging;

import com.cookingfox.lepasse.api.command.Command;
import com.cookingfox.lepasse.api.command.logging.CommandLogger;
import com.cookingfox.lepasse.api.event.Event;
import com.cookingfox.lepasse.api.event.logging.EventLogger;
import fixtures.state.FixtureState;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LePasseLoggers}.
 */
public class LePasseLoggersTest {

    //----------------------------------------------------------------------------------------------
    // TEST SETUP
    //----------------------------------------------------------------------------------------------

    private LePasseLoggers<FixtureState> loggers;

    @Before
    public void setUp() throws Exception {
        loggers = new LePasseLoggers<>();
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: addCommandLogger
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void addCommandLogger_should_throw_if_logger_null() throws Exception {
        loggers.addCommandLogger(null);
    }

    @Test
    public void addCommandLogger_should_add_logger() throws Exception {
        final AtomicBoolean commandErrorCalled = new AtomicBoolean(false);
        final AtomicBoolean commandResultCalled = new AtomicBoolean(false);

        loggers.addCommandLogger(new CommandLogger<FixtureState>() {
            @Override
            public void onCommandHandlerError(Throwable error, Command command, Event... events) {
                commandErrorCalled.set(true);
            }

            @Override
            public void onCommandHandlerResult(Command command, Event... events) {
                commandResultCalled.set(true);
            }
        });

        loggers.onCommandHandlerError(null, null);
        loggers.onCommandHandlerResult(null);

        assertTrue(commandErrorCalled.get());
        assertTrue(commandResultCalled.get());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: addEventLogger
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void addEventLogger_should_throw_if_logger_null() throws Exception {
        loggers.addEventLogger(null);
    }

    @Test
    public void addEventLogger_should_add_logger() throws Exception {
        final AtomicBoolean eventErrorCalled = new AtomicBoolean(false);
        final AtomicBoolean eventResultCalled = new AtomicBoolean(false);

        loggers.addEventLogger(new EventLogger<FixtureState>() {
            @Override
            public void onEventHandlerError(Throwable error, Event event, FixtureState newState) {
                eventErrorCalled.set(true);
            }

            @Override
            public void onEventHandlerResult(Event event, FixtureState newState) {
                eventResultCalled.set(true);
            }
        });

        loggers.onEventHandlerError(null, null, null);
        loggers.onEventHandlerResult(null, null);

        assertTrue(eventErrorCalled.get());
        assertTrue(eventResultCalled.get());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: addLogger
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void addLogger_should_throw_if_logger_null() throws Exception {
        loggers.addLogger(null);
    }

    @Test
    public void addLogger_should_add_multiple_loggers() throws Exception {
        final AtomicBoolean commandErrorCalled = new AtomicBoolean(false);
        final AtomicBoolean commandResultCalled = new AtomicBoolean(false);
        final AtomicBoolean eventErrorCalled = new AtomicBoolean(false);
        final AtomicBoolean eventResultCalled = new AtomicBoolean(false);

        loggers.addLogger(new DefaultLogger<FixtureState>() {
            @Override
            public void onCommandHandlerError(Throwable error, Command command, Event... events) {
                commandErrorCalled.set(true);
            }

            @Override
            public void onCommandHandlerResult(Command command, Event... events) {
                commandResultCalled.set(true);
            }

            @Override
            public void onEventHandlerError(Throwable error, Event event, FixtureState newState) {
                eventErrorCalled.set(true);
            }

            @Override
            public void onEventHandlerResult(Event event, FixtureState newState) {
                eventResultCalled.set(true);
            }
        });

        loggers.onCommandHandlerError(null, null);
        loggers.onCommandHandlerResult(null);
        loggers.onEventHandlerError(null, null, null);
        loggers.onEventHandlerResult(null, null);

        assertTrue(commandErrorCalled.get());
        assertTrue(commandResultCalled.get());
        assertTrue(eventErrorCalled.get());
        assertTrue(eventResultCalled.get());
    }

}