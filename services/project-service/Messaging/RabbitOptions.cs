namespace ProjectService.Messaging;

public class RabbitOptions
{
    public string HostName { get; set; } = "rabbitmq";
    public string UserName { get; set; } = "guest";
    public string Password { get; set; } = "guest";
    public string Exchange { get; set; } = "autonova.events";
    public bool Enabled { get; set; } = true;
}
