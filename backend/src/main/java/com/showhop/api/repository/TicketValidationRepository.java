package com.showhop.api.repository;

import com.showhop.api.entity.TicketValidation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketValidationRepository extends JpaRepository<TicketValidation, UUID> {
}
