# 收货地址功能 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。所有 git 命须在项目目录 `projects/xzf-octopus-demo-service-user` 内执行，不能在 workspace 根目录执行。

**目标：** 为用户服务新增收货地址管理功能，支持多地址、默认地址互斥逻辑、完整 CRUD + 设为默认 API。

**架构：** 嵌套资源模式，地址作为 `/api/users/{userId}/addresses` 下的子资源。沿用现有分层架构（Controller → Service → DAO → Model），使用 ConcurrentHashMap 内存存储，新增独立的 Address DAO/Service/Controller 与 User 模块同级。

**技术栈：** Spring Boot 3.2.5, Java 17, Jakarta Bean Validation, JUnit 5, Mockito, MockMvc, Maven

---

## 文件结构

### 创建文件

| 文件 | 职责 |
|------|------|
| `src/main/java/com/octopus/demo/userservice/model/Address.java` | 地址 POJO，含全字段 + getter/setter + 全参数构造器 + 无参构造器 |
| `src/main/java/com/octopus/demo/userservice/dao/AddressDao.java` | 地址 DAO 接口，定义 findByUserId, findById, findDefaultByUserId, save, update, deleteById |
| `src/main/java/com/octopus/demo/userservice/dao/impl/InMemoryAddressDao.java` | ConcurrentHashMap 内存实现，含 defaultAddressMap 互斥映射 |
| `src/main/java/com/octopus/demo/userservice/dto/CreateAddressRequest.java` | 创建地址 DTO，含 Jakarta Bean Validation 注解 |
| `src/main/java/com/octopus/demo/userservice/dto/UpdateAddressRequest.java` | 更新地址 DTO，所有字段可选 |
| `src/main/java/com/octopus/demo/userservice/service/AddressService.java` | 地址 Service 接口 |
| `src/main/java/com/octopus/demo/userservice/service/impl/AddressServiceImpl.java` | 地址 Service 实现，构造器注入 UserDao + AddressDao |
| `src/main/java/com/octopus/demo/userservice/controller/AddressController.java` | 地址 REST Controller，基路径 `/api/users/{userId}/addresses` |
| `src/test/java/com/octopus/demo/userservice/dao/InMemoryAddressDaoTest.java` | DAO 层单元测试 |
| `src/test/java/com/octopus/demo/userservice/service/AddressServiceImplTest.java` | Service 层单元测试（Mockito） |
| `src/test/java/com/octopus/demo/userservice/controller/AddressControllerTest.java` | Controller 层 MockMvc 测试 |

### 修改文件

| 文件 | 变更 |
|------|------|
| 无 | 不修改任何现有文件，所有新增为独立文件 |

---

## 任务 1：Address 模型

**文件：**
- 创建：`src/main/java/com/octopus/demo/userservice/model/Address.java`
- 测试：无需独立测试（POJO 通过 DAO 测试间接覆盖）

- [ ] **步骤 1：创建 Address.java**

```java
package com.octopus.demo.userservice.model;

import java.time.LocalDateTime;

public class Address {

    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String postalCode;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Address() {
    }

    public Address(Long id, Long userId, String receiverName, String receiverPhone,
                   String province, String city, String district, String detailAddress,
                   String postalCode, Boolean isDefault, LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.province = province;
        this.city = city;
        this.district = district;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
cd projects/xzf-octopus-demo-service-user
git add src/main/java/com/octopus/demo/userservice/model/Address.java
git commit -m "feat: add Address model POJO"
```

---

## 任务 2：CreateAddressRequest DTO

**文件：**
- 创建：`src/main/java/com/octopus/demo/userservice/dto/CreateAddressRequest.java`
- 测试：通过 Controller 测试间接覆盖验证规则

- [ ] **步骤 1：创建 CreateAddressRequest.java**

```java
package com.octopus.demo.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateAddressRequest {

    @NotBlank(message = "收件人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收件人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    private String postalCode;

    private Boolean isDefault;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
cd projects/xzf-octopus-demo-service-user
git add src/main/java/com/octopus/demo/userservice/dto/CreateAddressRequest.java
git commit -m "feat: add CreateAddressRequest DTO with validation"
```

---

## 任务 3：UpdateAddressRequest DTO

**文件：**
- 创建：`src/main/java/com/octopus/demo/userservice/dto/UpdateAddressRequest.java`
- 测试：通过 Controller 测试间接覆盖

