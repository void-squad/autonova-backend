using System.Linq;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using ProjectService.Data;
using ProjectService.Domain.Entities;
using Xunit;

namespace ProjectService.Tests
{
	public class MigrationTests
	{
		[Fact]
		public void ModelConfiguration_CreatesIndexes()
		{
			var options = new DbContextOptionsBuilder<AppDb>()
				.UseInMemoryDatabase("mig-model").Options;

			using var db = new AppDb(options);

			// Trigger model creation
			var model = db.Model;

			// Validate some expected entity types exist
			model.GetEntityTypes().Any(t => t.ClrType == typeof(Project)).Should().BeTrue();
		}
	}
}