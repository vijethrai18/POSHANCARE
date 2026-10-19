
package com.poshancare.config;

import com.poshancare.entity.AccountStatus;
import com.poshancare.entity.AppUser;
import com.poshancare.entity.UserRole;
import com.poshancare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${poshancare.admin.mobile:${ADMIN_MOBILE:}}")
    private String adminMobile;

    @Value("${poshancare.admin.password:${ADMIN_PASSWORD:}}")
    private String adminPassword;

    @Value("${poshancare.admin.name:${ADMIN_NAME:System Administrator}}")
    private String adminName;

    public AdminBootstrapInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // Don't create another administrator if one already exists.
        if (!userRepository.findByRole(UserRole.ADMIN).isEmpty()) {
            log.info(
                    "Administrator account already exists. Skipping bootstrap initialization.");
            return;
        }

        // Never create an administrator with missing credentials.
        if (adminMobile == null || adminMobile.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn(
                    "Admin bootstrap skipped: Configure ADMIN_MOBILE and ADMIN_PASSWORD.");
            return;
        }

        // Avoid conflicts with an existing user's mobile number.
        if (userRepository.existsByMobile(adminMobile)) {
            log.warn(
                    "Cannot initialize administrator: The configured mobile is already registered.");
            return;
        }

        AppUser admin = new AppUser();
        admin.setId("ADMIN001");
        admin.setFullName(adminName);
        admin.setMobile(adminMobile);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.APPROVED);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setPreferredLanguage("en");

        userRepository.save(admin);

        log.info("Initial administrator account initialized with ID ADMIN001.");
    }
}
