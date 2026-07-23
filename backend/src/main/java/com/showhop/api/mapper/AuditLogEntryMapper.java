package com.showhop.api.mapper;

import com.showhop.api.dto.AuditLogEntryResponseDto;
import com.showhop.api.entity.AuditLogEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogEntryMapper {

  AuditLogEntryResponseDto toResponseDto(AuditLogEntry entry);
}
