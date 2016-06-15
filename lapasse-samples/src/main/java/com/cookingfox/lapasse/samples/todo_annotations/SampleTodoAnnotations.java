package com.cookingfox.lapasse.samples.todo_annotations;

import com.cookingfox.lapasse.api.command.Command;
import com.cookingfox.lapasse.api.event.Event;
import com.cookingfox.lapasse.api.logging.CombinedLogger;
import com.cookingfox.lapasse.api.state.observer.OnStateChanged;
import com.cookingfox.lapasse.impl.facade.LaPasseFacade;
import com.cookingfox.lapasse.samples.shared.todo.command.AddTask;
import com.cookingfox.lapasse.samples.shared.todo.command.CompleteTask;
import com.cookingfox.lapasse.samples.shared.todo.command.RemoveTask;
import com.cookingfox.lapasse.samples.shared.todo.event.TaskAdded;
import com.cookingfox.lapasse.samples.shared.todo.state.TodoState;
import com.cookingfox.lapasse.samples.todo_annotations.facade.TodoFacadeAnnotations;

import java.util.Collection;
import java.util.UUID;

/**
 * Sample application using core LaPasse library with annotations.
 */
public class SampleTodoAnnotations implements CombinedLogger<TodoState>, OnStateChanged<TodoState> {

    public static void main(String[] args) throws Exception {
        new SampleTodoAnnotations().init();
    }

    private UUID firstCreatedTaskId;

    private void init() throws Exception {
        System.out.println("SAMPLE: " + getClass().getSimpleName());

        // create initial state
        TodoState initialState = TodoState.createInitialState();

        // create facade
        TodoFacadeAnnotations facade = new TodoFacadeAnnotations(new LaPasseFacade.Builder<>(initialState).build());

        // subscribe to state changes
        facade.subscribe(this);

        // log operations
        facade.addLogger(this);

        System.out.println("\nINITIAL STATE: " + initialState);

        System.out.println("\n>>> ADD TASK");
        facade.handleCommand(new AddTask("First task"));

        System.out.println("\n>>> MARK TASK COMPLETE");
        facade.handleCommand(new CompleteTask(firstCreatedTaskId));

        System.out.println("\n>>> REMOVE TASK");
        facade.handleCommand(new RemoveTask(firstCreatedTaskId));
    }

    @Override
    public void onStateChanged(TodoState state, Event event) {
        System.out.println("STATE CHANGED: " + state);

        // task added: save generated task UUID so we can use it in subsequent commands
        if (event instanceof TaskAdded) {
            firstCreatedTaskId = ((TaskAdded) event).getTask().getId();
        }
    }

    @Override
    public void onCommandHandlerError(Throwable error, Command command, Collection<Event> events) {
        error.printStackTrace();
    }

    @Override
    public void onCommandHandlerResult(Command command, Collection<Event> events) {
        System.out.println("COMMAND HANDLER RESULT: " + command);
    }

    @Override
    public void onEventHandlerError(Throwable error, Event event, TodoState newState) {
        error.printStackTrace();
    }

    @Override
    public void onEventHandlerResult(Event event, TodoState newState) {
        System.out.println("EVENT HANDLER RESULT: " + event);
    }

}