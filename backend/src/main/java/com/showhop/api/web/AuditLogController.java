package com.showhop.api.web;

import static com.showhop.api.security.JwtUtil.parseUserId;

import com.showhop.api.dto.AuditLogEntryResponseDto;
import com.showhop.api.mapper.AuditLogEntryMapper;
import com.showhop.api.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditLogService auditLogService;
  private final AuditLogEntryMapper auditLogEntryMapper;

  @GetMapping
  public Page<AuditLogEntryResponseDto> listAuditLog(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    return auditLogService.listForOrganizer(parseUserId(jwt), pageable)
        .map(auditLogEntryMapper::toResponseDto);
  }
}
