package com.qaprosoft.carina.core.foundation.api.util;

import com.qaprosoft.carina.core.foundation.api.exception.TaskExecuteException;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ActionPoller<T> {

    private Duration timeout;
    private Duration pollingInterval;
    private Supplier<T> task;
    private Predicate<T> successfulCondition;

    private ActionPoller() {
    }

    public static <T> ActionPoller<T> builder() {
        return new ActionPoller<>();
    }

    /**
     * Sets the repetition interval for lambda expression that should be passed to the task function
     *
     * @param period   repetition interval
     * @param timeUnit time unit
     * @return object of ActionPoller class for setting other parameters for builder or calling execute method for
     * getting the final result
     */
    public ActionPoller<T> pollEvery(long period, TemporalUnit timeUnit) {
        this.pollingInterval = Duration.of(period, timeUnit);
        return this;
    }

    /**
     * Sets a timeout for executing the lambda expression that should be passed to the task function
     *
     * @param timeout  timeout for task
     * @param timeUnit time unit
     * @return object of ActionPoller class for setting other parameters for builder or calling execute method for
     * getting the final result
     */
    public ActionPoller<T> stopAfter(long timeout, TemporalUnit timeUnit) {
        this.timeout = Duration.of(timeout, timeUnit);
        return this;
    }

    /**
     * Accepts a lambda expression that will repeat
     *
     * @param task lambda expression to re-execute
     * @return object of ActionPoller class for setting other parameters for builder or calling execute method for
     * * getting the final result
     */
    public ActionPoller<T> task(Supplier<T> task) {
        this.task = task;
        return this;
    }

    /**
     * Sets the condition under which the task is considered successfully completed and the result is returned
     *
     * @param successfulCondition lambda expression that that should return true if we consider the task completed
     *                            successfully, and false if not
     * @return object of ActionPoller class for setting other parameters for builder or calling execute method for
     * getting the final result
     */
    public ActionPoller<T> until(Predicate<T> successfulCondition) {
        this.successfulCondition = successfulCondition;
        return this;
    }

    /**
     * Execute  task in intervals with timeout. If condition, that should be set in until function returns true, this
     * method returns result of task, otherwise returns null
     *
     * @return result of task method
     * @throws TaskExecuteException
     */
    public T execute() throws TaskExecuteException {
        AtomicBoolean stopExecution = setupTerminateTask();
        T result = null;
        while (!stopExecution.get()) {
            T tempResult = task.get();
            if (successfulCondition.test(tempResult)) {
                result = tempResult;
            } else {
                sleep();
            }
        }
        return result;
    }

    private void sleep() throws TaskExecuteException {
        try {
            Thread.sleep(pollingInterval.toMillis());
        } catch (InterruptedException e) {
            throw new TaskExecuteException("Error when try to cause thread sleeping. " + e.getMessage(), e);
        }
    }

    private AtomicBoolean setupTerminateTask() {
        AtomicBoolean stopExecution = new AtomicBoolean();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopExecution.set(true);
                timer.cancel();
                timer.purge();
            }
        }, timeout.toMillis());
        return stopExecution;
    }
}
