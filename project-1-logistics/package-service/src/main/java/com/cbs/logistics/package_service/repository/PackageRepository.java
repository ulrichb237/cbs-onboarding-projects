package com.cbs.logistics.package_service.repository;

import com.cbs.logistics.package_service.entity.Package;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;

public interface PackageRepository extends JpaRepository<Package ,Long>{
    Page<Package> findAll(Pageable page);
}
