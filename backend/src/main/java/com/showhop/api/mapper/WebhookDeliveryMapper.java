package com.showhop.api.mapper;

import com.showhop.api.dto.WebhookDeliveryResponseDto;
import com.showhop.api.entity.WebhookDelivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WebhookDeliveryMapper {

  @Mapping(target = "eventType", source = "event.type")
  WebhookDeliveryResponseDto toResponseDto(WebhookDelivery delivery);
}
