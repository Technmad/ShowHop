package com.showhop.api.repository;

import com.showhop.api.entity.ProcessedRazorpayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedRazorpayEventRepository extends JpaRepository<ProcessedRazorpayEvent, String> {
}
