package com.alisonpariela.blogmanager.security;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.User;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest {
    @Test void distinguishesAccessAndRefreshTokens() {
        var service = new JwtService();
        ReflectionTestUtils.setField(service,"secretKey","Zm9saW8tdGVzdC1vbmx5LXNlY3JldC0zMi1ieXRlcy1sb25nISE=");
        ReflectionTestUtils.setField(service,"jwtExpiration",3600000L);
        ReflectionTestUtils.setField(service,"refreshExpiration",86400000L);
        var user = new User("writer","password",List.of());
        var access = service.generateToken(user); var refresh = service.generateRefreshToken(user);
        assertTrue(service.isTokenValid(access,user)); assertFalse(service.isTokenValid(refresh,user));
        assertTrue(service.isRefreshTokenValid(refresh,user)); assertFalse(service.isRefreshTokenValid(access,user));
        assertFalse(service.isTokenValid(access,new User("other","password",List.of())));
    }
}
