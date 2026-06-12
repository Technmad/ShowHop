package com.showhop.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serializes {@code Page<T>} responses via Spring Data's stable DTO shape
 * instead of {@code PageImpl} directly -- the default emits a startup
 * warning because {@code PageImpl}'s JSON shape isn't a supported contract.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
