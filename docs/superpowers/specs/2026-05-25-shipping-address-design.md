# 收货地址功能设计规格

## 核心功能目标

为用户服务新增收货地址管理功能，支持一个用户拥有多个收货地址，并支持默认地址设置与互斥逻辑。

## 技术方案概述

采用嵌套资源模式（方案 A），地址作为用户的子资源，API 路径嵌套在 `/api/users/{userId}/addresses` 下。沿用现有分层架构（Controller → Service → DAO → Model），使用 ConcurrentHashMap 内存存储，保持与项目现有模式完全一致。默认地址采用互斥设计——每个用户最多一个默认地址，设置新默认时自动取消旧的。

## 关键技术选型

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 存储方式 | ConcurrentHashMap 内存存储 | 与现有 InMemoryUserDao 保持一致，demo 项目无需真实数据库 |
| API 风格 | RESTful 嵌套资源 | `/api/users/{userId}/addresses`，资源归属清晰 |
| 架构分层 | 新增独立 DAO/Service/Controller | 与 User 模块同级，职责清晰 |
| 验证框架 | Jakarta Bean Validation | 与现有 DTO 验证方式一致 |
| 测试方式 | 三层覆盖（DAO/Service/Controller） | 与现有测试模式一致 |
| ID 生成 | AtomicLong | 与现有 UserDao 一致 |

## 主要约束和边界

1. 地址必须关联已有用户（创建时验证 userId 存在）
2. 每个用户最多一个默认地址（互斥逻辑）
3. 删除默认地址时自动将下一个地址设为默认（如存在）
4. 地址操作（更新/删除/设为默认）需验证地址归属该用户
5. 中国标准地址格式：省/市/区/详细地址
6. 不引入真实数据库，保持内存存储的 demo 特性

## 数据模型

### Address POJO

```
Address {
  Long id;                  // 自增 ID（AtomicLong）
  Long userId;              // 关联用户 ID
  String receiverName;      // 收件人姓名
  String receiverPhone;     // 收件人手机号
  String province;          // 省
  String city;              // 市
  String district;          // 区/县
  String detailAddress;     // 详细地址
  String postalCode;        // 邵政编码（可选，保留原格式一致性）
  Boolean isDefault;        // 是否默认地址
  LocalDateTime createdAt;  // 创建时间
  LocalDateTime updatedAt;  // 更新时间
}
```

## API 设计

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|---------|------|
| GET | `/api/users/{userId}/addresses` | 获取用户所有地址 | 无 | 200 + List<Address> |
| GET | `/api/users/{userId}/addresses/{id}` | 获取单个地址 | 无 | 200 + Address / 404 |
| POST | `/api/users/{userId}/addresses` | 新增地址 | CreateAddressRequest JSON | 201 + Address |
| PUT | `/api/users/{userId}/addresses/{id}` | 更新地址 | UpdateAddressRequest JSON | 200 + Address / 404 |
| DELETE | `/api/users/{userId}/addresses/{id}` | 删除地址 | 无 | 204 / 404 |
| PUT | `/api/users/{userId}/addresses/{id}/default` | 设为默认地址 | 无 | 200 + Address / 404 |

### 错误响应

沿用 `Map.of("error", message)` 格式，中文错误消息：
- 用户不存在 → 404 + `{error: "用户不存在, id: xxx"}`
- 地址不存在 → 404 + `{error: "地址不存在, id: xxx"}`
- 地址不属于该用户 → 403 + `{error: "地址不属于该用户"}`

## 项目结构（新增文件）

```
src/main/java/com/octopus/demo/userservice/
  model/
    Address.java
  dao/
    AddressDao.java
    impl/
      InMemoryAddressDao.java
  dto/
    CreateAddressRequest.java
    UpdateAddressRequest.java
  service/
    AddressService.java
    impl/
      AddressServiceImpl.java
  controller/
    AddressController.java

src/test/java/com/octopus/demo/userservice/
  dao/
    InMemoryAddressDaoTest.java
  service/
    AddressServiceImplTest.java
  controller/
    AddressControllerTest.java
```

## DAO 层设计

### AddressDao 接口

```java
public interface AddressDao {
    List<Address> findByUserId(Long userId);
    Optional<Address> findById(Long id);
    Optional<Address> findDefaultByUserId(Long userId);
    Address save(Address address);
    Address update(Address address);
    void deleteById(Long id);
}
```

### InMemoryAddressDao 实现

内部存储结构：
- `ConcurrentHashMap<Long, Address>` addressStore — 以 addressId 为 key
- `ConcurrentHashMap<Long, Long>` defaultAddressMap — userId → defaultAddressId 映射
- `AtomicLong` idGenerator — ID 自增生成器

`findDefaultByUserId` 通过 defaultAddressMap 查找，再从 addressStore 取出地址。
`save` 时如 isDefault=true，更新 defaultAddressMap。
`update` 时如 isDefault 变为 true，取消旧默认并更新映射。
`deleteById` 时如删除的是默认地址，清除映射。

## DTO 验证规则

### CreateAddressRequest

| 字段 | 验证规则 |
|------|---------|
| receiverName | @NotBlank |
| receiverPhone | @NotBlank + 正则（中国手机号格式 `^1[3-9]\d{9}$`） |
| province | @NotBlank |
| city | @NotBlank |
| district | @NotBlank |
| detailAddress | @NotBlank |
| postalCode | 可选 |
| isDefault | 可选，默认 false |

### UpdateAddressRequest

所有字段可选，提供则更新。验证规则同 CreateAddressRequest（仅在字段非空时触发）。

## Service 层关键逻辑

AddressServiceImpl 通过构造器注入 UserDao 和 AddressDao（与现有 UserServiceImpl 的注入方式一致）。

### 创建地址

1. 调用 UserDao.findById(userId) 验证用户存在
2. 如 isDefault=true 且用户已有默认地址，先取消旧默认地址
3. 调用 AddressDao.save(address) 保存
4. 返回保存后的 Address

### 更新地址

1. 调用 AddressDao.findById(id) 验证地址存在
2. 验证地址的 userId 匹配请求的 userId
3. 如 isDefault 变为 true，先取消旧默认地址
4. 调用 AddressDao.update(address) 更新
5. 返回更新后的 Address

### 删除地址

1. 调用 AddressDao.findById(id) 验证地址存在
2. 验证地址的 userId 匹配请求的 userId
3. 如删除的是默认地址，找到用户下一个地址（按 ID 最小的那个）设为默认
4. 调用 AddressDao.deleteById(id) 删除

### 设为默认地址

1. 验证地址存在且属于该用户
2. 取消旧默认地址的 isDefault
3. 设置新地址的 isDefault=true
4. 更新两个地址

## 测试覆盖

### InMemoryAddressDaoTest

- 保存地址并自增 ID
- 查询用户所有地址
- 查询默认地址
- 更新地址
- 删除地址
- 默认地址互斥逻辑（保存时、更新时）

### AddressServiceImplTest

- 创建地址成功（用户存在）
- 创建地址失败（用户不存在）
- 创建默认地址时自动取消旧默认
- 更新地址成功
- 更新地址失败（地址不存在/不属于该用户）
- 更新默认地址时自动取消旧默认
- 删除地址成功
- 删除默认地址后自动设置下一个为默认
- 设为默认地址成功

### AddressControllerTest

- GET 获取地址列表
- GET 获取单个地址
- POST 创建地址（成功/失败）
- PUT 更新地址（成功/失败）
- DELETE 删除地址（成功/失败）
- PUT 设为默认地址（成功/失败）
- 验证字段校验（空值、手机号格式等）