package com.secopshub.incident.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secopshub.contracts.MessagingNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
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
    public Binding incidentAlertBinding(@Qualifier("incidentAlertQueue") Queue queue,
                                        @Qualifier("alertsExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MessagingNames.ALERT_ROUTING_KEY);
    }

    @Bean
    public Binding incidentAlertDeadLetterBinding(@Qualifier("incidentAlertDeadLetterQueue") Queue queue,
                                                  @Qualifier("alertDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MessagingNames.ALERT_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange alertStatusExchange() {
        return new DirectExchange(MessagingNames.ALERT_STATUS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
