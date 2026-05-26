package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
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

    @Test
    void shouldGetAddressesByUserId() throws Exception {
        Address address = createTestAddress();
        PageResultBean<Address> result = new PageResultBean<>();
        result.setCount(1);
        result.setList(List.of(address));
        when(addressService.findByUserId(eq(1L), any(PageQueryBean.class))).thenReturn(result);

        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].receiverName").value("张三"));
    }

    @Test
    void shouldReturnEmptyListWhenNoAddresses() throws Exception {
        PageResultBean<Address> result = new PageResultBean<>();
        result.setCount(0);
        result.setList(List.of());
        when(addressService.findByUserId(eq(999L), any(PageQueryBean.class))).thenReturn(result);

        mockMvc.perform(get("/api/users/999/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    void shouldGetAddressById() throws Exception {
        Address address = createTestAddress();
        when(addressService.findById(1L)).thenReturn(Optional.of(address));

        mockMvc.perform(get("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.receiverName").value("张三"));
    }

    @Test
    void shouldReturn404WhenAddressNotFound() throws Exception {
        when(addressService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/1/addresses/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("地址不存在, id: 999"));
    }

    @Test
    void shouldReturn404WhenAddressBelongsToDifferentUser() throws Exception {
        Address address = new Address(1L, 2L, "张三", "13800138000", "北京市", "北京市",
                "朝阳区", "某某街道某某号", "100000", true, now, now);
        when(addressService.findById(1L)).thenReturn(Optional.of(address));

        mockMvc.perform(get("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

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
                }
                """;

        mockMvc.perform(post("/api/users/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.receiverName").value("张三"));
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
                }
                """;

        mockMvc.perform(post("/api/users/999/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("用户不存在, id: 999"));
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
                }
                """;

        mockMvc.perform(post("/api/users/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

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
                }
                """;

        mockMvc.perform(put("/api/users/1/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.receiverName").value("李四"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAddress() throws Exception {
        when(addressService.updateAddress(eq(999L), eq(1L), any(Address.class))).thenReturn(Optional.empty());

        String body = """
                {
                    "receiverName": "李四",
                    "receiverPhone": "13900139000"
                }
                """;

        mockMvc.perform(put("/api/users/1/addresses/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturn404WhenUpdatingAddressBelongsToDifferentUser() throws Exception {
        when(addressService.updateAddress(eq(1L), eq(2L), any(Address.class))).thenReturn(Optional.empty());

        String body = """
                {
                    "receiverName": "李四"
                }
                """;

        mockMvc.perform(put("/api/users/2/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturn400WhenUpdatingWithInvalidPhone() throws Exception {
        String body = """
                {
                    "receiverPhone": "abc"
                }
                """;

        mockMvc.perform(put("/api/users/1/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteAddress() throws Exception {
        when(addressService.deleteAddress(1L, 1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentAddress() throws Exception {
        when(addressService.deleteAddress(999L, 1L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/1/addresses/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturn404WhenDeletingAddressBelongsToDifferentUser() throws Exception {
        when(addressService.deleteAddress(1L, 2L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/2/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldSetDefaultAddress() throws Exception {
        Address address = createTestAddress();
        when(addressService.setDefaultAddress(1L, 1L)).thenReturn(Optional.of(address));

        mockMvc.perform(put("/api/users/1/addresses/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void shouldReturn404WhenSettingDefaultForNonExistentAddress() throws Exception {
        when(addressService.setDefaultAddress(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/1/addresses/999/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturn404WhenSettingDefaultForAddressBelongsToDifferentUser() throws Exception {
        when(addressService.setDefaultAddress(1L, 2L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/2/addresses/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
