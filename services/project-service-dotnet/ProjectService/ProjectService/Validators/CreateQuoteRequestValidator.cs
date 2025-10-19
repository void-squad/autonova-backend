using FluentValidation;
using ProjectService.Dtos;

namespace ProjectService.Validators;

public class CreateQuoteRequestValidator : AbstractValidator<CreateQuoteRequest>
{
    public CreateQuoteRequestValidator()
    {
        RuleFor(x => x.Total)
            .GreaterThan(0m);
    }
}
