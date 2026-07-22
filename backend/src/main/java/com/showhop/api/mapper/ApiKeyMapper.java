package com.showhop.api.mapper;

import com.showhop.api.dto.ApiKeyResponseDto;
import com.showhop.api.entity.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiKeyMapper {

  @Mapping(target = "key", ignore = true)
  ApiKeyResponseDto toResponseDto(ApiKey apiKey);

  /** Used only right after creation -- the raw key is never persisted or shown again. */
  @Mapping(target = "key", source = "rawKey")
  ApiKeyResponseDto toResponseDtoWithKey(ApiKey apiKey, String rawKey);
}
