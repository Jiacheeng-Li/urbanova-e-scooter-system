package com.lcyhz.urbanova.service.impl;

import com.lcyhz.urbanova.dto.TestDto;
import com.lcyhz.urbanova.entity.Test;
import com.lcyhz.urbanova.mapper.TestMapper;
import com.lcyhz.urbanova.service.TestService;
import com.lcyhz.urbanova.vo.TestVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zt
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private TestMapper testMapper;
    @Override
    public TestVo add(TestDto testDto) {
        Test test = new Test();
        test.setName(testDto.getName());
        test.setAge(testDto.getAge());
        int insert = testMapper.insert(test);
        if (insert > 0) {
            TestVo testVo = new TestVo();
            testVo.setId(test.getId());
            testVo.setName(test.getName());
            testVo.setAge(test.getAge());
            testVo.setComment("添加成功");
            return testVo;
        } else {
            TestVo testVo = new TestVo();
            testVo.setComment("添加失败");
            return testVo;
        }
    }

}
