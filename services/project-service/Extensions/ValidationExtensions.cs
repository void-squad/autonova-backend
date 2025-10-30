using System.Linq;
using FluentValidation.Results;

namespace ProjectService.Extensions;

public static class ValidationExtensions
{
    public static IDictionary<string, string[]> ToProblemDictionary(this ValidationResult validationResult) =>
        validationResult.Errors
            .GroupBy(e => e.PropertyName, StringComparer.OrdinalIgnoreCase)
            .ToDictionary(
                g => g.Key,
                g => g.Select(e => e.ErrorMessage).Distinct().ToArray(),
                StringComparer.OrdinalIgnoreCase);
}
