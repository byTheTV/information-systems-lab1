package ru.islab.labwork.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import ru.islab.labwork.dto.AddHardestRequest;
import ru.islab.labwork.dto.AddHardestResponse;
import ru.islab.labwork.dto.AverageResponse;
import ru.islab.labwork.dto.DecreaseRequest;
import ru.islab.labwork.dto.LabWorkDto;
import ru.islab.labwork.dto.UniquePointsResponse;
import ru.islab.labwork.security.AdminOnly;
import ru.islab.labwork.service.SpecialOperationService;

@Path("/special")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SpecialResource {

    @Inject
    SpecialOperationService specialOperationService;

    @GET
    @Path("/average-minimal-point")
    public AverageResponse average() {
        return specialOperationService.average();
    }

    @GET
    @Path("/by-description")
    public List<LabWorkDto> byDescription(@QueryParam("substring") String substring) {
        return specialOperationService.byDescription(substring);
    }

    @GET
    @Path("/unique-minimal-points")
    public UniquePointsResponse unique() {
        return specialOperationService.uniquePoints();
    }

    @POST
    @Path("/decrease-difficulty")
    @AdminOnly
    @Consumes(MediaType.APPLICATION_JSON)
    public LabWorkDto decrease(DecreaseRequest request) {
        return specialOperationService.decrease(request);
    }

    @POST
    @Path("/add-hardest")
    @AdminOnly
    @Consumes(MediaType.APPLICATION_JSON)
    public AddHardestResponse addHardest(AddHardestRequest request) {
        return specialOperationService.addHardest(request);
    }
}
