package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.common.audit.AuditContext;
import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final AuditLogger auditLogger;

    public UserServiceImpl(UserDao userDao, AuditLogger auditLogger) {
        this.userDao = userDao;
        this.auditLogger = auditLogger;
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public PageResultBean<User> getAllUsers(PageQueryBean query) {
        return userDao.findAll(query);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userDao.findById(id);
    }

    @Override
    public User createUser(User user) {
        User created = userDao.save(user);
        safeAudit(() -> auditLogger.log(AuditEvent.of(
            AuditContext.getCurrentUserId(),
            "CREATE", "USER", String.valueOf(created.getId()))));
        return created;
    }

    @Override
    public Optional<User> updateUser(Long id, User user) {
        Optional<User> updated = userDao.findById(id)
            .flatMap(existing -> {
                user.setId(id);
                return userDao.update(user);
            });
        updated.ifPresent(u -> safeAudit(() -> auditLogger.log(AuditEvent.of(
            AuditContext.getCurrentUserId(),
            "UPDATE", "USER", String.valueOf(id)))));
        return updated;
    }

    @Override
    public boolean deleteUser(Long id) {
        boolean deleted = userDao.deleteById(id);
        if (deleted) {
            safeAudit(() -> auditLogger.log(AuditEvent.of(
                AuditContext.getCurrentUserId(),
                "DELETE", "USER", String.valueOf(id))));
        }
        return deleted;
    }

    private void safeAudit(Runnable auditAction) {
        try {
            auditAction.run();
        } catch (Exception e) {
            log.warn("审计日志记录失败，不影响业务操作: {}", e.getMessage(), e);
        }
    }
}
