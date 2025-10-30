using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Options;
using ProjectService.Data;
using RabbitMQ.Client;

namespace ProjectService.Messaging;

public class OutboxDispatcher : BackgroundService
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly IOptions<RabbitOptions> _options;
    private readonly ILogger<OutboxDispatcher> _logger;
    private IConnection? _connection;
    private IModel? _channel;

    public OutboxDispatcher(IServiceScopeFactory scopeFactory, IOptions<RabbitOptions> options, ILogger<OutboxDispatcher> logger)
    {
        _scopeFactory = scopeFactory;
        _options = options;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("Outbox dispatcher starting.");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                if (!_options.Value.Enabled)
                {
                    await Task.Delay(TimeSpan.FromSeconds(5), stoppingToken);
                    continue;
                }

                using var scope = _scopeFactory.CreateScope();
                var db = scope.ServiceProvider.GetRequiredService<AppDb>();

                var messages = await db.Outbox
                    .Where(m => m.DispatchedAt == null)
                    .OrderBy(m => m.CreatedAt)
                    .Take(25)
                    .ToListAsync(stoppingToken);

                if (messages.Count == 0)
                {
                    await Task.Delay(TimeSpan.FromSeconds(2), stoppingToken);
                    continue;
                }

                var channel = await EnsureChannelAsync(stoppingToken);
                if (channel is null)
                {
                    await Task.Delay(TimeSpan.FromSeconds(5), stoppingToken);
                    continue;
                }

                foreach (var message in messages)
                {
                    try
                    {
                        var body = Encoding.UTF8.GetBytes(message.Payload);
                        channel.BasicPublish(
                            exchange: _options.Value.Exchange,
                            routingKey: message.Topic,
                            basicProperties: null,
                            body: body);

                        message.DispatchedAt = DateTimeOffset.UtcNow;
                    }
                    catch (Exception ex)
                    {
                        _logger.LogError(ex, "Failed to dispatch outbox message {MessageId}", message.Id);
                        break;
                    }
                }

                await db.SaveChangesAsync(stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Outbox dispatcher encountered an error.");
                await Task.Delay(TimeSpan.FromSeconds(5), stoppingToken);
            }
        }
    }

    private async Task<IModel?> EnsureChannelAsync(CancellationToken cancellationToken)
    {
        if (_channel is { IsOpen: true })
        {
            return _channel;
        }

        await DisposeChannelAsync();

        try
        {
            var factory = new ConnectionFactory
            {
                HostName = _options.Value.HostName,
                UserName = _options.Value.UserName,
                Password = _options.Value.Password,
                DispatchConsumersAsync = true
            };

            _connection = factory.CreateConnection();
            _channel = _connection.CreateModel();
            _channel.ExchangeDeclare(exchange: _options.Value.Exchange, type: ExchangeType.Topic, durable: true, autoDelete: false);
            _logger.LogInformation("Connected to RabbitMQ at {Host}", _options.Value.HostName);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unable to establish RabbitMQ connection.");
            await Task.Delay(TimeSpan.FromSeconds(5), cancellationToken);
            return null;
        }

        return _channel;
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        await base.StopAsync(cancellationToken);
        await DisposeChannelAsync();
    }

    private Task DisposeChannelAsync()
    {
        try
        {
            _channel?.Close();
            _channel?.Dispose();
            _connection?.Close();
            _connection?.Dispose();
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Error disposing RabbitMQ channel.");
        }
        finally
        {
            _channel = null;
            _connection = null;
        }

        return Task.CompletedTask;
    }
}