- [ ] **步骤 1：创建 UpdateAddressRequest.java**

```java
package com.octopus.demo.userservice.dto;

import jakarta.validation.constraints.Pattern;

public class UpdateAddressRequest {

    private String receiverName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String postalCode;
    private Boolean isDefault;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
cd projects/xzf-octopus-demo-service-user
git add src/main/java/com/octopus/demo/userservice/dto/UpdateAddressRequest.java
git commit -m "feat: add UpdateAddressRequest DTO with optional fields"
```

---

## 任务 4：AddressDao 接口与 InMemoryAddressDao 实现（TDD）

**文件：**
- 创建：`src/main/java/com/octopus/demo/userservice/dao/AddressDao.java`
- 创建：`src/main/java/com/octopus/demo/userservice/dao/impl/InMemoryAddressDao.java`
- 创建：`src/test/java/com/octopus/demo/userservice/dao/InMemoryAddressDaoTest.java`

- [ ] **步骤 1：编写失败的测试 — InMemoryAddressDaoTest.java**

```java
package com.octopus.demo.userservice.dao;

import com.octopus.demo.userservice.dao.impl.InMemoryAddressDao;
import com.octopus.demo.userservice.model.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAddressDaoTest {

    private InMemoryAddressDao addressDao;

    @BeforeEach
    void setUp() {
        addressDao = new InMemoryAddressDao();
    }

    @Test
    void shouldSaveAddressAndAssignId() {
        Address address = createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        Address saved = addressDao.save(address);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(1L, saved.getUserId());
        assertEquals("张三", saved.getReceiverName());
    }

    @Test
    void shouldFindAddressesByUserId() {
        addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false));
        addressDao.save(createAddress(null, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false));
        addressDao.save(createAddress(null, 2L, "王五", "13700137000",
                "浙江省", "杭州市", "西湖区", "西湖路3号", false));

        List<Address> user1Addresses = addressDao.findByUserId(1L);
        assertEquals(2, user1Addresses.size());

        List<Address> user2Addresses = addressDao.findByUserId(2L);
        assertEquals(1, user2Addresses.size());
    }

    @Test
    void shouldFindAddressById() {
        Address saved = addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false));

        Optional<Address> found = addressDao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("张三", found.get().getReceiverName());
    }

    @Test
    void shouldReturnEmptyWhenAddressNotFound() {
        Optional<Address> found = addressDao.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindDefaultAddressByUserId() {
        addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true));
        addressDao.save(createAddress(null, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false));

        Optional<Address> defaultAddr = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddr.isPresent());
        assertEquals("张三", defaultAddr.get().getReceiverName());
    }

    @Test
    void shouldReturnEmptyDefaultWhenNoDefaultAddress() {
        addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false));

        Optional<Address> defaultAddr = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddr.isEmpty());
    }

    @Test
    void shouldUpdateAddress() {
        Address saved = addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false));
        saved.setReceiverName("赵六");
        saved.setCity("广州市");

        Optional<Address> updated = addressDao.update(saved);
        assertTrue(updated.isPresent());
        assertEquals("赵六", updated.get().getReceiverName());
        assertEquals("广州市", updated.get().getCity());
        assertNotNull(updated.get().getUpdatedAt());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentAddress() {
        Address ghost = createAddress(999L, 1L, "幽灵", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);

        Optional<Address> updated = addressDao.update(ghost);
        assertTrue(updated.isEmpty());
    }

    @Test
    void shouldDeleteAddress() {
        Address saved = addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false));

        addressDao.deleteById(saved.getId());
        assertTrue(addressDao.findById(saved.getId()).isEmpty());
    }

    @Test
    void shouldClearDefaultMappingWhenDeletingDefaultAddress() {
        Address defaultAddr = addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true));

        addressDao.deleteById(defaultAddr.getId());
        Optional<Address> foundDefault = addressDao.findDefaultByUserId(1L);
        assertTrue(foundDefault.isEmpty());
    }

    @Test
    void shouldEnforceDefaultAddressMutexOnSave() {
        addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true));
        Address secondDefault = addressDao.save(createAddress(null, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", true));

        Optional<Address> defaultAddr = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddr.isPresent());
        assertEquals(secondDefault.getId(), defaultAddr.get().getId());

        Optional<Address> oldDefault = addressDao.findById(1L);
        assertTrue(oldDefault.isPresent());
        assertFalse(oldDefault.get().getIsDefault());
    }

    @Test
    void shouldEnforceDefaultAddressMutexOnUpdate() {
        Address addr1 = addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true));
        Address addr2 = addressDao.save(createAddress(null, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false));

        addr2.setIsDefault(true);
        addressDao.update(addr2);

        Optional<Address> defaultAddr = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddr.isPresent());
        assertEquals(addr2.getId(), defaultAddr.get().getId());

        Optional<Address> oldDefault = addressDao.findById(addr1.getId());
        assertTrue(oldDefault.isPresent());
        assertFalse(oldDefault.get().getIsDefault());
    }

    @Test
    void shouldGenerateIncrementalIds() {
        Address addr1 = addressDao.save(createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false));
        Address addr2 = addressDao.save(createAddress(null, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false));

        assertEquals(1L, addr1.getId());
        assertEquals(2L, addr2.getId());
    }

    private Address createAddress(Long id, Long userId, String receiverName,
                                   String receiverPhone, String province, String city,
                                   String district, String detailAddress, Boolean isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName(receiverName);
        address.setReceiverPhone(receiverPhone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault);
        return address;
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test -pl . -Dtest=InMemoryAddressDaoTest -DfailIfNoTests=false 2>&1 | tail -5`
预期：编译失败 — `InMemoryAddressDao` 类不存在

