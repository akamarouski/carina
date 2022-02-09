package com.qaprosoft.carina.core.foundation.api.util;

import com.qaprosoft.carina.core.foundation.api.AbstractApiMethodV2;
import com.qaprosoft.carina.core.foundation.api.exception.TaskExecuteException;
import io.restassured.response.Response;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ApiActionPoller {

    private AbstractApiMethodV2 apiMethod;
    private Duration timeout;
    private Duration pollingInterval;
    private Predicate<Response> successfulCondition;
    private Predicate<String> rqLogCondition;
    private Predicate<String> rsLogCondition;

    private ApiActionPoller(AbstractApiMethodV2 apiMethod) {
        this.apiMethod = apiMethod;
    }

    public static ApiActionPoller builder(AbstractApiMethodV2 apiMethod) {
        return new ApiActionPoller(apiMethod);
    }

    /**
     * Sets the repetition interval for lambda expression that should be passed to the task function
     *
     * @param period   repetition interval
     * @param timeUnit time unit
     * @return object of ActionPoller class for setting other parameters for builder or calling execute method for
     * getting the final result
     */
    public ApiActionPoller pollEvery(long period, TemporalUnit timeUnit) {
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
    public ApiActionPoller stopAfter(long timeout, TemporalUnit timeUnit) {
        this.timeout = Duration.of(timeout, timeUnit);
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
    public ApiActionPoller until(Predicate<Response> successfulCondition) {
        this.successfulCondition = successfulCondition;
        return this;
    }

    /**
     * Execute  task in intervals with timeout.
     *
     * @return response if condition, that was set in until method, otherwise null
     */
    public Response execute() throws TaskExecuteException {
        AtomicBoolean stopExecution = setupTerminateTask(timeout.toMillis());
        Response response = null;
        while (!stopExecution.get()) {
            Response tempResult = apiMethod.callAPI();
            if (successfulCondition.test(tempResult)) {
                response = tempResult;
            } else {
                sleep(pollingInterval.toMillis());
            }
            logRequestResponseByCondition(apiMethod.getRequestBody(), response != null ? response.asString() : null);
        }
        return response;
    }

    public ApiActionPoller setLoggingStrategy(Predicate<String> rqLogCondition, Predicate<String> rsLogCondition) {
        this.rqLogCondition = rqLogCondition;
        this.rsLogCondition = rsLogCondition;
        return this;
    }

    public void logRequestResponseByCondition(String request, String response) {
        apiMethod.setLogRequest(rqLogCondition.test(request));
        apiMethod.setLogResponse(rsLogCondition.test(response));
        apiMethod.logRequest();
        apiMethod.logResponse();
    }

    public void sleep(long time) throws TaskExecuteException {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new TaskExecuteException("Error when try to cause thread sleeping. " + e.getMessage(), e);
        }
    }

    public AtomicBoolean setupTerminateTask(long timeout) {
        AtomicBoolean stopExecution = new AtomicBoolean();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopExecution.set(true);
                timer.cancel();
                timer.purge();
            }
        }, timeout);
        return stopExecution;
    }
}
