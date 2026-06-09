package com.showhop.api.repository;

import com.showhop.api.entity.TicketType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
}
