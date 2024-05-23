package com.github.easylog;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.constants.OperateType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class EasyLogApplicationTests {


    @Resource
    private TestLog testLog;

    @EasyLog(module = "用户管理", type = OperateType.UPDATE,  success = "查询结果： {{#_result}}")
    @Test
    public void internalMethod() {
        TestLog.UserDto userDto1 = new TestLog.UserDto();
        userDto1.setId(0L);
        userDto1.setName("ss");
        TestLog.UserEntity update = testLog.update(userDto1);
    }



    @Test
    public void manyLog() {
        testLog.manyLog("ss");
    }




}
