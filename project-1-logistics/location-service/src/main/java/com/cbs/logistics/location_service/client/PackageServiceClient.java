package com.cbs.logistics.location_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.cbs.logistics.package_service.dto.PackageDto;

@FeignClient(name = "package-service", url = "http://localhost:8081")
public interface PackageServiceClient {

    @GetMapping("/api/packages/{id}")
    PackageDto getPackageById(@PathVariable Long id);
}