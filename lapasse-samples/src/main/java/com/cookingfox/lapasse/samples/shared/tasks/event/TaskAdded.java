package com.cookingfox.lapasse.samples.shared.tasks.event;

import com.cookingfox.lapasse.api.event.Event;
import com.cookingfox.lapasse.samples.shared.tasks.entity.Task;

import java.util.Objects;

/**
 * Task added event.
 */
public final class TaskAdded implements Event {

    private final Task task;

    public TaskAdded(Task task) {
        this.task = Objects.requireNonNull(task);
    }

    public Task getTask() {
        return task;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TaskAdded && o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return task.hashCode();
    }

    @Override
    public String toString() {
        return "TaskAdded{" +
                "task=" + task +
                '}';
    }

}
