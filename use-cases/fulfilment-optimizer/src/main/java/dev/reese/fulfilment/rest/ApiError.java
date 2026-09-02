package dev.reese.fulfilment.rest;

public record ApiError(int status, String error, String message) {
}
