package com.petbuddy.user_profile_service.web.controller;

import com.petbuddy.user_profile_service.domain.user.ShippingAddress;
import com.petbuddy.user_profile_service.domain.user.User;
import com.petbuddy.user_profile_service.service.UserProfileService;
import com.petbuddy.user_profile_service.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/profile/me")
@Tag(name = "User Profile", description = "Endpoints for managing user profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

        private final UserProfileService userProfileService;

        public UserProfileController(UserProfileService userProfileService) {
                this.userProfileService = userProfileService;
        }

        @PostMapping
        @Operation(summary = "Create a new user profile")
        public ResponseEntity<UserProfileResponse> addUserProfile(Authentication auth,
                        @Valid @RequestBody UserProfileRequest request) {
                User user = userProfileService.addUserProfile(UUID.fromString(auth.getName()), request);

                return ResponseEntity.ok(new UserProfileResponse(
                                user.getId().toString(),
                                user.getAuthUserId(),
                                user.getEmail(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getBio(),
                                user.getAvatarUrl(),
                                user.getStripeCustomerId()));
        }

        @GetMapping
        @Operation(summary = "Get current user's profile")
        public ResponseEntity<UserProfileResponse> getProfile(Authentication auth,
                        @RequestHeader("X-DB-User-Id") String userId) {
                User user = userProfileService.getUserProfile(UUID.fromString(userId));

                if (!user.getAuthUserId().equals(auth.getName())) {
                        return ResponseEntity.status(403).build();
                }

                return ResponseEntity.ok(new UserProfileResponse(
                                user.getId().toString(),
                                user.getAuthUserId(),
                                user.getEmail(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getBio(),
                                user.getAvatarUrl(),
                                user.getStripeCustomerId()));
        }

        @PutMapping
        @Operation(summary = "Update current user's profile")
        public ResponseEntity<UserProfileResponse> updateProfile(
                        Authentication auth,
                        @RequestHeader("X-DB-User-Id") String userId,
                        @Valid @RequestBody UserProfileUpdateRequest request) {

                User updatedUser = userProfileService.updateUserProfile(
                                UUID.fromString(userId), request);

                return ResponseEntity.ok(new UserProfileResponse(
                                updatedUser.getId().toString(),
                                updatedUser.getAuthUserId(),
                                updatedUser.getEmail(),
                                updatedUser.getFirstName(),
                                updatedUser.getLastName(),
                                updatedUser.getBio(),
                                updatedUser.getAvatarUrl(),
                                updatedUser.getStripeCustomerId()));
        }

        @GetMapping("/addresses")
        @Operation(summary = "List all shipping addresses")
        public ResponseEntity<List<ShippingAddressDto>> listAddresses(Authentication auth,
                        @RequestHeader("X-DB-User-Id") String userId) {
                List<ShippingAddressDto> addresses = userProfileService
                                .getUserAddresses(UUID.fromString(userId), auth.getName())
                                .stream()
                                .map(addr -> new ShippingAddressDto(
                                                addr.getId(),
                                                addr.getAddressLine1(),
                                                addr.getAddressLine2(),
                                                addr.getCity(),
                                                addr.getState(),
                                                addr.getZipCode(),
                                                addr.getCountry(),
                                                addr.isDefault()))
                                .collect(Collectors.toList());

                return ResponseEntity.ok(addresses);
        }

        @PostMapping("/addresses")
        @Operation(summary = "Add a new shipping address")
        public ResponseEntity<ShippingAddressDto> addAddress(
                        Authentication auth,
                        @RequestHeader("X-DB-User-Id") String userId,
                        @Valid @RequestBody ShippingAddressDto request) {

                ShippingAddress address = new ShippingAddress();
                address.setAddressLine1(request.addressLine1());
                address.setAddressLine2(request.addressLine2());
                address.setCity(request.city());
                address.setState(request.state());
                address.setZipCode(request.zipCode());
                address.setCountry(request.country());
                address.setDefault(request.isDefault());

                ShippingAddress saved = userProfileService.addShippingAddress(
                                UUID.fromString(userId),
                                auth.getName(),
                                address);

                return ResponseEntity.ok(new ShippingAddressDto(
                                saved.getId(),
                                saved.getAddressLine1(),
                                saved.getAddressLine2(),
                                saved.getCity(),
                                saved.getState(),
                                saved.getZipCode(),
                                saved.getCountry(),
                                saved.isDefault()));
        }

        @DeleteMapping("/addresses/{addressId}")
        @Operation(summary = "Delete a shipping address")
        public ResponseEntity<Void> deleteAddress(
                        Authentication auth,
                        @PathVariable Long addressId) {

                userProfileService.deleteShippingAddress(
                                UUID.fromString(auth.getName()),
                                addressId);

                return ResponseEntity.noContent().build();
        }

        @PutMapping("/addresses/{addressId}/default")
        @Operation(summary = "Set an address as default")
        public ResponseEntity<ShippingAddressDto> setDefaultAddress(
                        Authentication auth,
                        @RequestHeader("X-DB-User-Id") String userId,
                        @PathVariable Long addressId) {

                ShippingAddress address = userProfileService.setDefaultAddress(
                                UUID.fromString(userId),
                                auth.getName(),
                                addressId);

                return ResponseEntity.ok(new ShippingAddressDto(
                                address.getId(),
                                address.getAddressLine1(),
                                address.getAddressLine2(),
                                address.getCity(),
                                address.getState(),
                                address.getZipCode(),
                                address.getCountry(),
                                address.isDefault()));
        }
}