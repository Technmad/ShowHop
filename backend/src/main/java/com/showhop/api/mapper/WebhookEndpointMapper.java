package com.showhop.api.mapper;

import com.showhop.api.dto.WebhookEndpointResponseDto;
import com.showhop.api.entity.WebhookEndpoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WebhookEndpointMapper {

  @Mapping(target = "secret", ignore = true)
  WebhookEndpointResponseDto toResponseDto(WebhookEndpoint endpoint);

  /** Used only right after registration or a secret rotation. */
  WebhookEndpointResponseDto toResponseDtoWithSecret(WebhookEndpoint endpoint);
}