- [ ] **步骤 3：创建 AddressDao 接口**

```java
package com.octopus.demo.userservice.dao;

import com.octopus.demo.userservice.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressDao {

    List<Address> findByUserId(Long userId);

    Optional<Address> findById(Long id);

    Optional<Address> findDefaultByUserId(Long userId);

    Address save(Address address);

    Optional<Address> update(Address address);

    void deleteById(Long id);
}
```

- [ ] **步骤 4：创建 InMemoryAddressDao 实现**

```java
package com.octopus.demo.userservice.dao.impl;

import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.model.Address;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryAddressDao implements AddressDao {

    private final ConcurrentHashMap<Long, Address> addressStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> defaultAddressMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Address> findByUserId(Long userId) {
        return addressStore.values().stream()
                .filter(addr -> addr.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findById(Long id) {
        return Optional.ofNullable(addressStore.get(id));
    }

    @Override
    public Optional<Address> findDefaultByUserId(Long userId) {
        Long defaultId = defaultAddressMap.get(userId);
        if (defaultId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(addressStore.get(defaultId));
    }

    @Override
    public Address save(Address address) {
        Long id = idGenerator.getAndIncrement();
        address.setId(id);
        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            Long oldDefaultId = defaultAddressMap.get(address.getUserId());
            if (oldDefaultId != null) {
                Address oldDefault = addressStore.get(oldDefaultId);
                if (oldDefault != null) {
                    oldDefault.setIsDefault(false);
                    oldDefault.setUpdatedAt(LocalDateTime.now());
                    addressStore.put(oldDefaultId, oldDefault);
                }
            }
            defaultAddressMap.put(address.getUserId(), id);
        }

        addressStore.put(id, address);
        return address;
    }

    @Override
    public Optional<Address> update(Address address) {
        if (!addressStore.containsKey(address.getId())) {
            return Optional.empty();
        }
        address.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            Long oldDefaultId = defaultAddressMap.get(address.getUserId());
            if (oldDefaultId != null && !oldDefaultId.equals(address.getId())) {
                Address oldDefault = addressStore.get(oldDefaultId);
                if (oldDefault != null) {
                    oldDefault.setIsDefault(false);
                    oldDefault.setUpdatedAt(LocalDateTime.now());
                    addressStore.put(oldDefaultId, oldDefault);
                }
            }
            defaultAddressMap.put(address.getUserId(), address.getId());
        } else if (Boolean.FALSE.equals(address.getIsDefault())) {
            Long currentDefaultId = defaultAddressMap.get(address.getUserId());
            if (currentDefaultId != null && currentDefaultId.equals(address.getId())) {
                defaultAddressMap.remove(address.getUserId());
            }
        }

        addressStore.put(address.getId(), address);
        return Optional.of(address);
    }

    @Override
    public void deleteById(Long id) {
        Address removed = addressStore.remove(id);
        if (removed != null && Boolean.TRUE.equals(removed.getIsDefault())) {
            defaultAddressMap.remove(removed.getUserId());
        }
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test -Dtest=InMemoryAddressDaoTest -DfailIfNoTests=false 2>&1 | tail -10`
预期：所有 12 个测试 PASS

