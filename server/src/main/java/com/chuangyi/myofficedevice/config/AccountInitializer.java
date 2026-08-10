package com.chuangyi.myofficedevice.config;

import com.chuangyi.myofficedevice.user.UserAccount;
import com.chuangyi.myofficedevice.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AccountInitializer.class);
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountInitializer(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${app.admin.username:admin}")
    private String adminUsername;
    @Value("${app.admin.password:admin123}")
    private String adminPassword;
    @Value("${app.admin.display-name:管理员}")
    private String adminDisplayName;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userAccountRepository.findByUsername(adminUsername).isPresent()) return;
        UserAccount admin = new UserAccount();
        admin.setUsername(adminUsername);
        admin.setDisplayName(adminDisplayName);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        userAccountRepository.save(admin);
        log.warn("Initialized NetDesk administrator account '{}'. Change NETDESK_ADMIN_PASSWORD before production use.", adminUsername);
    }
}
