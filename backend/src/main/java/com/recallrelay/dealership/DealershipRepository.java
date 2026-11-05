package com.recallrelay.dealership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealershipRepository extends JpaRepository<Dealership, Long> {
    Optional<Dealership> findByIdAndOrganizationId(Long id, Long organizationId);
}