- [ ] **步骤 6：Commit**

```bash
cd projects/xzf-octopus-demo-service-user
git add src/main/java/com/octopus/demo/userservice/dao/AddressDao.java \
        src/main/java/com/octopus/demo/userservice/dao/impl/InMemoryAddressDao.java \
        src/test/java/com/octopus/demo/userservice/dao/InMemoryAddressDaoTest.java
git commit -m "feat: add AddressDao interface and InMemoryAddressDao with default mutex logic"
```

---

## 任务 5：AddressService 接口与 AddressServiceImpl 实现（TDD）

**文件：**
- 创建：`src/main/java/com/octopus/demo/userservice/service/AddressService.java`
- 创建：`src/main/java/com/octopus/demo/userservice/service/impl/AddressServiceImpl.java`
- 创建：`src/test/java/com/octopus/demo/userservice/service/AddressServiceImplTest.java`

- [ ] **步骤 1：编写失败的测试 — AddressServiceImplTest.java**

```java
package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private AddressDao addressDao;

    private AddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressServiceImpl(userDao, addressDao);
    }

    @Test
    void shouldFindAddressesByUserId() {
        Address addr = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        when(addressDao.findByUserId(1L)).thenReturn(List.of(addr));

        List<Address> result = addressService.findByUserId(1L);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getReceiverName());
    }

    @Test
    void shouldFindAddressById() {
        Address addr = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(addr));

        Optional<Address> result = addressService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("张三", result.get().getReceiverName());
    }

    @Test
    void shouldCreateAddressWhenUserExists() {
        User user = new User(1L, "test", "test@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        Address newAddr = createAddress(null, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        Address saved = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        when(addressDao.save(any(Address.class))).thenReturn(saved);

        Address result = addressService.createAddress(1L, newAddr);
        assertNotNull(result);
        assertEquals("张三", result.getReceiverName());
        verify(addressDao).save(any(Address.class));
    }

    @Test
    void shouldThrowWhenCreatingAddressForNonExistentUser() {
        when(userDao.findById(999L)).thenReturn(Optional.empty());
        Address newAddr = createAddress(null, 999L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);

        assertThrows(RuntimeException.class, () ->
                addressService.createAddress(999L, newAddr));
        verify(addressDao, never()).save(any());
    }

    @Test
    void shouldCancelOldDefaultWhenCreatingDefaultAddress() {
        User user = new User(1L, "test", "test@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        Address oldDefault = createAddress(1L, 1L, "旧默认", "13800138000",
                "广东省", "深圳市", "南山区", "旧地址", true);
        when(addressDao.findDefaultByUserId(1L)).thenReturn(Optional.of(oldDefault));
        when(addressDao.update(oldDefault)).thenReturn(Optional.of(oldDefault));
        Address newAddr = createAddress(null, 1L, "新默认", "13900139000",
                "北京市", "北京市", "朝阳区", "新地址", true);
        Address savedNew = createAddress(2L, 1L, "新默认", "13900139000",
                "北京市", "北京市", "朝阳区", "新地址", true);
        when(addressDao.save(any(Address.class))).thenReturn(savedNew);

        Address result = addressService.createAddress(1L, newAddr);
        assertEquals("新默认", result.getReceiverName());
        verify(addressDao).update(oldDefault);
    }

    @Test
    void shouldUpdateAddressSuccessfully() {
        Address existing = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));
        Address updated = createAddress(1L, 1L, "赵六", "13800138000",
                "广东省", "广州市", "天河区", "天河路2号", false);
        when(addressDao.update(any(Address.class))).thenReturn(Optional.of(updated));

        Optional<Address> result = addressService.updateAddress(1L, 1L, updated);
        assertTrue(result.isPresent());
        assertEquals("赵六", result.get().getReceiverName());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentAddress() {
        when(addressDao.findById(999L)).thenReturn(Optional.empty());
        Address updateReq = createAddress(999L, 1L, "幽灵", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);

        Optional<Address> result = addressService.updateAddress(1L, 999L, updateReq);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenUpdatingAddressBelongsToDifferentUser() {
        Address otherUserAddr = createAddress(1L, 2L, "王五", "13700137000",
                "浙江省", "杭州市", "西湖区", "西湖路3号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(otherUserAddr));
        Address updateReq = createAddress(1L, 1L, "张三修改", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号修改", false);

        assertThrows(RuntimeException.class, () ->
                addressService.updateAddress(1L, 1L, updateReq));
        verify(addressDao, never()).update(any());
    }

    @Test
    void shouldCancelOldDefaultWhenUpdatingToDefault() {
        Address existing = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false);
        when(addressDao.findById(2L)).thenReturn(Optional.of(existing));
        Address oldDefault = createAddress(1L, 1L, "旧默认", "13800138000",
                "广东省", "深圳市", "南山区", "旧地址", true);
        when(addressDao.findDefaultByUserId(1L)).thenReturn(Optional.of(oldDefault));
        when(addressDao.update(oldDefault)).thenReturn(Optional.of(oldDefault));
        Address updatedAddr = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", true);
        when(addressDao.update(existing)).thenReturn(Optional.of(updatedAddr));

        existing.setIsDefault(true);
        Optional<Address> result = addressService.updateAddress(1L, 2L, existing);
        assertTrue(result.isPresent());
        verify(addressDao).update(oldDefault);
    }

    @Test
    void shouldDeleteAddressSuccessfully() {
        Address existing = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = addressService.deleteAddress(1L, 1L);
        assertTrue(result);
        verify(addressDao).deleteById(1L);
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentAddress() {
        when(addressDao.findById(999L)).thenReturn(Optional.empty());

        boolean result = addressService.deleteAddress(1L, 999L);
        assertFalse(result);
        verify(addressDao, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDeletingAddressBelongsToDifferentUser() {
        Address otherUserAddr = createAddress(1L, 2L, "王五", "13700137000",
                "浙江省", "杭州市", "西湖区", "西湖路3号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(otherUserAddr));

        assertThrows(RuntimeException.class, () ->
                addressService.deleteAddress(1L, 1L));
        verify(addressDao, never()).deleteById(any());
    }

    @Test
    void shouldAutoSetNextAsDefaultWhenDeletingDefaultAddress() {
        Address defaultAddr = createAddress(1L, 1L, "默认", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true);
        Address nextAddr = createAddress(2L, 1L, "下一个", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(defaultAddr));
        when(addressDao.findByUserId(1L)).thenReturn(List.of(nextAddr));
        when(addressDao.update(nextAddr)).thenReturn(Optional.of(nextAddr));

        boolean result = addressService.deleteAddress(1L, 1L);
        assertTrue(result);
        verify(addressDao).update(nextAddr);
    }

    @Test
    void shouldNotSetDefaultWhenNoOtherAddressExists() {
        Address defaultAddr = createAddress(1L, 1L, "默认", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true);
        when(addressDao.findById(1L)).thenReturn(Optional.of(defaultAddr));
        when(addressDao.findByUserId(1L)).thenReturn(List.of());

        boolean result = addressService.deleteAddress(1L, 1L);
        assertTrue(result);
        verify(addressDao, never()).update(any());
    }

    @Test
    void shouldSetDefaultAddressSuccessfully() {
        Address addr = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false);
        when(addressDao.findById(2L)).thenReturn(Optional.of(addr));
        when(addressDao.findDefaultByUserId(1L)).thenReturn(Optional.empty());
        Address updated = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", true);
        when(addressDao.update(addr)).thenReturn(Optional.of(updated));

        Optional<Address> result = addressService.setDefaultAddress(1L, 2L);
        assertTrue(result.isPresent());
        verify(addressDao).update(addr);
    }

    @Test
    void shouldCancelOldDefaultWhenSettingNewDefault() {
        Address newDefault = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", false);
        when(addressDao.findById(2L)).thenReturn(Optional.of(newDefault));
        Address oldDefault = createAddress(1L, 1L, "旧默认", "13800138000",
                "广东省", "深圳市", "南山区", "旧地址", true);
        when(addressDao.findDefaultByUserId(1L)).thenReturn(Optional.of(oldDefault));
        when(addressDao.update(oldDefault)).thenReturn(Optional.of(oldDefault));
        Address updatedNew = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", true);
        when(addressDao.update(newDefault)).thenReturn(Optional.of(updatedNew));

        Optional<Address> result = addressService.setDefaultAddress(1L, 2L);
        assertTrue(result.isPresent());
        verify(addressDao).update(oldDefault);
        verify(addressDao).update(newDefault);
    }

    @Test
    void shouldReturnEmptyWhenSettingDefaultForNonExistentAddress() {
        when(addressDao.findById(999L)).thenReturn(Optional.empty());

        Optional<Address> result = addressService.setDefaultAddress(1L, 999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenSettingDefaultForOtherUserAddress() {
        Address otherUserAddr = createAddress(1L, 2L, "王五", "13700137000",
                "浙江省", "杭州市", "西湖区", "西湖路3号", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(otherUserAddr));

        assertThrows(RuntimeException.class, () ->
                addressService.setDefaultAddress(1L, 1L));
    }

    private Address createAddress(Long id, Long userId, String receiverName,
                                   String receiverPhone, String province, String city,
                                   String district, String detailAddress, Boolean isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName(receiverName);
        address.setReceiverPhone(receiverPhone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault);
        return address;
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test -Dtest=AddressServiceImplTest -DfailIfNoTests=false 2>&1 | tail -5`
预期：编译失败 — `AddressServiceImpl` 类不存在

