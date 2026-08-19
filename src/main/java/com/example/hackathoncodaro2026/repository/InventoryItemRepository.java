package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByResourceTypeAndEnabledTrueOrderByNameAsc(ResourceType resourceType);

    Optional<InventoryItem> findByNameIgnoreCaseAndResourceType(String name, ResourceType resourceType);

    boolean existsByNameIgnoreCaseAndResourceType(String name, ResourceType resourceType);
}
