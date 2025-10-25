using FluentValidation;
using ProjectService.Domain.Enums;
using ProjectService.Dtos;

namespace ProjectService.Validators;

public class UpdateProjectStatusRequestValidator : AbstractValidator<UpdateProjectStatusRequest>
{
    public UpdateProjectStatusRequestValidator()
    {
        RuleFor(x => x.NewStatus)
            .Must(status => Enum.IsDefined(typeof(ProjectStatus), status))
            .WithMessage("Status is not recognised.");
    }
}
