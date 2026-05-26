package com.lencode.paper.auth.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.dto.AuthTokenPayload;
import com.lencode.paper.auth.dto.IssuedToken;
import com.lencode.paper.auth.dto.LoginRequest;
import com.lencode.paper.auth.dto.RegisterRequest;
import com.lencode.paper.auth.entity.UserAccount;
import com.lencode.paper.auth.mapper.UserMapper;
import com.lencode.paper.auth.vo.LoginResponse;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.DuplicateUsernameException;
import com.lencode.paper.common.exception.UnauthorizedException;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public AuthService(
            UserMapper userMapper,
            BCryptPasswordEncoder passwordEncoder,
            AuthTokenService authTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("请求参数不能为空");
        }

        String username = normalizeUsername(request.getUsername());
        validate(username, request.getPassword());

        if (existsByUsername(username)) {
            throw new DuplicateUsernameException("账号已存在");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        try {
            UserAccount user = new UserAccount();
            user.setUsername(username);
            user.setPasswordHash(passwordHash);
            user.setRole(DEFAULT_ROLE);
            user.setStatus(DEFAULT_STATUS);
            userMapper.insert(user);
            return UserResponse.from(user);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateUsernameException("账号已存在");
        }
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("请求参数不能为空");
        }

        String username = normalizeUsername(request.getUsername());
        validate(username, request.getPassword());

        UserAccount user = findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("账号或密码错误"));
        ensureActive(user);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("账号或密码错误");
        }

        IssuedToken issuedToken = authTokenService.issueToken(user);
        return new LoginResponse(
                issuedToken.getToken(),
                AuthTokenService.TOKEN_TTL_SECONDS,
                UserResponse.from(user)
        );
    }

    public UserResponse currentUser() {
        return currentAuthenticatedUser().toUserResponse();
    }

    public AuthenticatedUser authenticateToken(String token) {
        AuthTokenPayload payload = authTokenService.resolve(token);
        UserAccount user = userMapper.selectById(payload.getUserId());
        if (user == null) {
            throw new UnauthorizedException("登录已失效");
        }
        ensureActive(user);
        return AuthenticatedUser.from(user);
    }

    private boolean existsByUsername(String username) {
        Long count = userMapper.selectCount(new QueryWrapper<UserAccount>().eq("username", username));
        return count != null && count > 0;
    }

    private java.util.Optional<UserAccount> findByUsername(String username) {
        return java.util.Optional.ofNullable(userMapper.selectOne(
                new QueryWrapper<UserAccount>().eq("username", username).last("LIMIT 1")
        ));
    }

    private static AuthenticatedUser currentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            throw new UnauthorizedException("未登录");
        }
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    private static void ensureActive(UserAccount user) {
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw new UnauthorizedException("账号不可用");
        }
    }

    private static void validate(String username, String password) {
        if (!hasText(username)) {
            throw new BadRequestException("账号不能为空");
        }
        if (!hasText(password)) {
            throw new BadRequestException("密码不能为空");
        }
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

