package com.github.easylog;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.api.ILogRecordService;
import com.github.easylog.api.IOperatorService;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.constants.OperateType;
import com.github.easylog.function.ParseFunction;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.util.PlaceholderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EasyLogAnnotationFeaturesTest.Config.class)
class EasyLogAnnotationFeaturesTest {

    @Autowired
    DemoService demoService;
    @Autowired
    CapturingLogRecordService logRecordService;

    @AfterEach
    void clear() {
        logRecordService.clear();
    }

    @Test
    void successLog_withDslFunctionsAndBizNo() {
        demoService.create("alice");
        EasyLogInfo log = logRecordService.copy().get(0);
        assertEquals("DEMO", log.getModule());
        assertEquals(OperateType.CREATE, log.getType());
        assertEquals("tester", log.getOperator());
        assertEquals("test-service", log.getPlatform());
        assertEquals("ALICE", log.getBizNo(), "bizNo 应由 upper 函数转换为大写");
        assertTrue(log.getSuccess());
        assertEquals("created before-alice", log.getContent(), "应替换函数与占位符");
    }

    @Test
    void successLog_withPlaceholdersAndCondition() {
        demoService.update("bob", "late", true);
        EasyLogInfo log = logRecordService.copy().get(0);
        assertEquals("user bob reason late", log.getContent());
        assertTrue(log.getSuccess());
        assertEquals("true", log.getCondition());
    }

    @Test
    void conditionFalse_shouldSkipLog() {
        demoService.update("bob", "late", false);
        assertTrue(logRecordService.copy().isEmpty(), "condition=false 时不应记录日志");
    }

    @Test
    void failLog_recordsErrorTemplate() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> demoService.fail("carl"));
        assertEquals("boom", ex.getMessage());
        EasyLogInfo log = logRecordService.copy().get(0);
        assertFalse(log.getSuccess());
        assertEquals("fail boom", log.getContent());
        assertEquals("boom", log.getErrorMsg());
    }

    @Test
    void overridePlatformOperatorAndFailParams() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> demoService.failWithParam("dora"));
        assertEquals("fail boom", ex.getMessage());
        EasyLogInfo log = logRecordService.copy().get(0);
        assertEquals("anno-platform", log.getPlatform());
        assertEquals("dora", log.getOperator());
        assertEquals("fail boom", log.getContent());
        assertEquals("boom", log.getErrorMsg());
    }

    @Test
    void detailDiffGeneratesFieldInfo() {
        demoService.diff("{\"age\":1}", "{\"age\":2}");
        EasyLogInfo log = logRecordService.copy().get(0);
        assertEquals("[{\"age\":1}, {\"age\":2}]", log.getDetail());
        List<FieldInfo> fields = log.getFieldInfoList();
        assertEquals(1, fields.size());
        assertEquals("age", fields.get(0).getFieldName());
        assertEquals("1", fields.get(0).getOldFieldVal());
        assertEquals("2", fields.get(0).getNewFieldVal());
    }

    @Test
    void detailFromJavaObjectShouldSerialize() {
        demoService.detailFromObject(Map.of("foo", "bar"));
        EasyLogInfo log = logRecordService.copy().get(0);
        assertEquals("{\"foo\":\"bar\"}", log.getDetail());
        List<FieldInfo> fields = log.getFieldInfoList();
        assertEquals(1, fields.size());
        assertEquals("{\"foo\":\"bar\"}", fields.get(0).getVal());
    }

    @Test
    void detailDiffWithObjectsShouldSerializeAndDiff() {
        Map<String, Object> oldObj = Map.of("age", 1);
        Map<String, Object> newObj = Map.of("age", 2);
        demoService.diffObjects(oldObj, newObj);
        EasyLogInfo log = logRecordService.copy().get(0);
        assertEquals("[{\"age\":1},{\"age\":2}]", log.getDetail().replaceAll("\\s", ""));
        List<FieldInfo> fields = log.getFieldInfoList();
        assertEquals(1, fields.size());
        assertEquals("map[age]", fields.get(0).getFieldName());
        assertEquals("1", fields.get(0).getOldFieldVal());
        assertEquals("2", fields.get(0).getNewFieldVal());
    }

    @TestConfiguration
    static class Config {
        @Bean
        DemoService demoService() { return new DemoService(); }

        @Bean
        IOperatorService operatorService() {
            return new IOperatorService() {
                @Override
                public String getOperator() { return "tester"; }
                @Override
                public String getPlatform() { return "test-service"; }
            };
        }

        @Bean
        ParseFunction beforeFunction() {
            return new ParseFunction() {
                @Override
                public String functionName() { return "before"; }
                @Override
                public String apply(String value) { return "before-" + value; }
                @Override
                public boolean executeBefore() { return true; }
            };
        }

        @Bean
        ParseFunction upperFunction() {
            return new ParseFunction() {
                @Override
                public String functionName() { return "upper"; }
                @Override
                public String apply(String value) { return value == null ? "" : value.toUpperCase(); }
            };
        }

        @Bean
        CapturingLogRecordService logRecordService() { return new CapturingLogRecordService(); }
    }

    static class CapturingLogRecordService implements ILogRecordService {
        private final CopyOnWriteArrayList<EasyLogInfo> logs = new CopyOnWriteArrayList<>();
        @Override
        public void record(EasyLogInfo log) {
            // 模拟 DefaultLogRecordServiceImpl 的占位符替换，以便断言最终内容
            String resolved = PlaceholderResolver.getDefaultResolver().resolve(log.getContent(), log.getContentParam());
            log.setContent(resolved);
            logs.add(log);
        }
        void clear() { logs.clear(); }
        List<EasyLogInfo> copy() { return new ArrayList<>(logs); }
    }

    static class DemoService {
        @EasyLog(module = "DEMO", type = OperateType.CREATE,
                bizNo = "{upper{#name}}",
                success = "created {before{#name}}",
                detail = "{before{#name}}")
        public String create(String name) { return name; }

        @EasyLog(module = "DEMO", type = OperateType.UPDATE,
                success = "user ${} reason ${}",
                successParamList = {"{{#name}}","{{#reason}}"},
                condition = "{{#enabled}}")
        public String update(String name, String reason, boolean enabled) { return name; }

        @EasyLog(module = "DEMO", type = OperateType.DELETE,
                bizNo = "{{#name}}",
                success = "ok",
                fail = "fail {{#_errMsg}}")
        public void fail(String name) {
            throw new RuntimeException("boom");
        }

        @EasyLog(module = "DEMO", type = OperateType.DELETE,
                platform = "anno-platform",
                operator = "{{#name}}",
                bizNo = "{{#name}}",
                success = "ok",
                fail = "fail ${}",
                failParamList = {"{{#_errMsg}}"})
        public void failWithParam(String name) {
            throw new RuntimeException("boom");
        }

        @EasyLog(module = "DEMO", type = OperateType.UPDATE,
                detail = "[{{#oldJson}}, {{#newJson}}]",
                success = "diff")
        public void diff(String oldJson, String newJson) { }

        @EasyLog(module = "DEMO", type = OperateType.READ,
                detail = "{{#payload}}",
                success = "obj")
        public void detailFromObject(Map<String, Object> payload) { }

        @EasyLog(module = "DEMO", type = OperateType.UPDATE,
                detail = "[{{#oldObj}}, {{#newObj}}]",
                success = "diff-obj")
        public void diffObjects(Map<String, Object> oldObj, Map<String, Object> newObj) { }
    }
}
