using ProjectService.Domain.Entities;
using ProjectService.Dtos.ChangeRequests;

namespace ProjectService.Extensions;

public static class ChangeRequestMappingExtensions
{
    public static ChangeRequestResponse ToResponse(this ChangeRequest entity) => new()
    {
        ChangeRequestId = entity.ChangeRequestId,
        ProjectId = entity.ProjectId,
        Title = entity.Title,
        Description = entity.Description,
        ProposedPriceDelta = entity.ProposedPriceDelta,
        ProposedExtraHours = entity.ProposedExtraHours,
        ProposedNewDueDate = entity.ProposedNewDueDate,
        Status = entity.Status,
        CreatedBy = entity.CreatedBy,
        CreatedAt = entity.CreatedAt,
        DecidedBy = entity.DecidedBy,
        DecidedAt = entity.DecidedAt,
        ClientRequestId = entity.ClientRequestId,
        xmin = entity.xmin
    };
}
