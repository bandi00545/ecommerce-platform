package com.ecommerce.common.config;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.common.filter.RequestContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
    JacksonConfig.class,
    GlobalExceptionHandler.class,
    RequestContextFilter.class
})
public class CommonAutoConfiguration {
    // No additional bean definitions here.
    // All beans are defined in the @Import-ed classes.
}
