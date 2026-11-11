package com.ketan.payflow.common;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String type;    // slug, e.g. "insufficient-funds"
    private final String title;   // short human-readable title

    protected ApiException(HttpStatus status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public HttpStatus status() { return status; }

    public ProblemDetail toProblem(String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, getMessage());
        pd.setType(URI.create("/errors/" + type));
        pd.setTitle(title);
        pd.setInstance(URI.create(path));
        return pd;
    }
}
