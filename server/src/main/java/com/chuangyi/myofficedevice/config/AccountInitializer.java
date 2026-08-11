package com.chuangyi.myofficedevice.config;

import com.chuangyi.myofficedevice.user.UserAccount;
import com.chuangyi.myofficedevice.user.UserAccountRepository;
import com.chuangyi.myofficedevice.user.UserRole;
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
        UserAccount admin = userAccountRepository.findByUsername(adminUsername).orElse(null);
        if (admin != null) {
            if (admin.getRole() != UserRole.SUPER_ADMIN) {
                admin.setRole(UserRole.SUPER_ADMIN);
                admin.setEnabled(true);
                userAccountRepository.save(admin);
                log.info("Upgraded initialized account '{}' to super administrator.", adminUsername);
            }
            return;
        }
        UserAccount initialAdmin = new UserAccount();
        initialAdmin.setUsername(adminUsername);
        initialAdmin.setDisplayName(adminDisplayName);
        initialAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
        initialAdmin.setRole(UserRole.SUPER_ADMIN);
        userAccountRepository.save(initialAdmin);
        log.warn("Initialized NetDesk super administrator account '{}'. Change NETDESK_ADMIN_PASSWORD before production use.", adminUsername);
    }
}
