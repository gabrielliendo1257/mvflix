package com.gcorp.service.app.mvflix_activity.infrastructure;

import com.gcorp.service.app.mvflix_activity.application.*;
import com.gcorp.service.app.mvflix_activity.application.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import io.r2dbc.spi.ConnectionFactory;

@Configuration
public class ActivityConfiguration {
  @Bean R2dbcTransactionManager connectionFactoryTransactionManager(ConnectionFactory cf) { return new R2dbcTransactionManager(cf); }
  @Bean TransactionalOperator transactionalOperator(R2dbcTransactionManager tm) { return TransactionalOperator.create(tm); }
  @Bean ActivityProcessor activityProcessor(ActivityInbox inbox, WatchActivityRepository projection, TransactionalOperator tx) { return new ActivityProcessor(inbox, projection, tx); }
  @Bean ActivityQueryService activityQueryService(WatchActivityRepository repository) { return new ActivityQueryService(repository); }
}