- [ ] **步骤 3：创建 AddressService 接口**

```java
package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressService {

    List<Address> findByUserId(Long userId);

    Optional<Address> findById(Long id);

    Address createAddress(Long userId, Address address);

    Optional<Address> updateAddress(Long userId, Long id, Address address);

    boolean deleteAddress(Long userId, Long id);

    Optional<Address> setDefaultAddress(Long userId, Long id);
}
```

- [ ] **步骤 4：创建 AddressServiceImpl 实现**

```java
package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.service.AddressService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AddressServiceImpl implements AddressService {

    private final UserDao userDao;
    private final AddressDao addressDao;

    public AddressServiceImpl(UserDao userDao, AddressDao addressDao) {
        this.userDao = userDao;
        this.addressDao = addressDao;
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        return addressDao.findByUserId(userId);
    }

    @Override
    public Optional<Address> findById(Long id) {
        return addressDao.findById(id);
    }

    @Override
    public Address createAddress(Long userId, Address address) {
        userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在, id: " + userId));

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            cancelOldDefault(userId);
        }

        address.setUserId(userId);
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        return addressDao.save(address);
    }

    @Override
    public Optional<Address> updateAddress(Long userId, Long id, Address address) {
        Optional<Address> existing = addressDao.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Address current = existing.get();
        if (!current.getUserId().equals(userId)) {
            throw new RuntimeException("地址不属于该用户");
        }

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            cancelOldDefault(userId);
        }

        address.setId(id);
        address.setUserId(userId);
        return addressDao.update(address);
    }

    @Override
    public boolean deleteAddress(Long userId, Long id) {
        Optional<Address> existing = addressDao.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        Address current = existing.get();
        if (!current.getUserId().equals(userId)) {
            throw new RuntimeException("地址不属于该用户");
        }

        boolean wasDefault = Boolean.TRUE.equals(current.getIsDefault());
        addressDao.deleteById(id);

        if (wasDefault) {
            autoSetNextDefault(userId);
        }

        return true;
    }

    @Override
    public Optional<Address> setDefaultAddress(Long userId, Long id) {
        Optional<Address> existing = addressDao.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Address current = existing.get();
        if (!current.getUserId().equals(userId)) {
            throw new RuntimeException("地址不属于该用户");
        }

        cancelOldDefault(userId);

        current.setIsDefault(true);
        return addressDao.update(current);
    }

    private void cancelOldDefault(Long userId) {
        addressDao.findDefaultByUserId(userId).ifPresent(oldDefault -> {
            oldDefault.setIsDefault(false);
            addressDao.update(oldDefault);
        });
    }

    private void autoSetNextDefault(Long userId) {
        List<Address> remaining = addressDao.findByUserId(userId);
        if (!remaining.isEmpty()) {
            Address next = remaining.stream()
                    .min(Comparator.comparingLong(Address::getId))
                    .orElse(remaining.get(0));
            next.setIsDefault(true);
            addressDao.update(next);
        }
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test -Dtest=AddressServiceImplTest -DfailIfNoTests=false 2>&1 | tail -10`
预期：所有 16 个测试 PASS

