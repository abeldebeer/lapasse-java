package com.cookingfox.lapasse.impl.message.bus;

import com.cookingfox.lapasse.api.message.exception.NoMessageHandlersException;
import fixtures.example.event.CountIncremented;
import fixtures.message.ExtendedFixtureMessage;
import fixtures.message.FixtureMessage;
import fixtures.message.bus.FixtureMessageBus;
import fixtures.message.handler.FixtureMessageHandler;
import fixtures.message.store.FixtureMessageStore;
import org.junit.Before;
import org.junit.Test;
import testing.TestingUtils;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AbstractMessageBus}.
 */
public class AbstractMessageBusTest {

    //----------------------------------------------------------------------------------------------
    // TEST SETUP
    //----------------------------------------------------------------------------------------------

    FixtureMessageBus messageBus;
    private FixtureMessageStore messageStore;

    @Before
    public void setUp() throws Exception {
        messageStore = new FixtureMessageStore();
        messageBus = new FixtureMessageBus(messageStore);
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: constructor
    //----------------------------------------------------------------------------------------------

    @Test
    public void constructor_should_add_store_listener() throws Exception {
        assertEquals(1, messageStore.getMessageAddedListeners().size());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: dispose
    //----------------------------------------------------------------------------------------------

    @Test
    public void dispose_should_clear_mapped_handlers() throws Exception {
        messageBus.mapMessageHandler(FixtureMessage.class, new FixtureMessageHandler());

        assertEquals(1, messageBus.messageHandlerMap.size());

        messageBus.dispose();

        assertEquals(0, messageBus.messageHandlerMap.size());
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: handleMessage
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void handleMessage_null_message_should_throw() throws Exception {
        messageBus.handleMessage(null);
    }

    @Test(expected = NoMessageHandlersException.class)
    public void handleMessage_should_throw_if_no_mapped_handlers() throws Exception {
        messageBus.handleMessage(new FixtureMessage());
    }

    @Test
    public void handleMessage_should_execute_mapped_handler() throws Exception {
        FixtureMessage message = new FixtureMessage();
        FixtureMessageHandler handler = new FixtureMessageHandler();

        messageBus.mapMessageHandler(FixtureMessage.class, handler);
        messageBus.handleMessage(message);

        assertEquals(1, messageBus.executeHandlerCalls);
        assertEquals(1, handler.handledMessages.size());
        assertTrue(handler.handledMessages.contains(message));
    }

    @Test
    public void handleMessage_should_use_add_message_to_store() throws Exception {
        FixtureMessage message = new FixtureMessage();
        FixtureMessageHandler handler = new FixtureMessageHandler();

        messageBus.mapMessageHandler(FixtureMessage.class, handler);
        messageBus.handleMessage(message);

        assertTrue(messageStore.addedMessages.contains(message));
        assertEquals(1, messageStore.addedMessages.size());
    }

    @Test
    public void handleMessage_should_check_added_message_type() throws Exception {
        FixtureMessage message = new FixtureMessage();
        FixtureMessageHandler handler = new FixtureMessageHandler();

        messageBus.mapMessageHandler(FixtureMessage.class, handler);
        messageBus.handleMessage(message);

        assertEquals(1, messageBus.shouldHandleMessageCalls);
    }

    @Test
    public void handleMessage_should_use_handler_for_message_super_type() throws Exception {
        ExtendedFixtureMessage message = new ExtendedFixtureMessage();
        FixtureMessageHandler handler = new FixtureMessageHandler();

        messageBus.mapMessageHandler(FixtureMessage.class, handler);
        messageBus.handleMessage(message);

        assertEquals(1, messageBus.executeHandlerCalls);
        assertTrue(handler.handledMessages.contains(message));
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: mapMessageHandler
    //----------------------------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void mapMessageHandler_null_class_should_throw() throws Exception {
        messageBus.mapMessageHandler(null, new FixtureMessageHandler());
    }

    @Test(expected = NullPointerException.class)
    public void mapMessageHandler_null_handler_should_throw() throws Exception {
        messageBus.mapMessageHandler(FixtureMessage.class, null);
    }

    @Test
    public void mapMessageHandler_should_support_multiple_handlers_for_one_message() throws Exception {
        FixtureMessage message = new FixtureMessage();
        FixtureMessageHandler firstHandler = new FixtureMessageHandler();
        FixtureMessageHandler secondHandler = new FixtureMessageHandler();
        FixtureMessageHandler thirdHandler = new FixtureMessageHandler();

        messageBus.mapMessageHandler(FixtureMessage.class, firstHandler);
        messageBus.mapMessageHandler(FixtureMessage.class, secondHandler);
        messageBus.mapMessageHandler(FixtureMessage.class, thirdHandler);
        messageBus.handleMessage(message);

        assertEquals(3, messageBus.executeHandlerCalls);

        assertEquals(1, firstHandler.handledMessages.size());
        assertTrue(firstHandler.handledMessages.contains(message));

        assertEquals(1, secondHandler.handledMessages.size());
        assertTrue(secondHandler.handledMessages.contains(message));

        assertEquals(1, thirdHandler.handledMessages.size());
        assertTrue(thirdHandler.handledMessages.contains(message));
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: getMessageHandlers
    //----------------------------------------------------------------------------------------------

    @Test
    public void getMessageHandlers_should_not_throw_for_no_matching_type_handlers() throws Exception {
        Set<FixtureMessageHandler> handlers = messageBus.getMessageHandlers(CountIncremented.class);

        assertNull(handlers);
    }

    @Test
    public void getMessageHandlers_should_not_throw_for_no_matching_super_type_handlers() throws Exception {
        messageBus.mapMessageHandler(FixtureMessage.class, new FixtureMessageHandler());

        Set<FixtureMessageHandler> handlers = messageBus.getMessageHandlers(CountIncremented.class);

        assertNull(handlers);
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: onMessageAddedToStore
    //----------------------------------------------------------------------------------------------

    @Test
    public void onMessageAddedToStore_should_not_throw_for_no_message_handlers() throws Exception {
        messageBus.onMessageAddedToStore.onMessageAdded(new FixtureMessage());
    }

    @Test
    public void onMessageAddedToStore_should_not_throw_for_unsupported_message_type() throws Exception {
        messageBus.onMessageAddedToStore.onMessageAdded(new CountIncremented(123));
    }

    //----------------------------------------------------------------------------------------------
    // TESTS: CONCURRENCY (messageAddedListeners)
    //----------------------------------------------------------------------------------------------

    @Test
    public void messageAddedListeners_should_pass_concurrency_tests() throws Exception {
        TestingUtils.runConcurrencyTest(new Runnable() {
            @Override
            public void run() {
                messageBus.mapMessageHandler(FixtureMessage.class, new FixtureMessageHandler());
                messageBus.handleMessage(new FixtureMessage());
            }
        });
    }

}
