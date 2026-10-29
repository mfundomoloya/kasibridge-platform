package com.kasibridge.auth;

import com.kasibridge.auth.security.PrivateKeyProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @MockitoBean
    private PrivateKeyProvider privateKeyProvider;

    @Test
    void contextLoads() {
    }
}