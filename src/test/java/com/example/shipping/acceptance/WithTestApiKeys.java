package com.example.shipping.acceptance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.TestPropertySource;

/** Configures a known USER and ADMIN API key for an acceptance test class. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
        "api.keys.user=" + TestApiKeys.USER,
        "api.keys.admin=" + TestApiKeys.ADMIN
})
public @interface WithTestApiKeys {
}
