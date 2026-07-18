package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderCommitteeAssignmentRepository extends JpaRepository<TenderCommitteeAssignment, Long> {

    List<TenderCommitteeAssignment> findByTenderIdAndActiveTrue(Long tenderId);

    boolean existsByTenderIdAndUserIdAndActiveTrue(Long tenderId, Long userId);
}
