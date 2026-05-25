package com.octopus.demo.userservice.controller;

import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressController.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    private final LocalDateTime now = LocalDateTime.now();

    private Address createTestAddress() {
        return new Address(1L, 1L, "张三", "13800138000", "北京市", "北京市",
                "朝阳区", "某某街道某某号", "100000", true, now, now);
    }

    // ===== GET /api/users/{userId}/addresses =====

    @Test
    void shouldGetAddressesByUserId() throws Exception {
        Address address = createTestAddress();
        when(addressService.findByUserId(1L)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiverName").value("张三"))
                .andExpect(jsonPath("$[0].receiverPhone").value("13800138000"))
                .andExpect(jsonPath("$[0].province").value("北京市"))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void shouldReturnEmptyListWhenNoAddresses() throws Exception {
        when(addressService.findByUserId(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/999/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===== GET /api/users/{userId}/addresses/{id} =====

    @Test
    void shouldGetAddressById() throws Exception {
        Address address = createTestAddress();
        when(addressService.findById(1L)).thenReturn(Optional.of(address));

        mockMvc.perform(get("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverName").value("张三"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldReturn404WhenAddressNotFound() throws Exception {
        when(addressService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/1/addresses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不存在, id: 999"));
    }

    @Test
    void shouldReturn404WhenAddressBelongsToDifferentUser() throws Exception {
        // Address belongs to user 2, but request is for user 1
        Address address = new Address(1L, 2L, "张三", "13800138000", "北京市", "北京市",
                "朝阳区", "某某街道某某号", "100000", true, now, now);
        when(addressService.findById(1L)).thenReturn(Optional.of(address));

        mockMvc.perform(get("/api/users/1/addresses/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不存在, id: 1"));
    }

    // ===== POST /api/users/{userId}/addresses =====

    @Test
    void shouldCreateAddress() throws Exception {
        Address address = createTestAddress();
        when(addressService.createAddress(eq(1L), any(Address.class))).thenReturn(Optional.of(address));

        String body = """
                {
                    "receiverName": "张三",
                    "receiverPhone": "13800138000",
                    "province": "北京市",
                    "city": "北京市",
                    "district": "朝阳区",
                    "detailAddress": "某某街道某某号",
                    "postalCode": "100000",
                    "isDefault": true
                }""";

        mockMvc.perform(post("/api/users/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiverName").value("张三"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldReturn404WhenCreatingAddressForNonExistentUser() throws Exception {
        when(addressService.createAddress(eq(999L), any(Address.class))).thenReturn(Optional.empty());

        String body = """
                {
                    "receiverName": "张三",
                    "receiverPhone": "13800138000",
                    "province": "北京市",
                    "city": "北京市",
                    "district": "朝阳区",
                    "detailAddress": "某某街道某某号"
                }""";

        mockMvc.perform(post("/api/users/999/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("用户不存在, id: 999"));
    }

    @Test
    void shouldReturn400WhenCreatingWithInvalidData() throws Exception {
        String body = """
                {
                    "receiverName": "",
                    "receiverPhone": "abc",
                    "province": "",
                    "city": "",
                    "district": "",
                    "detailAddress": ""
                }""";

        mockMvc.perform(post("/api/users/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ===== PUT /api/users/{userId}/addresses/{id} =====

    @Test
    void shouldUpdateAddress() throws Exception {
        Address updated = new Address(1L, 1L, "李四", "13900139000", "上海市", "上海市",
                "浦东新区", "某某路某某号", "200000", false, now, now);
        when(addressService.updateAddress(eq(1L), eq(1L), any(Address.class))).thenReturn(Optional.of(updated));

        String body = """
                {
                    "receiverName": "李四",
                    "receiverPhone": "13900139000",
                    "province": "上海市",
                    "city": "上海市",
                    "district": "浦东新区",
                    "detailAddress": "某某路某某号",
                    "postalCode": "200000",
                    "isDefault": false
                }""";

        mockMvc.perform(put("/api/users/1/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverName").value("李四"))
                .andExpect(jsonPath("$.province").value("上海市"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAddress() throws Exception {
        when(addressService.updateAddress(eq(999L), eq(1L), any(Address.class))).thenReturn(Optional.empty());
        when(addressService.findById(999L)).thenReturn(Optional.empty());

        String body = """
                {
                    "receiverName": "李四",
                    "receiverPhone": "13900139000"
                }""";

        mockMvc.perform(put("/api/users/1/addresses/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不存在, id: 999"));
    }

    @Test
    void shouldReturn404WhenUpdatingAddressBelongsToDifferentUser() throws Exception {
        // Address 1 belongs to user 1, but request is from user 2
        Address otherUserAddress = new Address(1L, 1L, "张三", "13800138000", "北京市", "北京市",
                "朝阳区", "某某街道某某号", "100000", true, now, now);
        when(addressService.updateAddress(eq(1L), eq(2L), any(Address.class))).thenReturn(Optional.empty());
        when(addressService.findById(1L)).thenReturn(Optional.of(otherUserAddress));

        String body = """
                {
                    "receiverName": "李四"
                }""";

        mockMvc.perform(put("/api/users/2/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不属于该用户"));
    }

    @Test
    void shouldReturn400WhenUpdatingWithInvalidPhone() throws Exception {
        String body = """
                {
                    "receiverPhone": "abc"
                }""";

        mockMvc.perform(put("/api/users/1/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE /api/users/{userId}/addresses/{id} =====

    @Test
    void shouldDeleteAddress() throws Exception {
        when(addressService.deleteAddress(1L, 1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1/addresses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentAddress() throws Exception {
        when(addressService.deleteAddress(999L, 1L)).thenReturn(false);
        when(addressService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/users/1/addresses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不存在, id: 999"));
    }

    @Test
    void shouldReturn404WhenDeletingAddressBelongsToDifferentUser() throws Exception {
        // Address 1 belongs to user 1, but request is from user 2
        Address otherUserAddress = new Address(1L, 1L, "张三", "13800138000", "北京市", "北京市",
                "朝阳区", "某某街道某某号", "100000", true, now, now);
        when(addressService.deleteAddress(1L, 2L)).thenReturn(false);
        when(addressService.findById(1L)).thenReturn(Optional.of(otherUserAddress));

        mockMvc.perform(delete("/api/users/2/addresses/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不属于该用户"));
    }

    // ===== PUT /api/users/{userId}/addresses/{id}/default =====

    @Test
    void shouldSetDefaultAddress() throws Exception {
        Address address = createTestAddress();
        when(addressService.setDefaultAddress(1L, 1L)).thenReturn(Optional.of(address));

        mockMvc.perform(put("/api/users/1/addresses/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void shouldReturn404WhenSettingDefaultForNonExistentAddress() throws Exception {
        when(addressService.setDefaultAddress(999L, 1L)).thenReturn(Optional.empty());
        when(addressService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/1/addresses/999/default"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不存在, id: 999"));
    }

    @Test
    void shouldReturn404WhenSettingDefaultForAddressBelongsToDifferentUser() throws Exception {
        // Address 1 belongs to user 1, but request is from user 2
        Address otherUserAddress = new Address(1L, 1L, "张三", "13800138000", "北京市", "北京市",
                "朝阳区", "某某街道某某号", "100000", true, now, now);
        when(addressService.setDefaultAddress(1L, 2L)).thenReturn(Optional.empty());
        when(addressService.findById(1L)).thenReturn(Optional.of(otherUserAddress));

        mockMvc.perform(put("/api/users/2/addresses/1/default"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("地址不属于该用户"));
    }
}