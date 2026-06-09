package com.showhop.api.repository;

import com.showhop.api.entity.QrCode;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {
}
