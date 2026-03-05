package com.lcyhz.urbanova;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zt
 */
@MapperScan("com.lcyhz.urbanova.mapper")
@SpringBootApplication
public class UrbanovaApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrbanovaApplication.class, args);
    }

}
