package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.dto.TestDto;
import com.lcyhz.urbanova.service.TestService;
import com.lcyhz.urbanova.vo.TestVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zt
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TestService testService;

    @PostMapping("/add")
    public TestVo add(@RequestBody TestDto test) {
        return testService.add(test);
    }
}
