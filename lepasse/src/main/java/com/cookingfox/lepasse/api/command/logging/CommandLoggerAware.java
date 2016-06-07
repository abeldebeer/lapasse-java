package com.cookingfox.lepasse.api.command.logging;

import com.cookingfox.lepasse.api.state.State;

/**
 * Interface for elements that can have a command logger added.
 *
 * @param <S> The concrete type of the state object.
 */
public interface CommandLoggerAware<S extends State> {

    /**
     * Add a command logger.
     *
     * @param logger The command logger to add.
     */
    void addCommandLogger(CommandLogger<S> logger);

}