- [ ] **步骤 6：Commit**

```bash
cd projects/xzf-octopus-demo-service-user
git add src/main/java/com/octopus/demo/userservice/service/AddressService.java \
        src/main/java/com/octopus/demo/userservice/service/impl/AddressServiceImpl.java \
        src/test/java/com/octopus/demo/userservice/service/AddressServiceImplTest.java
git commit -m "feat: add AddressService interface and AddressServiceImpl with business logic"
```

---

## 任务 6：AddressController（TDD）

**文件：**
- 创建：`src/main/java/com/octopus/demo/userservice/controller/AddressController.java`
- 创建：`src/test/java/com/octopus/demo/userservice/controller/AddressControllerTest.java`

- [ ] **步骤 1：编写失败的测试 — AddressControllerTest.java**

```java
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

    @Test
    void shouldGetAllAddressesForUser() throws Exception {
        Address addr = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true);
        when(addressService.findByUserId(1L)).thenReturn(List.of(addr));

        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiverName").value("张三"))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void shouldGetSingleAddress() throws Exception {
        Address addr = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", false);
        when(addressService.findById(1L)).thenReturn(Optional.of(addr));

        mockMvc.perform(get("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverName").value("张三"));
    }

    @Test
    void shouldReturn404WhenAddressNotFound() throws Exception {
        when(addressService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/1/addresses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldCreateAddress() throws Exception {
        Address saved = createAddress(1L, 1L, "张三", "13800138000",
                "广东省", "深圳市", "南山区", "科技园路1号", true);
        when(addressService.createAddress(eq(1L), any(Address.class))).thenReturn(saved);

        String body = """
                {
                    "receiverName": "张三",
                    "receiverPhone": "13800138000",
                    "province": "广东省",
                    "city": "深圳市",
                    "district": "南山区",
                    "detailAddress": "科技园路1号",
                    "isDefault": true
                }""";

        mockMvc.perform(post("/api/users/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiverName").value("张三"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void shouldReturn404WhenCreatingForNonExistentUser() throws Exception {
        when(addressService.createAddress(eq(999L), any(Address.class)))
                .thenThrow(new RuntimeException("用户不存在, id: 999"));

        String body = """
                {
                    "receiverName": "张三",
                    "receiverPhone": "13800138000",
                    "province": "广东省",
                    "city": "深圳市",
                    "district": "南山区",
                    "detailAddress": "科技园路1号"
                }""";

        mockMvc.perform(post("/api/users/999/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
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

    @Test
    void shouldUpdateAddress() throws Exception {
        Address updated = createAddress(1L, 1L, "赵六", "13800138000",
                "广东省", "广州市", "天河区", "天河路2号", false);
        when(addressService.updateAddress(eq(1L), eq(1L), any(Address.class)))
                .thenReturn(Optional.of(updated));

        String body = """
                {
                    "receiverName": "赵六",
                    "city": "广州市",
                    "district": "天河区",
                    "detailAddress": "天河路2号"
                }""";

        mockMvc.perform(put("/api/users/1/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverName").value("赵六"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAddress() throws Exception {
        when(addressService.updateAddress(eq(1L), eq(999L), any(Address.class)))
                .thenReturn(Optional.empty());

        String body = """
                {
                    "receiverName": "幽灵"
                }""";

        mockMvc.perform(put("/api/users/1/addresses/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn403WhenUpdatingOtherUserAddress() throws Exception {
        when(addressService.updateAddress(eq(1L), eq(1L), any(Address.class)))
                .thenThrow(new RuntimeException("地址不属于该用户"));

        String body = """
                {
                    "receiverName": "篡改"
                }""";

        mockMvc.perform(put("/api/users/1/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("地址不属于该用户"));
    }

    @Test
    void shouldDeleteAddress() throws Exception {
        when(addressService.deleteAddress(1L, 1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1/addresses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentAddress() throws Exception {
        when(addressService.deleteAddress(1L, 999L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/1/addresses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn403WhenDeletingOtherUserAddress() throws Exception {
        when(addressService.deleteAddress(1L, 1L))
                .thenThrow(new RuntimeException("地址不属于该用户"));

        mockMvc.perform(delete("/api/users/1/addresses/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("地址不属于该用户"));
    }

    @Test
    void shouldSetDefaultAddress() throws Exception {
        Address setDefault = createAddress(2L, 1L, "李四", "13900139000",
                "北京市", "北京市", "朝阳区", "建国路2号", true);
        when(addressService.setDefaultAddress(1L, 2L)).thenReturn(Optional.of(setDefault));

        mockMvc.perform(put("/api/users/1/addresses/2/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void shouldReturn404WhenSettingDefaultForNonExistentAddress() throws Exception {
        when(addressService.setDefaultAddress(1L, 999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/1/addresses/999/default"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn403WhenSettingDefaultForOtherUserAddress() throws Exception {
        when(addressService.setDefaultAddress(1L, 1L))
                .thenThrow(new RuntimeException("地址不属于该用户"));

        mockMvc.perform(put("/api/users/1/addresses/1/default"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("地址不属于该用户"));
    }

    private Address createAddress(Long id, Long userId, String receiverName,
                                   String receiverPhone, String province, String city,
                                   String district, String detailAddress, Boolean isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName(receiverName);
        address.setReceiverPhone(receiverPhone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        return address;
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test -Dtest=AddressControllerTest -DfailIfNoTests=false 2>&1 | tail -5`
预期：编译失败 — `AddressController` 类不存在

