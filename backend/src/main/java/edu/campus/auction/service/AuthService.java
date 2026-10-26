package edu.campus.auction.service;

import edu.campus.auction.data.*;
import edu.campus.auction.web.ApiModels.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final SessionRepository sessions;
    private final Clock clock;
    private final long sessionHours;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, SessionRepository sessions, Clock clock,
            @Value("${auction.session-hours}") long sessionHours) {
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
        this.sessionHours = sessionHours;
    }

    @Transactional
    public UserView register(Credentials credentials) {
        if (users.findByUsername(credentials.username()).isPresent()) {
            throw new ApiException(409, "USERNAME_EXISTS", "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.username = credentials.username();
        user.passwordHash = passwords.encode(credentials.password());
        return UserView.of(users.saveAndFlush(user));
    }

    @Transactional
    public LoginView login(Credentials credentials) {
        UserAccount user = users.findByUsername(credentials.username()).orElse(null);
        if (user == null || !passwords.matches(credentials.password(), user.passwordHash)) {
            throw new ApiException(401, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UserSession session = new UserSession();
        session.tokenHash = hash(token);
        session.userId = user.id;
        session.expiresAt = clock.instant().plus(Duration.ofHours(sessionHours));
        sessions.save(session);
        return new LoginView(token, "Bearer", session.expiresAt, UserView.of(user));
    }

    @Transactional(readOnly = true)
    public Long requireUser(String authorization) {
        String token = extractToken(authorization);
        UserSession session = sessions.findById(hash(token)).orElseThrow(AuthService::loginRequired);
        if (!clock.instant().isBefore(session.expiresAt)) { throw loginRequired(); }
        return session.userId;
    }

    @Transactional(readOnly = true)
    public UserView me(String authorization) {
        Long id = requireUser(authorization);
        return UserView.of(users.findById(id).orElseThrow(AuthService::loginRequired));
    }

    @Transactional
    public void logout(String authorization) {
        // 重复退出也成功；令牌一旦删除就不能再访问需要登录的接口。
        sessions.deleteById(hash(extractToken(authorization)));
    }

    private static String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() > 200) {
            throw loginRequired();
        }
        String token = header.substring(7);
        if (!token.matches("[A-Za-z0-9_-]{43}")) { throw loginRequired(); }
        return token;
    }

    private static ApiException loginRequired() {
        return new ApiException(401, "LOGIN_REQUIRED", "请登录，或重新登录获取有效令牌");
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
