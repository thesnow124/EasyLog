package com.github.easylog;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.annotation.EasyLogs;
import com.github.easylog.constants.OperateType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 测试方法
 * @date 2024/5/10 13:47:05
 */
@Service
public class TestLog {

    @EasyLogs({
            @EasyLog(module = "用户管理", type = OperateType.UPDATE, success = "测试多个日志-1： ${} 这是后缀${}",successParamList = {"{getBeforeRealNameByName{#name}}"}),
            @EasyLog(module = "用户管理", type = OperateType.READ, success = "测试多个日志-2： {getBeforeRealNameByName{#name}}")
    })
    public void manyLog(String name) {

    }

    @EasyLog(module = "用户管理", type = OperateType.UPDATE, success = "更新了用户信息：{{#userDto.name}}")
    public UserEntity  update(UserDto userDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userDto.getId());
        userEntity.setName(userDto.getName());
        return userEntity;
    }

    @Data
    public static class UserEntity {
        private Long id;
        private String name;

        public void setId(Long id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Data
    @NoArgsConstructor
    public static class UserDto {
        private Long id;
        private String name;

    }
}
