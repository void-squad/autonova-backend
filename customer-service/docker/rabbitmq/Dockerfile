# Dockerfile to run RabbitMQ for the customer-service microservice
# Uses the management-enabled official image so the management console is available on port 15672
FROM rabbitmq:3.13-management

# Declare defaults that mirror customer-service/.env values
ENV RABBITMQ_DEFAULT_USER=guest \
  RABBITMQ_DEFAULT_PASS=guest \
  RABBITMQ_DEFAULT_VHOST=/

# Copy optional definitions file to pre-create exchange and queues
# (Uncomment the COPY line and provide definitions.json to seed infrastructure.)
# COPY definitions.json /etc/rabbitmq/

# Enable the management plugin explicitly (already on in the -management image, kept for clarity)
RUN rabbitmq-plugins enable --offline rabbitmq_management

# Expose the AMQP and management ports
EXPOSE 5672 15672

# Leverage the default command
CMD ["rabbitmq-server"]
