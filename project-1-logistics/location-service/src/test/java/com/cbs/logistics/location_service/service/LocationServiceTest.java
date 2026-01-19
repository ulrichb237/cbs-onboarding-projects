package com.cbs.logistics.location_service.service;

import com.cbs.logistics.location_service.client.PackageServiceClient;
import com.cbs.logistics.location_service.dto.CreateLocationRequest;
import com.cbs.logistics.location_service.dto.EnrichedLocationDto;
import com.cbs.logistics.location_service.dto.LocationDto;
import com.cbs.logistics.location_service.entity.Location;
import com.cbs.logistics.location_service.exception.LocationNotFoundException;
import com.cbs.logistics.location_service.locationMapper.LocationMapper;
import com.cbs.logistics.location_service.repository.LocationRepository;
import com.cbs.logistics.package_service.dto.PackageDto;
import com.cbs.logistics.package_service.entity.PackageStatus;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private PackageServiceClient packageServiceClient;

    @InjectMocks
    private LocationService locationService;

    private CreateLocationRequest createRequest;
    private Location location;
    private LocationDto locationDto;
    private PackageDto packageDto;

    @BeforeEach
    void setUp() {
        createRequest = CreateLocationRequest.builder()
                .city("Paris")
                .zone("Zone Nord")
                .checkpointAvailable(true)
                .packageId(1L)
                .build();

        location = Location.builder()
                .locationId("507f1f77bcf86cd799439011")
                .city("Paris")
                .zone("Zone Nord")
                .checkpointAvailable(true)
                .packageId(1L)
                .build();

        locationDto = new LocationDto("Paris", "Zone Nord", true);

        packageDto = new PackageDto(1L, "Colis fragile", "Ordinateur", 
                "Électronique", 2.5, true, PackageStatus.NEW);
    }

    @Test
    void create_ShouldCreateLocation_WhenPackageExists() {
        when(packageServiceClient.getPackageById(1L)).thenReturn(packageDto);
        when(locationMapper.toEntity(createRequest)).thenReturn(location);
        when(locationRepository.save(location)).thenReturn(location);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        LocationDto result = locationService.create(createRequest);

        assertNotNull(result);
        assertEquals("Paris", result.city());
        assertEquals("Zone Nord", result.zone());
        assertTrue(result.checkpointAvailable());
        verify(packageServiceClient).getPackageById(1L);
        verify(locationRepository).save(location);
    }

    @Test
    void create_ShouldThrowException_WhenPackageNotFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/packages/1",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        when(packageServiceClient.getPackageById(1L))
                .thenThrow(new FeignException.NotFound("Not found", request, null, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.create(createRequest));
        assertTrue(exception.getMessage().contains("Package avec id 1"));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void getById_ShouldReturnLocation_WhenExists() {
        String locationId = "507f1f77bcf86cd799439011";
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        LocationDto result = locationService.getById(locationId);

        assertNotNull(result);
        assertEquals("Paris", result.city());
        verify(locationRepository).findById(locationId);
    }

    @Test
    void getById_ShouldThrowException_WhenNotFound() {
        String locationId = "nonexistent";
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class,
                () -> locationService.getById(locationId));
    }

    @Test
    void getAll_ShouldReturnPageOfLocations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Location> locationPage = new PageImpl<>(List.of(location));
        when(locationRepository.findAll(pageable)).thenReturn(locationPage);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        Page<LocationDto> result = locationService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Paris", result.getContent().get(0).city());
        verify(locationRepository).findAll(pageable);
    }

    @Test
    void getByPackageId_ShouldReturnEnrichedLocation_WhenExists() {
        Long packageId = 1L;
        when(locationRepository.findByPackageId(packageId)).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);
        when(packageServiceClient.getPackageById(packageId)).thenReturn(packageDto);

        EnrichedLocationDto result = locationService.getByPackageId(packageId);

        assertNotNull(result);
        assertNotNull(result.location());
        assertNotNull(result.packageInfo());
        assertEquals("Paris", result.location().city());
        assertEquals("Ordinateur", result.packageInfo().packageName());
        verify(locationRepository).findByPackageId(packageId);
        verify(packageServiceClient).getPackageById(packageId);
    }

    @Test
    void getByPackageId_ShouldThrowException_WhenLocationNotFound() {
        Long packageId = 999L;
        when(locationRepository.findByPackageId(packageId)).thenReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class,
                () -> locationService.getByPackageId(packageId));
        verify(packageServiceClient, never()).getPackageById(any());
    }

    @Test
    void getByPackageId_ShouldThrowException_WhenPackageServiceFails() {
        Long packageId = 1L;
        Request request = Request.create(Request.HttpMethod.GET, "/api/packages/1",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        when(locationRepository.findByPackageId(packageId)).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);
        when(packageServiceClient.getPackageById(packageId))
                .thenThrow(new FeignException.ServiceUnavailable("Service unavailable", request, null, null));

        assertThrows(RuntimeException.class,
                () -> locationService.getByPackageId(packageId));
    }
}
