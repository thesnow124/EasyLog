package com.github.easylog;

import com.github.easylog.TestLog.UserDto;
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.api.ILogRecordService;
import com.github.easylog.api.IOperatorService;
import com.github.easylog.constants.OperateType;
import com.github.easylog.model.EasyLogInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "easylog.enable=true",
        "easylog.store=log"
})
class TestLogIntegrationTests {

    @Autowired
    private TestLog testLog;

    @Autowired
    private CapturingLogRecordService capturing;

    @BeforeEach
    void setUp() {
        capturing.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void update_shouldRecordOneSuccessLog_withExpectedContentAndContext() {
        // given
        UserDto dto = new UserDto();
        dto.setId(123L);
        dto.setName("Alice");

        // and request context
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/user/update");
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when
        TestLog.UserEntity result = testLog.update(dto);

        // then
        assertNotNull(result);
        assertEquals(123L, result.getId());
        assertEquals("Alice", result.getName());

        List<EasyLogInfo> logs = capturing.copy();
        assertEquals(1, logs.size());
        EasyLogInfo log = logs.get(0);

        assertEquals("用户管理", log.getModule());
        assertEquals("UPDATE", log.getType());
        assertTrue(Objects.equals(Boolean.TRUE, log.getSuccess()));
        assertEquals("更新了用户信息：Alice", log.getContent());
        assertEquals("tester", log.getOperator());
        assertEquals("test-service", log.getPlatform());

        assertEquals("203.0.113.1", log.getIp());
        assertEquals("POST", log.getHttpMethod());
        assertTrue(log.getUrl().contains("/user/update"));
        assertTrue(log.getClassMethod().contains("TestLog.update"));
        assertNotNull(log.getResult());
        assertNotNull(log.getOperateTime());
        assertNotNull(log.getExecuteTime());
        assertNotNull(log.getParam());
    }

    @Test
    void manyLog_shouldProduceTwoAnnotationLogs() {
        // when
        testLog.manyLog("Bob");

        // then
        List<EasyLogInfo> logs = capturing.copy();
        assertEquals(2, logs.size());

        List<String> types = logs.stream().map(EasyLogInfo::getType).collect(Collectors.toList());
        assertTrue(types.contains("UPDATE"));
        assertTrue(types.contains("READ"));
        logs.forEach(l -> assertTrue(Objects.equals(Boolean.TRUE, l.getSuccess())));
    }

    @Test
    void failingMethod_shouldRecordFailLog_andRethrow() {
        // given
        capturing.clear();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> failingService.run());
        assertEquals("boom", ex.getMessage());

        List<EasyLogInfo> logs = capturing.copy();
        assertEquals(1, logs.size());
        EasyLogInfo log = logs.get(0);
        assertEquals("FAIL: boom", log.getContent());
        assertEquals("用户管理", log.getModule());
        assertEquals("UPDATE", log.getType());
        assertEquals("tester", log.getOperator());
        assertEquals("test-service", log.getPlatform());
        assertFalse(log.getSuccess());
        assertEquals("boom", log.getErrorMsg());
        assertNotNull(log.getStackTrace());
    }

    // helper to access bean from test config
    @Autowired
    private FailingService failingService;

    @TestConfiguration
    static class Beans {
        @Bean
        ILogRecordService logRecordService() {
            return new CapturingLogRecordService();
        }

        @Bean
        IOperatorService operatorService() {
            return new IOperatorService() {
                @Override
                public String getOperator() { return "tester"; }
                @Override
                public String getPlatform() { return "test-service"; }
            };
        }

        @Bean(name = "easyLogFunctions")
        EasyLogFunctions easyLogFunctions() { return new EasyLogFunctions(); }

        @Bean
        FailingService failingService() { return new FailingService(); }
    }

    static class EasyLogFunctions {
        public String getBeforeRealNameByName(String name) {
            // simulate a lookup, return name directly for test
            return name;
        }
        public String userLabel(Long userId) {
            return "U-" + userId;
        }
    }

    static class CapturingLogRecordService implements ILogRecordService {
        private static final List<EasyLogInfo> CAPTURED = new CopyOnWriteArrayList<>();
        @Override
        public void record(EasyLogInfo log) { CAPTURED.add(log); }
        void clear() { CAPTURED.clear(); }
        List<EasyLogInfo> copy() { return new ArrayList<>(CAPTURED); }
    }

    static class FailingService {
        @EasyLog(module = "用户管理", type = OperateType.UPDATE,
                success = "OK", fail = "FAIL: {{#_errMsg}}")
        public void run() {
            throw new RuntimeException("boom");
        }
    }
}
