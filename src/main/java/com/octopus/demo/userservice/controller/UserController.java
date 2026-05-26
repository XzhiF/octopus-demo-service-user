package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dto.CreateUserRequest;
import com.octopus.demo.userservice.dto.UpdateUserRequest;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import com.octopus.demo.userservice.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public R<PageResultBean<UserVO>> getAllUsers(PageQueryBean query) {
        PageResultBean<User> result = userService.getAllUsers(query);
        PageResultBean<UserVO> voResult = new PageResultBean<>();
        voResult.setCount(result.getCount());
        voResult.setList(result.getList().stream().map(UserVO::from).collect(Collectors.toList()));
        return R.ok(voResult);
    }

    @GetMapping("/{id}")
    public R<UserVO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "用户不存在, id: " + id));
    }

    @PostMapping
    public R<UserVO> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        User created = userService.createUser(user);
        return R.ok(UserVO.from(created));
    }

    @PutMapping("/{id}")
    public R<UserVO> updateUser(@PathVariable Long id,
                                 @Valid @RequestBody UpdateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        return userService.updateUser(id, user)
                .map(UserVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "用户不存在, id: " + id));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) return R.fail(404, "用户不存在, id: " + id);
        return R.ok();
    }
}
