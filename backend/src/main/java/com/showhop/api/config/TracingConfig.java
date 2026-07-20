package com.showhop.api.config;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a {@link SpanExporter} bean; Spring Boot's OpenTelemetry
 * autoconfiguration picks up every such bean and wires it into the SDK
 * tracer provider automatically. No collector (Tempo/Jaeger/etc.) runs
 * locally, so spans render to the application log instead -- swap or add an
 * OTLP exporter later by setting {@code management.otlp.tracing.endpoint};
 * Spring Boot autoconfigures that exporter itself once the property exists.
 */
@Configuration
public class TracingConfig {

  @Bean
  public SpanExporter loggingSpanExporter() {
    return LoggingSpanExporter.create();
  }
}
