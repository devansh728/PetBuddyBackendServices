package com.petbuddy.user_profile_service.service;

import com.petbuddy.user_profile_service.domain.user.*;
import com.petbuddy.user_profile_service.exception.ResourceNotFoundException;
import com.petbuddy.user_profile_service.util.AESUtil;
import com.petbuddy.user_profile_service.util.HashUtil;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.petbuddy.user_profile_service.web.dto.UserProfileRequest;
import com.petbuddy.user_profile_service.web.dto.UserProfileUpdateRequest;

import java.util.List;
import java.util.UUID;

@Service
public class UserProfileService {

    private final AESUtil aesUtil;
    private final UserRepository userRepository;
    private final ShippingAddressRepository shippingAddressRepository;

    public UserProfileService(AESUtil aesUtil,
            UserRepository userRepository,
            ShippingAddressRepository shippingAddressRepository) {
        this.aesUtil = aesUtil;
        this.userRepository = userRepository;
        this.shippingAddressRepository = shippingAddressRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user-profile", key = "#userId")
    public User getUserProfile(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User addUserProfile(UUID userId, UserProfileRequest request) {
        User user = new User();
        user.setAuthUserId(userId.toString());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setGender(getGender(request.getGender()));
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPhoneHashEncrypted(encryptPhoneNumber(request.getPhoneNumber()));
        user.setPhoneSearchHash(HashUtil.sha256(request.getPhoneNumber()));
        user.setPhoneVerified(false);
        user.setPhoneVerificationToken(null);
        user.setStripeCustomerId(null);
        return userRepository.save(user);
    }

    private Gender getGender(String gender) {
        return Gender.valueOf(gender.toUpperCase());
    }

    private String encryptPhoneNumber(String phoneNumber) {
        return aesUtil.encrypt(phoneNumber);
    }

    public String getDecryptedPhone(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return aesUtil.decrypt(user.getPhoneHashEncrypted());
    }

    @Transactional
    @CacheEvict(value = "user-profile", key = "#userId")
    public User updateUserProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setAge(request.age());
        user.setGender(getGender(request.gender()));
        user.setAvatarUrl(request.avatarUrl());
        user.setPhoneHashEncrypted(encryptPhoneNumber(request.phoneNumber()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user-addresses", key = "#userId")
    public List<ShippingAddress> getUserAddresses(UUID userId, String authUserId) {
        return shippingAddressRepository.findAllByUserId(userId);
    }

    @Transactional
    @CacheEvict(value = {"user-profile", "user-addresses"}, key = "#userId")
    public ShippingAddress addShippingAddress(UUID userId, String authUserId, ShippingAddress address) {

        if (!userRepository.existsByIdAndAuthUserId(userId, authUserId)) {
            throw new ResourceNotFoundException("User not found");
        }

        if (address.isDefault()) {
            shippingAddressRepository.clearDefaultAddresses(userId);
        }

        User userRef = userRepository.getReferenceById(userId);
        address.setUser(userRef);

        return shippingAddressRepository.save(address);
    }

    @Transactional
    @CacheEvict(value = "user-addresses", key = "#userId")
    public void deleteShippingAddress(UUID userId, Long addressId) {
        shippingAddressRepository.deleteByIdAndUserId(addressId, userId);
    }

    @Transactional
    @CacheEvict(value = "user-addresses", key = "#userId")
    public ShippingAddress setDefaultAddress(UUID userId, String authUserId, Long addressId) {
        if (!userRepository.existsByIdAndAuthUserId(userId, authUserId)) {
            throw new ResourceNotFoundException("User not found");
        }

        ShippingAddress newDefaultAddress = shippingAddressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Reset old default and set new default
        shippingAddressRepository.clearDefaultAddresses(userId);
        newDefaultAddress.setDefault(true);
        return shippingAddressRepository.save(newDefaultAddress);
    }
}