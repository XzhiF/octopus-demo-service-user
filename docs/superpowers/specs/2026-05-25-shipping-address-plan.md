# 收货地址功能实现计划

## 阶段 1：模型与 DTO（TDD 先写测试）

### 1.1 Address 模型
- 创建 `model/Address.java` POJO
- 字段：id, userId, receiverName, receiverPhone, province, city, district, detailAddress, postalCode, isDefault, createdAt, updatedAt
- 全参数构造器 + 无参构造器 + getter/setter

### 1.2 DTO 类
- 创建 `dto/CreateAddressRequest.java`（receiverName @NotBlank, receiverPhone @NotBlank + 手机号正则, province/city/district/detailAddress @NotBlank, postalCode 可选, isDefault 可选）
- 创建 `dto/UpdateAddressRequest.java`（所有字段可选）

## 阶段 2：DAO 层（TDD）

### 2.1 AddressDao 接口
- 创建 `dao/AddressDao.java`（findByUserId, findById, findDefaultByUserId, save, update, deleteById）

### 2.2 InMemoryAddressDaoTest
- 先写测试（RED）：保存、查询、更新、删除、默认地址互斥

### 2.3 InMemoryAddressDao 实现
- `dao/impl/InMemoryAddressDao.java`
- ConcurrentHashMap<Long, Address> addressStore
- ConcurrentHashMap<Long, Long> defaultAddressMap（userId → defaultAddressId）
- AtomicLong idGenerator
- 实现所有接口方法，运行测试通过（GREEN）

## 阶段 3：Service 层（TDD）

### 3.1 AddressService 接口
- 创建 `service/AddressService.java`（findByUserId, findById, createAddress, updateAddress, deleteAddress, setDefaultAddress）

### 3.2 AddressServiceImplTest
- 先写测试（RED）：创建（含用户不存在）、更新（含归属校验）、删除（含默认地址自动切换）、设为默认

### 3.3 AddressServiceImpl 实现
- `service/impl/AddressServiceImpl.java`
- 构造器注入 UserDao + AddressDao
- 实现所有业务逻辑，运行测试通过（GREEN）

## 阶段 4：Controller 层（TDD）

### 4.1 AddressControllerTest
- 先写测试（RED）：所有 API 端点的 MockMvc 测试

### 4.2 AddressController 实现
- `controller/AddressController.java`
- @RestController, 基路径 `/api/users/{userId}/addresses`
- 实现所有端点，运行测试通过（GREEN）

## 阶段 5：验证与清理

- 运行全量测试确认无回归
- 确认代码覆盖率 >= 80%
- 代码审查（code-reviewer 代理）