package com.secopshub.alert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secopshub.contracts.MessagingNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableRabbit
public class RabbitMessagingConfig {
    @Bean
    public DirectExchange alertsExchange() {
        return new DirectExchange(MessagingNames.ALERT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange alertDeadLetterExchange() {
        return new DirectExchange(MessagingNames.ALERT_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue incidentAlertQueue() {
        return QueueBuilder.durable(MessagingNames.INCIDENT_ALERT_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingNames.ALERT_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingNames.ALERT_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue incidentAlertDeadLetterQueue() {
        return QueueBuilder.durable(MessagingNames.INCIDENT_ALERT_DLQ).build();
    }

    @Bean
    public Binding incidentAlertBinding(@Qualifier("incidentAlertQueue") Queue incidentAlertQueue,
                                        @Qualifier("alertsExchange") DirectExchange alertsExchange) {
        return BindingBuilder.bind(incidentAlertQueue).to(alertsExchange).with(MessagingNames.ALERT_ROUTING_KEY);
    }

    @Bean
    public Binding incidentAlertDeadLetterBinding(@Qualifier("incidentAlertDeadLetterQueue") Queue incidentAlertDeadLetterQueue,
                                                  @Qualifier("alertDeadLetterExchange") DirectExchange alertDeadLetterExchange) {
        return BindingBuilder.bind(incidentAlertDeadLetterQueue).to(alertDeadLetterExchange)
                .with(MessagingNames.ALERT_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange alertStatusExchange() {
        return new DirectExchange(MessagingNames.ALERT_STATUS_EXCHANGE, true, false);
    }

    @Bean
    public Queue alertStatusQueue() {
        return QueueBuilder.durable(MessagingNames.ALERT_STATUS_QUEUE).build();
    }

    @Bean
    public Binding alertStatusBinding(@Qualifier("alertStatusQueue") Queue alertStatusQueue,
                                      @Qualifier("alertStatusExchange") DirectExchange alertStatusExchange) {
        return BindingBuilder.bind(alertStatusQueue).to(alertStatusExchange).with(MessagingNames.ALERT_STATUS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
