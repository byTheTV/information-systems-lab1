package ru.islab.labwork.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import ru.islab.labwork.dto.CoordinatesDto;
import ru.islab.labwork.dto.DeleteInfo;
import ru.islab.labwork.dto.DeleteResult;
import ru.islab.labwork.dto.DisciplineDto;
import ru.islab.labwork.dto.LocationDto;
import ru.islab.labwork.dto.PersonDto;
import ru.islab.labwork.security.AdminOnly;
import ru.islab.labwork.service.CatalogService;

@Path("/catalog")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {

    @Inject
    CatalogService catalogService;

    @GET
    @Path("/coordinates")
    public List<CoordinatesDto> coordinates() {
        return catalogService.coordinates();
    }

    @GET
    @Path("/disciplines")
    public List<DisciplineDto> disciplines() {
        return catalogService.disciplines();
    }

    @GET
    @Path("/persons")
    public List<PersonDto> persons() {
        return catalogService.persons();
    }

    @GET
    @Path("/locations")
    public List<LocationDto> locations() {
        return catalogService.locations();
    }

    @GET
    @Path("/coordinates/{id}/delete-info")
    public DeleteInfo coordinatesInfo(@PathParam("id") long id) {
        return catalogService.coordinatesInfo(id);
    }

    @DELETE
    @Path("/coordinates/{id}")
    @AdminOnly
    public DeleteResult deleteCoordinates(@PathParam("id") long id, @QueryParam("replacementId") Long replacementId) {
        return catalogService.deleteCoordinates(id, replacementId);
    }

    @GET
    @Path("/disciplines/{id}/delete-info")
    public DeleteInfo disciplineInfo(@PathParam("id") long id) {
        return catalogService.disciplineInfo(id);
    }

    @DELETE
    @Path("/disciplines/{id}")
    @AdminOnly
    public DeleteResult deleteDiscipline(@PathParam("id") long id, @QueryParam("replacementId") Long replacementId) {
        return catalogService.deleteDiscipline(id, replacementId);
    }

    @GET
    @Path("/persons/{id}/delete-info")
    public DeleteInfo personInfo(@PathParam("id") long id) {
        return catalogService.personInfo(id);
    }

    @DELETE
    @Path("/persons/{id}")
    @AdminOnly
    public DeleteResult deletePerson(@PathParam("id") long id, @QueryParam("replacementId") Long replacementId) {
        return catalogService.deletePerson(id, replacementId);
    }

    @GET
    @Path("/locations/{id}/delete-info")
    public DeleteInfo locationInfo(@PathParam("id") long id) {
        return catalogService.locationInfo(id);
    }

    @DELETE
    @Path("/locations/{id}")
    @AdminOnly
    public DeleteResult deleteLocation(@PathParam("id") long id, @QueryParam("replacementId") Long replacementId) {
        return catalogService.deleteLocation(id, replacementId);
    }
}
