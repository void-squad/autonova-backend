using FluentValidation;
using ProjectService.Dtos.ChangeRequests;

namespace ProjectService.Validators.ChangeRequests;

public class CreateChangeRequestRequestValidator : AbstractValidator<CreateChangeRequestRequest>
{
    public CreateChangeRequestRequestValidator()
    {
        RuleFor(x => x.Title)
            .NotEmpty()
            .MaximumLength(120);

        RuleFor(x => x.Description)
            .MaximumLength(4000);

        RuleFor(x => x.ProposedPriceDelta)
            .GreaterThan(0m)
            .When(x => x.ProposedPriceDelta.HasValue);

        RuleFor(x => x.ProposedExtraHours)
            .GreaterThan(0)
            .When(x => x.ProposedExtraHours.HasValue);
    }
}
