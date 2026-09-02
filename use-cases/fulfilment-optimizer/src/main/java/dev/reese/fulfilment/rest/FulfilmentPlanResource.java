package dev.reese.fulfilment.rest;

import dev.reese.fulfilment.domain.FulfilmentPlan;
import dev.reese.fulfilment.service.FulfilmentPlanningService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/fulfilment-plans")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FulfilmentPlanResource {

    private final FulfilmentPlanningService service;

    @Inject
    public FulfilmentPlanResource(FulfilmentPlanningService service) {
        this.service = service;
    }

    @POST
    public Response submit(@HeaderParam("Idempotency-Key") String idempotencyKey, FulfilmentPlan problem) {
        JobSubmissionResponse response = service.submit(idempotencyKey, problem);
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @POST
    @Path("/baseline")
    public FulfilmentPlan baseline(FulfilmentPlan problem) {
        return service.baseline(problem);
    }

    @GET
    @Path("/{jobId}")
    public PlanJobResponse get(@PathParam("jobId") String jobId) {
        return service.get(jobId);
    }

    @DELETE
    @Path("/{jobId}")
    public PlanJobResponse cancel(@PathParam("jobId") String jobId) {
        return service.cancel(jobId);
    }
}
