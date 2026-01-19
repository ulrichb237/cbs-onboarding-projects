package com.cbs.logistics.location_service.dto;

import com.cbs.logistics.package_service.dto.PackageDto;

public record EnrichedLocationDto(LocationDto location, PackageDto packageInfo) {
}