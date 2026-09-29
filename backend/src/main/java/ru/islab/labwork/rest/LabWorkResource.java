package ru.islab.labwork.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import ru.islab.labwork.dto.DeleteInfo;
import ru.islab.labwork.dto.DeleteResult;
import ru.islab.labwork.dto.LabWorkBriefDto;
import ru.islab.labwork.dto.LabWorkDto;
import ru.islab.labwork.dto.LabWorkInput;
import ru.islab.labwork.dto.PageDto;
import ru.islab.labwork.security.AdminOnly;
import ru.islab.labwork.service.LabWorkService;

@Path("/labworks")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class LabWorkResource {

    @Inject
    LabWorkService labWorkService;

    @GET
    public PageDto<LabWorkDto> list(@QueryParam("page") Integer page,
                                    @QueryParam("size") Integer size,
                                    @QueryParam("name") String name,
                                    @QueryParam("description") String description,
                                    @QueryParam("difficulty") String difficulty,
                                    @QueryParam("disciplineName") String disciplineName,
                                    @QueryParam("authorName") String authorName,
                                    @QueryParam("sort") String sort,
                                    @QueryParam("order") String order) {
        return labWorkService.list(page, size, name, description, difficulty, disciplineName, authorName, sort, order);
    }

    @GET
    @Path("/brief")
    public List<LabWorkBriefDto> brief() {
        return labWorkService.briefs();
    }

    @GET
    @Path("/{id}")
    public LabWorkDto get(@PathParam("id") int id) {
        return labWorkService.get(id);
    }

    @GET
    @Path("/{id}/delete-info")
    public DeleteInfo deleteInfo(@PathParam("id") int id) {
        return labWorkService.deleteInfo(id);
    }

    @POST
    @AdminOnly
    @Consumes(MediaType.APPLICATION_JSON)
    public LabWorkDto create(LabWorkInput input) {
        return labWorkService.create(input);
    }

    @PUT
    @Path("/{id}")
    @AdminOnly
    @Consumes(MediaType.APPLICATION_JSON)
    public LabWorkDto update(@PathParam("id") int id, LabWorkInput input) {
        return labWorkService.update(id, input);
    }

    @DELETE
    @Path("/{id}")
    @AdminOnly
    public DeleteResult delete(@PathParam("id") int id, @QueryParam("replacementId") Integer replacementId) {
        return labWorkService.delete(id, replacementId);
    }
}