- [ ] **步骤 3：创建 AddressController.java**

```java
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

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<Address>> listAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.findByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddress(@PathVariable Long userId, @PathVariable Long id) {
        return addressService.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "地址不存在, id: " + id)));
    }

    @PostMapping
    public ResponseEntity<?> createAddress(@PathVariable Long userId,
                                            @Valid @RequestBody CreateAddressRequest request) {
        try {
            Address address = new Address();
            address.setReceiverName(request.getReceiverName());
            address.setReceiverPhone(request.getReceiverPhone());
            address.setProvince(request.getProvince());
            address.setCity(request.getCity());
            address.setDistrict(request.getDistrict());
            address.setDetailAddress(request.getDetailAddress());
            address.setPostalCode(request.getPostalCode());
            address.setIsDefault(request.getIsDefault());
            Address saved = addressService.createAddress(userId, address);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("用户不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable Long userId, @PathVariable Long id,
                                            @Valid @RequestBody UpdateAddressRequest request) {
        try {
            Address address = new Address();
            address.setReceiverName(request.getReceiverName());
            address.setReceiverPhone(request.getReceiverPhone());
            address.setProvince(request.getProvince());
            address.setCity(request.getCity());
            address.setDistrict(request.getDistrict());
            address.setDetailAddress(request.getDetailAddress());
            address.setPostalCode(request.getPostalCode());
            address.setIsDefault(request.getIsDefault());

            return addressService.updateAddress(userId, id, address)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "地址不存在, id: " + id)));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("地址不属于该用户")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long userId, @PathVariable Long id) {
        try {
            boolean deleted = addressService.deleteAddress(userId, id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "地址不存在, id: " + id));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("地址不属于该用户")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable Long userId, @PathVariable Long id) {
        try {
            return addressService.setDefaultAddress(userId, id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "地址不存在, id: " + id)));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("地址不属于该用户")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test -Dtest=AddressControllerTest -DfailIfNoTests=false 2>&1 | tail -10`
