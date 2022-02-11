package com.qaprosoft.carina.core.foundation.api.util;

import com.qaprosoft.carina.core.foundation.api.exception.ParamsNotInitialized;
import com.qaprosoft.carina.core.foundation.utils.common.CommonUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ActionPoller<B> {

    private Duration timeout;
    private Duration pollingInterval;
    private Supplier<B> resultSupplier;
    private Predicate<B> predicate;

    protected ActionPoller(Builder<?, B> builder) {
        this.timeout = builder.timeout;
        this.pollingInterval = builder.pollingInterval;
        this.resultSupplier = builder.resultSupplier;
        this.predicate = builder.predicate;
        isParamsInitialized();
    }

    public static Builder builder() {
        return new Builder() {

            @Override public Builder getThis() {
                return this;
            }
        };
    }

    public abstract static class Builder<T extends Builder<T, K>, K> {

        private Duration timeout;
        private Duration pollingInterval;
        private Supplier<K> resultSupplier;
        private Predicate<K> predicate;

        public abstract T getThis();

        /**
         * Sets the time to repeat the action
         *
         * @param period   The interval through which the action will be repeated
         * @param timeUnit unit of time in which the repetition will occur
         * @return builder
         */
        public T pollEvery(long period, TemporalUnit timeUnit) {
            this.pollingInterval = Duration.of(period, timeUnit);
            return this.getThis();
        }

        /**
         * Sets the time after which the repetition of the action will stop
         *
         * @param timeout  timeout to successfully complete a recurring action
         * @param timeUnit unit of time in which the timeout will be indicated
         * @return builder
         */
        public T stopAfter(long timeout, TemporalUnit timeUnit) {
            this.timeout = Duration.of(timeout, timeUnit);
            return this.getThis();
        }

        /**
         * Sets the action that will be repeated
         *
         * @param resultSupplier lambda expression that will be repeated
         * @return builder
         */
        public T action(Supplier<K> resultSupplier) {
            this.resultSupplier = resultSupplier;
            return this.getThis();
        }

        /**
         * Sets a condition in case of successful execution  of which the repetitions will stop
         *
         * @param predicate lambda expression that returns true if the received value from action suits
         *                  us and we want to stop repeation and false otherwise
         * @return builder
         */
        public T until(Predicate<K> predicate) {
            this.predicate = predicate;
            return this.getThis();
        }

        /**
         * A function that starts repeating the specified action
         *
         * @return the value of the action expression if the until method succeeds, and null otherwise
         */
        public K execute() {
            AtomicBoolean stopExecution = setupTerminateTask(timeout.toMillis());
            K result = null;
            while (!stopExecution.get()) {
                K tempResult = resultSupplier.get();
                if (predicate.test(tempResult)) {
                    result = tempResult;
                    break;
                }
                CommonUtils.pause(pollingInterval.getSeconds());
            }
            return result;
        }

        public ActionPoller<?> build() {
            return new ActionPoller<>(this);
        }

        protected Duration getTimeout() {
            return timeout;
        }

        protected Duration getPollingInterval() {
            return pollingInterval;
        }

        protected Supplier<K> getResultSupplier() {
            return resultSupplier;
        }

        protected Predicate<K> getPredicate() {
            return predicate;
        }

        protected static AtomicBoolean setupTerminateTask(Long timeoutInMillis) {
            AtomicBoolean stopExecution = new AtomicBoolean();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stopExecution.set(true);
                    timer.cancel();
                    timer.purge();
                }
            }, timeoutInMillis);
            return stopExecution;
        }
    }

    private void isParamsInitialized() {
        if (!ObjectUtils.allNotNull(timeout, pollingInterval, resultSupplier, predicate)) {
            throw new ParamsNotInitialized("One or more parameters of ActionPoller object is not initialized ", new Throwable());
        }
    }
}
