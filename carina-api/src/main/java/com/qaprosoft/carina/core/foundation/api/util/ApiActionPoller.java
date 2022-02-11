package com.qaprosoft.carina.core.foundation.api.util;

import com.qaprosoft.carina.core.foundation.api.AbstractApiMethodV2;
import com.qaprosoft.carina.core.foundation.utils.common.CommonUtils;
import io.restassured.response.Response;
import org.apache.commons.lang3.ObjectUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ApiActionPoller extends ActionPoller<Response> {

    private AbstractApiMethodV2 apiMethod;
    private Predicate<RequestWrapper> requestLoggingCondition;
    private Predicate<Response> responseLoggingCondition;

    public ApiActionPoller(Builder builder) {
        super(builder);
        this.apiMethod = builder.apiMethod;
        this.requestLoggingCondition = builder.requestLoggingCondition;
        this.responseLoggingCondition = builder.responseLoggingCondition;
        isParamsInitialized();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ActionPoller.Builder<Builder, Response> {

        private AbstractApiMethodV2 apiMethod;
        private Predicate<RequestWrapper> requestLoggingCondition;
        private Predicate<Response> responseLoggingCondition;

        @Override
        public Builder getThis() {
            return this;
        }

        public ApiActionPoller build() {
            return new ApiActionPoller(this);
        }

        /**
         * Sets the object whose methods we will call when logging
         *
         * @param apiMethod object of AbstractApiMethodV2 class
         * @return builder
         */
        public Builder apiMethod(AbstractApiMethodV2 apiMethod) {
            this.apiMethod = apiMethod;
            return this;
        }

        /**
         * Allows you to specify a lambda expression that allows you to control the output of query logs
         *
         * @param condition lambda expression that sets the query output condition
         * @return builder
         */
        public Builder requestLogging(Predicate<RequestWrapper> condition) {
            this.requestLoggingCondition = condition;
            return this;
        }

        /**
         * Allows you to specify a lambda expression that allows you to control the output of response logs
         *
         * @param condition lambda expression that sets the response output condition
         * @return builder
         */
        public Builder responseLogging(Predicate<Response> condition) {
            this.responseLoggingCondition = condition;
            return this;
        }

        /**
         * A function that starts repeating the specified action (by default calling api function from AbstractApiMethodV2 class)
         *
         * @return response if the until method succeeds, and null otherwise
         */
        @Override
        public Response execute() {
            AtomicBoolean stopExecution = setupTerminateTask(getTimeout().toMillis());
            Response result = null;
            while (!stopExecution.get()) {
                apiMethod.setRequestLogCondition(requestLoggingCondition);
                apiMethod.setResponseLogCondition(responseLoggingCondition);
                Response tempResult = getResultSupplier().get();
                if (getPredicate().test(tempResult)) {
                    result = tempResult;
                    break;
                }
                CommonUtils.pause(getPollingInterval().getSeconds());
            }
            return result;
        }
    }

    private void isParamsInitialized() {
        if (!ObjectUtils.allNotNull(apiMethod, requestLoggingCondition, responseLoggingCondition)) {
            throw new RuntimeException("One or many parameters of ApiActionPoller object is not initialized");
        }
    }
}
