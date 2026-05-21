package com.octopus.demo.userservice.dao.impl;

import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserDao implements UserDao {

    private final ConcurrentHashMap<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public User save(User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        store.put(id, user);
        return user;
    }

    @Override
    public Optional<User> update(User user) {
        if (!store.containsKey(user.getId())) {
            return Optional.empty();
        }
        user.setUpdatedAt(LocalDateTime.now());
        store.put(user.getId(), user);
        return Optional.of(user);
    }

    @Override
    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }
}