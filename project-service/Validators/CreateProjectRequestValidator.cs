using FluentValidation;
using ProjectService.Dtos;

namespace ProjectService.Validators;

public class CreateProjectRequestValidator : AbstractValidator<CreateProjectRequest>
{
    public CreateProjectRequestValidator()
    {
        RuleFor(x => x.VehicleId)
            .NotEmpty();

        RuleFor(x => x.Title)
            .NotEmpty()
            .MaximumLength(200);

        RuleFor(x => x.Description)
            .MaximumLength(4000);

        RuleForEach(x => x.Tasks)
            .SetValidator(new CreateProjectTaskRequestValidator());
    }
}

public class CreateProjectTaskRequestValidator : AbstractValidator<CreateProjectTaskRequest>
{
    public CreateProjectTaskRequestValidator()
    {
        RuleFor(x => x.Title)
            .NotEmpty()
            .MaximumLength(200);

        RuleFor(x => x.ServiceType)
            .NotEmpty()
            .MaximumLength(160);

        RuleFor(x => x.Detail)
            .MaximumLength(2000);
    }
}
