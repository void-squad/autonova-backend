using ProjectService.Domain.Entities;
using ProjectService.Dtos.Tasks;

namespace ProjectService.Mapping;

public static class TaskMapping
{
    public static TaskResponse ToDto(this TaskItem entity, bool includeProject = false)
    {
        var response = new TaskResponse
        {
            TaskId = entity.TaskId,
            ProjectId = entity.ProjectId,
            Title = entity.Title,
            Description = null,
            AssigneeId = entity.AssigneeId,
            Status = entity.Status,
            EstimateHours = entity.EstimateHours,
            CreatedAt = DateTimeOffset.MinValue
        };

        if (includeProject && entity.Project is not null)
        {
            response.Project = new TaskProjectSummary
            {
                ProjectId = entity.Project.ProjectId,
                Title = entity.Project.Title,
                Status = entity.Project.Status.ToString()
            };
        }

        return response;
    }
}
