package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dto.CreateAddressRequest;
import com.octopus.demo.userservice.dto.UpdateAddressRequest;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.service.AddressService;
import com.octopus.demo.userservice.vo.AddressVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public R<PageResultBean<AddressVO>> getAddressesByUserId(@PathVariable Long userId, PageQueryBean query) {
        PageResultBean<Address> result = addressService.findByUserId(userId, query);
        PageResultBean<AddressVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(AddressVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }

    @GetMapping("/{id}")
    public R<AddressVO> getAddress(@PathVariable Long userId, @PathVariable Long id) {
        return addressService.findById(id)
                .filter(a -> a.getUserId().equals(userId))
                .map(AddressVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "地址不存在, id: " + id));
    }

    @PostMapping
    public R<AddressVO> createAddress(@PathVariable Long userId,
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
                .map(AddressVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "用户不存在, id: " + userId));
    }

    @PutMapping("/{id}")
    public R<AddressVO> updateAddress(@PathVariable Long userId,
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
                .map(AddressVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "地址不存在, id: " + id));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long id) {
        boolean deleted = addressService.deleteAddress(id, userId);
        if (!deleted) return R.fail(404, "地址不存在, id: " + id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    public R<AddressVO> setDefaultAddress(@PathVariable Long userId, @PathVariable Long id) {
        return addressService.setDefaultAddress(id, userId)
                .map(AddressVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "地址不存在, id: " + id));
    }
}
