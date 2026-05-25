package com.octopus.demo.userservice.controller;

import com.octopus.demo.userservice.dto.CreateAddressRequest;
import com.octopus.demo.userservice.dto.UpdateAddressRequest;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<Address>> getAddressesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.findByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddress(@PathVariable Long userId, @PathVariable Long id) {
        return addressService.findById(id)
                .filter(a -> a.getUserId().equals(userId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "地址不存在, id: " + id)));
    }

    @PostMapping
    public ResponseEntity<?> createAddress(@PathVariable Long userId,
                                           @Valid @RequestBody CreateAddressRequest request) {
        Address address = new Address();
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setPostalCode(request.getPostalCode());
        address.setIsDefault(request.getIsDefault());

        return addressService.createAddress(userId, address)
                .<ResponseEntity<?>>map(a -> ResponseEntity.status(HttpStatus.CREATED).body(a))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "用户不存在, id: " + userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable Long userId,
                                           @PathVariable Long id,
                                           @Valid @RequestBody UpdateAddressRequest request) {
        Address address = new Address();
        if (request.getReceiverName() != null) address.setReceiverName(request.getReceiverName());
        if (request.getReceiverPhone() != null) address.setReceiverPhone(request.getReceiverPhone());
        if (request.getProvince() != null) address.setProvince(request.getProvince());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getDistrict() != null) address.setDistrict(request.getDistrict());
        if (request.getDetailAddress() != null) address.setDetailAddress(request.getDetailAddress());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getIsDefault() != null) address.setIsDefault(request.getIsDefault());

        return addressService.updateAddress(id, userId, address)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "地址不存在, id: " + id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long userId, @PathVariable Long id) {
        boolean deleted = addressService.deleteAddress(id, userId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "地址不存在, id: " + id));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable Long userId, @PathVariable Long id) {
        return addressService.setDefaultAddress(id, userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "地址不存在, id: " + id)));
    }
}