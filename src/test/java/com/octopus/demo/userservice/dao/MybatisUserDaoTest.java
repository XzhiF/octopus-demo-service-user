package com.octopus.demo.userservice.dao;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dao.impl.MybatisUserDao;
import com.octopus.demo.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MybatisUserDaoTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("octopus_demo")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    @Autowired
    private MybatisUserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void saveAssignsGeneratedIdAndTimestamps() {
        User saved = userDao.save(newUser("mysql_phase1_save", "保存用户", "save@example.com", 28));

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
        assertEquals("mysql_phase1_save", saved.getUsername());
        assertEquals("保存用户", saved.getNickname());
        assertEquals("save@example.com", saved.getEmail());
        assertEquals(28, saved.getAge());
        assertEquals("hash-value", saved.getPasswordHash());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void findByIdReturnsUserWhenExistsAndEmptyWhenMissing() {
        User saved = userDao.save(newUser("mysql_phase1_find_id", "查找", "findid@example.com", 30));

        Optional<User> found = userDao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("mysql_phase1_find_id", found.get().getUsername());

        assertTrue(userDao.findById(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void findByUsernameReturnsUserWhenExistsAndEmptyWhenMissing() {
        User saved = userDao.save(newUser("mysql_phase1_find_name", "名字", "findname@example.com", 31));

        Optional<User> found = userDao.findByUsername("mysql_phase1_find_name");
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());

        assertTrue(userDao.findByUsername("mysql_phase1_no_such_user").isEmpty());
    }

    @Test
    void findAllReturnsAllUsersOrderedByIdAscending() {
        User first = userDao.save(newUser("mysql_phase1_all_1", "一", "all1@example.com", 20));
        User second = userDao.save(newUser("mysql_phase1_all_2", "二", "all2@example.com", 21));
        User third = userDao.save(newUser("mysql_phase1_all_3", "三", "all3@example.com", 22));

        List<User> users = userDao.findAll();

        assertEquals(3, users.size());
        assertEquals(List.of(first.getId(), second.getId(), third.getId()),
                users.stream().map(User::getId).toList());
    }

    @Test
    void paginationReturnsFirstPage() {
        saveUsers(5);

        PageResultBean<User> result = userDao.findAll(pageQuery(1, 2));

        assertEquals(5, result.getCount());
        assertEquals(2, result.getList().size());
        assertTrue(result.getList().get(0).getId() < result.getList().get(1).getId());
    }

    @Test
    void paginationReturnsLastPartialPage() {
        saveUsers(5);

        PageResultBean<User> result = userDao.findAll(pageQuery(3, 2));

        assertEquals(5, result.getCount());
        assertEquals(1, result.getList().size());
    }

    @Test
    void paginationBeyondRangeReturnsEmptyListKeepingCount() {
        saveUsers(5);

        PageResultBean<User> result = userDao.findAll(pageQuery(4, 2));

        assertEquals(5, result.getCount());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void updateChangesFieldsAndRefreshesUpdatedAt() {
        User saved = userDao.save(newUser("mysql_phase1_update", "旧名", "old@example.com", 40));
        jdbcTemplate.update("UPDATE users SET updated_at = '2000-01-01 00:00:00' WHERE id = ?", saved.getId());

        saved.setNickname("新名");
        saved.setEmail("new@example.com");
        saved.setAge(41);
        Optional<User> updated = userDao.update(saved);

        assertTrue(updated.isPresent());
        assertEquals("新名", updated.get().getNickname());
        assertEquals("new@example.com", updated.get().getEmail());
        assertEquals(41, updated.get().getAge());
        assertTrue(updated.get().getUpdatedAt().isAfter(LocalDateTime.of(2000, 1, 1, 0, 0, 0)));
    }

    @Test
    void updateReturnsEmptyWhenUserDoesNotExist() {
        User missing = newUser("mysql_phase1_update_missing", "不存在", "missing@example.com", 50);
        missing.setId(Long.MAX_VALUE);

        assertTrue(userDao.update(missing).isEmpty());
    }

    @Test
    void deleteByIdReturnsTrueWhenDeletedAndFalseOtherwise() {
        User saved = userDao.save(newUser("mysql_phase1_delete", "删除", "delete@example.com", 60));

        assertTrue(userDao.deleteById(saved.getId()));
        assertTrue(userDao.findById(saved.getId()).isEmpty());
        assertFalse(userDao.deleteById(saved.getId()));
    }

    @Test
    void saveDuplicateUsernameThrowsIllegalArgumentException() {
        userDao.save(newUser("mysql_phase1_duplicate", "第一个", "dup1@example.com", 70));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userDao.save(newUser("mysql_phase1_duplicate", "第二个", "dup2@example.com", 71)));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    void updateDuplicateUsernameThrowsIllegalArgumentException() {
        userDao.save(newUser("mysql_phase1_update_dup_owner", "甲", "owner@example.com", 80));
        User other = userDao.save(newUser("mysql_phase1_update_dup_other", "乙", "other@example.com", 81));

        other.setUsername("mysql_phase1_update_dup_owner");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userDao.update(other));
        assertEquals("用户名已存在", ex.getMessage());
    }

    private void saveUsers(int count) {
        for (int i = 1; i <= count; i++) {
            userDao.save(newUser("mysql_phase1_page_" + i, "分页" + i, "page" + i + "@example.com", 20 + i));
        }
    }

    private static PageQueryBean pageQuery(int page, int size) {
        PageQueryBean query = new PageQueryBean();
        query.setPage(page);
        query.setSize(size);
        return query;
    }

    private static User newUser(String username, String nickname, String email, int age) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setAge(age);
        user.setPasswordHash("hash-value");
        return user;
    }
}