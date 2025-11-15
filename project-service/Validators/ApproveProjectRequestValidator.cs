using FluentValidation;
using ProjectService.Dtos;

namespace ProjectService.Validators;

public class ApproveProjectRequestValidator : AbstractValidator<ApproveProjectRequest>
{
    public ApproveProjectRequestValidator()
    {
        RuleForEach(x => x.Tasks)
            .SetValidator(new ApproveProjectTaskUpdateValidator());
    }
}

public class ApproveProjectTaskUpdateValidator : AbstractValidator<ApproveProjectTaskUpdate>
{
    public ApproveProjectTaskUpdateValidator()
    {
        RuleFor(x => x.TaskId)
            .NotEmpty();
    }
}

public class UpdateTaskStatusRequestValidator : AbstractValidator<ProjectService.Dtos.UpdateTaskStatusRequest>
{
    public UpdateTaskStatusRequestValidator()
    {
        RuleFor(x => x.Note)
            .MaximumLength(1000);
    }
}