预期：所有 15 个测试 PASS

- [ ] **步骤 5：Commit**

```bash
cd projects/xzf-octopus-demo-service-user
git add src/main/java/com/octopus/demo/userservice/controller/AddressController.java \
        src/test/java/com/octopus/demo/userservice/controller/AddressControllerTest.java
git commit -m "feat: add AddressController with CRUD and set-default endpoints"
```

---

## 任务 7：全量测试验证与最终检查

**文件：**
- 无新文件

- [ ] **步骤 1：运行全量测试**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw test 2>&1 | tail -15`
预期：所有测试 PASS（UserControllerTest + UserServiceImplTest + InMemoryUserDaoTest + InMemoryAddressDaoTest + AddressServiceImplTest + AddressControllerTest）

- [ ] **步骤 2：检查编译无错误**

运行：`cd projects/xzf-octopus-demo-service-user && ./mvnw compile -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：确认所有文件都已提交**

运行：`cd projects/xzf-octopus-demo-service-user && git status`
预期：nothing to commit, working tree clean

- [ ] **步骤 4：确认最终提交日志**

运行：`cd projects/xzf-octopus-demo-service-user && git log --oneline -6`
预期：6 个新增 commit（Address model, CreateAddressRequest DTO, UpdateAddressRequest DTO, AddressDao, AddressService, AddressController）