package com.autonova.customer.event.auth;

import com.autonova.customer.service.AuthenticatedUserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes auth-service login events and delegates to the customer
 * synchronisation service.
 */
@Component
public class AuthUserLoginEventListener {

  private final Logger logger = LoggerFactory.getLogger(AuthUserLoginEventListener.class);
  private final AuthenticatedUserSyncService authenticatedUserSyncService;

  public AuthUserLoginEventListener(AuthenticatedUserSyncService authenticatedUserSyncService) {
    this.authenticatedUserSyncService = authenticatedUserSyncService;
  }

  @RabbitListener(queues = "${auth.events.login.queue:customer-service.auth.user-login}")
  public void onUserLoggedIn(AuthUserLoggedInEvent event) {
    try {
      authenticatedUserSyncService.syncCustomer(event);
    } catch (Exception ex) {
      logger.error("Failed to process auth login event", ex);
      throw ex;
    }
  }
}
