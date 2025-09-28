package com.github.easylog;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.annotation.EasyLogs;
import com.github.easylog.constants.OperateType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 示例用法（测试环境）
 */
@Service
public class TestLog {

    @EasyLogs({
            @EasyLog(module = "用户管理", type = OperateType.UPDATE, success = "测试多个日志-1： ${} 这是后缀${}",successParamList = {"{{@easyLogFunctions.getBeforeRealNameByName(#p0)}}"}),
            @EasyLog(module = "用户管理", type = OperateType.READ, success = "测试多个日志-2： {{@easyLogFunctions.getBeforeRealNameByName(#p0)}}")
    })
    public void manyLog(String name) {
        UserEntity old = new UserEntity();
        old.setId(1L);
        old.setName("AAAA");
        UserEntity ne = new UserEntity();
        ne.setId(2L);
        ne.setName("BBBB");
        // manual record removed; prefer annotation+detail or context variables

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
