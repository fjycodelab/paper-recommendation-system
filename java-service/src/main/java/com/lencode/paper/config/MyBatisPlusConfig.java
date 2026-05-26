package com.lencode.paper.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.lencode.paper.auth.mapper",
        "com.lencode.paper.tag.mapper",
        "com.lencode.paper.paper.mapper",
        "com.lencode.paper.download.mapper",
        "com.lencode.paper.behavior.mapper"
})
public class MyBatisPlusConfig {
}
