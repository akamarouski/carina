package com.qaprosoft.carina.core.foundation.api.util;

import com.qaprosoft.carina.core.foundation.api.http.HttpMethodType;

public class RequestWrapper {

    private final StringBuilder bodyContent;
    private final String methodPath;
    private final HttpMethodType methodType;

    public RequestWrapper(StringBuilder bodyContent, String methodPath, HttpMethodType methodType) {
        this.bodyContent = bodyContent;
        this.methodPath = methodPath;
        this.methodType = methodType;
    }

    public StringBuilder getBodyContent() {
        return bodyContent;
    }

    public String getMethodPath() {
        return methodPath;
    }

    public HttpMethodType getMethodType() {
        return methodType;
    }
}
