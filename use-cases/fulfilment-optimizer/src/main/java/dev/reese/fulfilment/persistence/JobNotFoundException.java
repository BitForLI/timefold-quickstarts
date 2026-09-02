package dev.reese.fulfilment.persistence;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("No fulfilment plan job found for ID: " + jobId);
    }
}
