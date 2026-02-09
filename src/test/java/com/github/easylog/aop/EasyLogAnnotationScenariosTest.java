package com.github.easylog.aop;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.annotation.EasyLogDiffField;
import com.github.easylog.annotation.EasyLogDiffObject;
import com.github.easylog.annotation.EasyLogs;
import com.github.easylog.configuration.EasyLogProperties;
import com.github.easylog.context.EasyLogContext;
import com.github.easylog.diff.DefaultDiffEngine;
import com.github.easylog.diff.DiffDTO;
import com.github.easylog.diff.DiffFieldDTO;
import com.github.easylog.function.DefaultFunctionServiceImpl;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.function.IFunctionService;
import com.github.easylog.function.IParseFunction;
import com.github.easylog.function.ParseFunctionFactory;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.util.PlaceholderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EasyLogAnnotationScenariosTest.TestConfig.class)
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = MergeMode.REPLACE_DEFAULTS
)
class EasyLogAnnotationScenariosTest {

    @Autowired
    ScenarioService scenarioService;
    @Autowired
    CapturingLogRecordService logRecordService;
    @Autowired
    ScenarioStore scenarioStore;

    @BeforeEach
    void reset() {
        logRecordService.clear();
        scenarioStore.clear();
    }

    @Nested
    @DisplayName("Basic SpEL")
    class BasicSpel {
        @Test
        void renders_operator_platform_bizno_and_result() {
            ScenarioRequest request = ScenarioRequest.base()
                    .withOperator("bob")
                    .withBizNo("B-100")
                    .withPlatform("mobile")
                    .withAddress("road-1");

            String result = scenarioService.basic(request);
            assertEquals("ok", result);

            EasyLogInfo info = singleLog();
            assertEquals("B-100", info.getBizNo());
            assertEquals("hello bob at road-1 result ok", info.getContent());
            assertEquals("user", info.getModule());
            assertEquals("UPDATE", info.getType());
            assertEquals("bob", info.getOperator());
            assertEquals("mobile", info.getPlatform());
            assertTrue(info.getSuccess());
        }
    }

    @Nested
    @DisplayName("Placeholder Params")
    class PlaceholderParams {
        @Test
        void resolves_placeholder_params_with_address() {
            ScenarioRequest request = ScenarioRequest.base()
                    .withOperator("alice")
                    .withAddress("street-9");

            scenarioService.placeholder(request);

            EasyLogInfo info = singleLog();
            assertEquals("create alice at street-9", info.getContent());
            assertEquals("B-1", info.getBizNo());
        }
    }

    @Nested
    @DisplayName("Beans and Context")
    class BeansAndContext {
        @Test
        void uses_context_old_value() {
            ScenarioRequest request = ScenarioRequest.base()
                    .withBizNo("B-200")
                    .withAddress("new-addr");
            scenarioStore.put("B-200", "old-addr");

            scenarioService.updateAddress(request);

            EasyLogInfo info = singleLog();
            assertEquals("old old-addr new new-addr", info.getContent());
            assertEquals("new-addr", scenarioStore.get("B-200"));
        }

        @Test
        void resolves_bean_method_call() {
            ScenarioRequest request = ScenarioRequest.base().withLabelId("L-1");

            scenarioService.beanCall(request);

            EasyLogInfo info = singleLog();
            assertEquals("label label-L-1", info.getContent());
        }
    }

    @Nested
    @DisplayName("Conditions and Failures")
    class ConditionsAndFailures {
        @Test
        void skips_when_condition_false() {
            ScenarioRequest request = ScenarioRequest.base().withEnabled(false);

            scenarioService.conditional(request);

            assertTrue(logRecordService.snapshot().isEmpty());
        }

        @Test
        void uses_fail_template_and_default_operator_platform() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-9");

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scenarioService.fail(request));
            assertEquals("boom", ex.getMessage());

            EasyLogInfo info = singleLog();
            assertFalse(info.getSuccess());
            assertEquals("fail boom ctx v-B-9", info.getContent());
            assertEquals("boom", info.getErrorMsg());
            assertEquals("default-op", info.getOperator());
            assertEquals("default-plat", info.getPlatform());
        }
    }

    @Nested
    @DisplayName("DiffKey")
    class DiffKey {
        @Test
        void uses_diff_function_expression() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-201");

            scenarioService.diffWithFunction(request);

            EasyLogInfo info = singleLog();
            assertNotNull(info.getDiffDTO());
            assertEquals(1, info.getDiffDTO().getDiffFieldDTOList().size());
            DiffFieldDTO field = info.getDiffDTO().getDiffFieldDTOList().get(0);
            assertEquals("age", field.getFieldName());
            assertEquals(1, field.getOldValue());
            assertEquals(2, field.getNewValue());
        }

        @Test
        void uses_diff_from_context_key() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-202");

            scenarioService.diffWithContext(request);

            EasyLogInfo info = singleLog();
            assertNotNull(info.getDiffDTO());
            assertEquals(1, info.getDiffDTO().getDiffFieldDTOList().size());
            DiffFieldDTO field = info.getDiffDTO().getDiffFieldDTOList().get(0);
            assertEquals("name", field.getFieldName());
            assertEquals("old", field.getOldValue());
            assertEquals("new", field.getNewValue());
        }
    }

    @Nested
    @DisplayName("Context Vars")
    class ContextVars {
        @Test
        void allows_context_variables_to_override_params() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-300");

            scenarioService.withContext(request);

            EasyLogInfo info = singleLog();
            assertEquals("ctx v-B-300 biz B-999", info.getContent());
        }
    }

    @Nested
    @DisplayName("Repeatable Annotations")
    class RepeatableAnnotations {
        @Test
        void records_multiple_logs() {
            ScenarioRequest request = ScenarioRequest.base().withOperator("leo");

            scenarioService.multi(request);

            List<EasyLogInfo> logs = logRecordService.snapshot();
            assertEquals(2, logs.size());
            Set<String> modules = logs.stream().map(EasyLogInfo::getModule).collect(Collectors.toSet());
            assertTrue(modules.contains("user"));
            assertTrue(modules.contains("audit"));
            Set<String> contents = logs.stream().map(EasyLogInfo::getContent).collect(Collectors.toSet());
            assertTrue(contents.contains("user-log"));
            assertTrue(contents.contains("audit leo"));
        }
    }

    @Nested
    @DisplayName("P0 Critical Combos")
    class P0CriticalCombos {
        @Test
        void empty_fail_template_records_null_content() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P0-1");
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scenarioService.failWithEmptyTemplate(request));
            assertEquals("boom-empty", ex.getMessage());

            EasyLogInfo info = singleLog();
            assertFalse(info.getSuccess());
            assertNull(info.getContent());
            assertEquals("boom-empty", info.getErrorMsg());
        }

        @Test
        void placeholder_with_missing_params_keeps_unresolved_placeholder() {
            ScenarioRequest request = ScenarioRequest.base().withOperator("amy").withBizNo("B-P0-2");
            scenarioService.placeholderMissingParam(request);

            EasyLogInfo info = singleLog();
            assertEquals("a amy b ${}", info.getContent());
        }

        @Test
        void placeholder_with_extra_params_ignores_extra_values() {
            ScenarioRequest request = ScenarioRequest.base().withOperator("amy").withBizNo("B-P0-3");
            scenarioService.placeholderExtraParam(request);

            EasyLogInfo info = singleLog();
            assertEquals("a amy", info.getContent());
        }

        @Test
        void non_boolean_condition_string_skips_recording() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P0-4");
            scenarioService.conditionalStringYes(request);
            assertTrue(logRecordService.snapshot().isEmpty());
        }

        @Test
        void null_condition_skips_recording() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P0-5");
            scenarioService.conditionalNull(request);
            assertTrue(logRecordService.snapshot().isEmpty());
        }

        @Test
        void diff_key_expression_can_fallback_to_context_key() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P0-6");
            scenarioService.diffKeyExpressionReturnsString(request);

            EasyLogInfo info = singleLog();
            assertNotNull(info.getDiffDTO());
            assertEquals(1, info.getDiffDTO().getDiffFieldDTOList().size());
            DiffFieldDTO field = info.getDiffDTO().getDiffFieldDTOList().get(0);
            assertEquals("status", field.getFieldName());
            assertEquals("old", field.getOldValue());
            assertEquals("new", field.getNewValue());
        }

        @Test
        void missing_diff_key_returns_null_diff_dto() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P0-7");
            scenarioService.diffKeyMissing(request);

            EasyLogInfo info = singleLog();
            assertNull(info.getDiffDTO());
        }
    }

    @Nested
    @DisplayName("P1 Important Combos")
    class P1ImportantCombos {
        @Test
        void operator_fallback_only() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P1-1").withPlatform("mobile");
            scenarioService.operatorFallbackOnly(request);

            EasyLogInfo info = singleLog();
            assertEquals("default-op", info.getOperator());
            assertEquals("mobile", info.getPlatform());
        }

        @Test
        void platform_fallback_only() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P1-2").withOperator("tom");
            scenarioService.platformFallbackOnly(request);

            EasyLogInfo info = singleLog();
            assertEquals("tom", info.getOperator());
            assertEquals("default-plat", info.getPlatform());
        }

        @Test
        void extra_is_resolved_from_expression() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P1-3").withAddress("addr-x");
            scenarioService.extraResolved(request);

            EasyLogInfo info = singleLog();
            assertEquals("addr-x", info.getExtra());
        }

        @Test
        void empty_extra_keeps_null() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P1-4");
            scenarioService.extraEmpty(request);

            EasyLogInfo info = singleLog();
            assertNull(info.getExtra());
        }

        @Test
        void repeatable_annotations_with_mixed_conditions_record_partial_logs() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P1-5");
            scenarioService.multiWithCondition(request);

            List<EasyLogInfo> logs = logRecordService.snapshot();
            assertEquals(1, logs.size());
            assertEquals("user-cond", logs.get(0).getModule());
            assertEquals("cond-true", logs.get(0).getContent());
        }

        @Test
        void repeatable_annotations_use_fail_templates_on_exception() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-P1-6");
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scenarioService.multiFail(request));
            assertEquals("boom-multi", ex.getMessage());

            List<EasyLogInfo> logs = logRecordService.snapshot();
            assertEquals(2, logs.size());
            Set<String> contents = logs.stream().map(EasyLogInfo::getContent).collect(Collectors.toSet());
            assertTrue(contents.contains("fail-one boom-multi"));
            assertTrue(contents.contains("fail-two boom-multi"));
            assertTrue(logs.stream().allMatch(log -> !log.getSuccess()));
        }
    }

    @Nested
    @DisplayName("P2 Edge Combos")
    class P2EdgeCombos {
        @Test
        void biz_no_can_be_null() {
            scenarioService.bizNoNull(ScenarioRequest.base().withBizNo("B-P2-1"));
            EasyLogInfo info = singleLog();
            assertNull(info.getBizNo());
        }

        @Test
        void empty_success_template_with_params_records_null_content() {
            scenarioService.successEmptyWithParams(ScenarioRequest.base().withBizNo("B-P2-2"));
            EasyLogInfo info = singleLog();
            assertNull(info.getContent());
        }

        @Test
        void success_template_can_reference_errmsg_as_empty() {
            scenarioService.successUsesErrMsg(ScenarioRequest.base().withBizNo("B-P2-3"));
            EasyLogInfo info = singleLog();
            assertEquals("err ", info.getContent());
        }

        @Test
        void unknown_function_in_single_block_returns_null_content() {
            scenarioService.unknownFunctionInTemplate(ScenarioRequest.base().withBizNo("B-P2-4"));
            EasyLogInfo info = singleLog();
            assertNull(info.getContent());
        }
    }

    private EasyLogInfo singleLog() {
        List<EasyLogInfo> logs = logRecordService.snapshot();
        assertEquals(1, logs.size());
        return logs.get(0);
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        ScenarioStore scenarioStore() {
            return new ScenarioStore();
        }

        @Bean
        ScenarioService scenarioService(ScenarioStore scenarioStore) {
            return new ScenarioService(scenarioStore);
        }

        @Bean
        LabelService labelService() {
            return new LabelService();
        }

        @Bean
        IOperatorService operatorService() {
            return new FixedOperatorService();
        }

        @Bean
        CapturingLogRecordService logRecordService() {
            return new CapturingLogRecordService();
        }

        @Bean
        EasyLogProperties easyLogProperties() {
            return new EasyLogProperties();
        }

        @Bean
        IParseFunction diffFunction(EasyLogProperties properties) {
            return new DefaultDiffEngine(properties);
        }

        @Bean
        ParseFunctionFactory parseFunctionFactory(List<IParseFunction> parseFunctions) {
            return new ParseFunctionFactory(parseFunctions);
        }

        @Bean
        IFunctionService functionService(ParseFunctionFactory parseFunctionFactory) {
            return new DefaultFunctionServiceImpl(parseFunctionFactory);
        }

        @Bean
        EasyLogParser easyLogParser(IFunctionService functionService) {
            return new EasyLogParser(functionService);
        }

        @Bean
        EasyLogAspect easyLogAspect(ILogRecordService logRecordService,
                                   IOperatorService operatorService,
                                   EasyLogParser easyLogParser) {
            return new EasyLogAspect(logRecordService, operatorService, easyLogParser, false);
        }
    }

    static class ScenarioService {
        private final ScenarioStore store;

        ScenarioService(ScenarioStore store) {
            this.store = store;
        }

        @EasyLog(
                platform = "{{#request.platform}}",
                operator = "{{#request.operator}}",
                module = "user",
                type = "UPDATE",
                bizNo = "#request.bizNo",
                success = "hello {{#request.operator}} at {{#request.address}} result {{#_result}}"
        )
        public String basic(ScenarioRequest request) {
            return "ok";
        }

        @EasyLog(
                module = "user",
                type = "CREATE",
                bizNo = "#request.bizNo",
                success = "create ${} at ${}",
                successParamList = {"{{#request.operator}}", "{{#request.address}}"}
        )
        public void placeholder(ScenarioRequest request) {
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "#request.bizNo",
                success = "ok",
                condition = "{{#request.enabled}}"
        )
        public void conditional(ScenarioRequest request) {
        }

        @EasyLog(
                module = "user",
                type = "DELETE",
                bizNo = "{{#request.bizNo}}",
                success = "ok",
                fail = "fail ${} ctx ${}",
                failParamList = {"{{#_errMsg}}", "{{#ctxFail}}"}
        )
        public void fail(ScenarioRequest request) {
            EasyLogContext.put("ctxFail", "v-" + request.getBizNo());
            throw new IllegalStateException("boom");
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                diffKey = "{{DIFF(#oldObj,#newObj)}}",
                success = "diff"
        )
        public void diffWithFunction(ScenarioRequest request) {
            DiffUser oldObj = new DiffUser(1, "same");
            DiffUser newObj = new DiffUser(2, "same");
            EasyLogContext.put("oldObj", oldObj);
            EasyLogContext.put("newObj", newObj);
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                diffKey = "diff",
                success = "diff-manual"
        )
        public void diffWithContext(ScenarioRequest request) {
            DiffDTO diffDTO = new DiffDTO();
            DiffFieldDTO field = new DiffFieldDTO();
            field.setFieldName("name");
            field.setOldValue("old");
            field.setNewValue("new");
            diffDTO.setDiffFieldDTOList(java.util.Collections.singletonList(field));
            EasyLogContext.put("diff", diffDTO);
        }


        @EasyLog(
                module = "user",
                type = "READ",
                bizNo = "{{#request.bizNo}}",
                success = "label {{@labelService.label(#request.labelId)}}"
        )
        public void beanCall(ScenarioRequest request) {
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                success = "old {{#oldAddr}} new {{#request.address}}"
        )
        public void updateAddress(ScenarioRequest request) {
            EasyLogContext.put("oldAddr", store.get(request.getBizNo()));
            store.put(request.getBizNo(), request.getAddress());
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                success = "ctx {{#ctxVar}} biz {{#request.bizNo}}"
        )
        public void withContext(ScenarioRequest request) {
            EasyLogContext.put("ctxVar", "v-" + request.getBizNo());
            ScenarioRequest shadow = ScenarioRequest.base().withBizNo("B-999");
            EasyLogContext.put("request", shadow);
        }

        @EasyLogs({
                @EasyLog(
                        module = "user",
                        type = "CREATE",
                        bizNo = "{{#request.bizNo}}",
                        success = "user-log"
                ),
                @EasyLog(
                        module = "audit",
                        type = "TRACE",
                        bizNo = "{{#request.bizNo}}",
                        success = "audit {{#request.operator}}"
                )
        })
        public void multi(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p0",
                type = "DELETE",
                bizNo = "{{#request.bizNo}}",
                success = "ok",
                fail = ""
        )
        public void failWithEmptyTemplate(ScenarioRequest request) {
            throw new IllegalStateException("boom-empty");
        }

        @EasyLog(
                module = "p0",
                type = "CREATE",
                bizNo = "#request.bizNo",
                success = "a ${} b ${}",
                successParamList = {"{{#request.operator}}"}
        )
        public void placeholderMissingParam(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p0",
                type = "CREATE",
                bizNo = "#request.bizNo",
                success = "a ${}",
                successParamList = {"{{#request.operator}}", "{{#request.bizNo}}"}
        )
        public void placeholderExtraParam(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p0",
                type = "UPDATE",
                bizNo = "#request.bizNo",
                success = "ok",
                condition = "{{'yes'}}"
        )
        public void conditionalStringYes(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p0",
                type = "UPDATE",
                bizNo = "#request.bizNo",
                success = "ok",
                condition = "{{null}}"
        )
        public void conditionalNull(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p0",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                diffKey = "{{'manualDiff'}}",
                success = "diff-fallback"
        )
        public void diffKeyExpressionReturnsString(ScenarioRequest request) {
            DiffDTO diffDTO = new DiffDTO();
            DiffFieldDTO field = new DiffFieldDTO();
            field.setFieldName("status");
            field.setOldValue("old");
            field.setNewValue("new");
            diffDTO.setDiffFieldDTOList(java.util.Collections.singletonList(field));
            EasyLogContext.put("manualDiff", diffDTO);
        }

        @EasyLog(
                module = "p0",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                diffKey = "missingDiff",
                success = "diff-missing"
        )
        public void diffKeyMissing(ScenarioRequest request) {
        }

        @EasyLog(
                platform = "{{#request.platform}}",
                operator = "",
                module = "p1",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                success = "operator-fallback"
        )
        public void operatorFallbackOnly(ScenarioRequest request) {
        }

        @EasyLog(
                platform = "",
                operator = "{{#request.operator}}",
                module = "p1",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                success = "platform-fallback"
        )
        public void platformFallbackOnly(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p1",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                extra = "{{#request.address}}",
                success = "extra-resolved"
        )
        public void extraResolved(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p1",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                extra = "",
                success = "extra-empty"
        )
        public void extraEmpty(ScenarioRequest request) {
        }

        @EasyLogs({
                @EasyLog(
                        module = "user-cond",
                        type = "TRACE",
                        bizNo = "{{#request.bizNo}}",
                        success = "cond-true",
                        condition = "{{true}}"
                ),
                @EasyLog(
                        module = "audit-cond",
                        type = "TRACE",
                        bizNo = "{{#request.bizNo}}",
                        success = "cond-false",
                        condition = "{{false}}"
                )
        })
        public void multiWithCondition(ScenarioRequest request) {
        }

        @EasyLogs({
                @EasyLog(
                        module = "mfail1",
                        type = "TRACE",
                        bizNo = "{{#request.bizNo}}",
                        success = "ok1",
                        fail = "fail-one {{#_errMsg}}"
                ),
                @EasyLog(
                        module = "mfail2",
                        type = "TRACE",
                        bizNo = "{{#request.bizNo}}",
                        success = "ok2",
                        fail = "fail-two {{#_errMsg}}"
                )
        })
        public void multiFail(ScenarioRequest request) {
            throw new IllegalStateException("boom-multi");
        }

        @EasyLog(
                module = "p2",
                type = "READ",
                bizNo = "{{null}}",
                success = "biz-null"
        )
        public void bizNoNull(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p2",
                type = "READ",
                bizNo = "{{#request.bizNo}}",
                success = "",
                successParamList = {"{{#request.operator}}"}
        )
        public void successEmptyWithParams(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p2",
                type = "READ",
                bizNo = "{{#request.bizNo}}",
                success = "err {{#_errMsg}}"
        )
        public void successUsesErrMsg(ScenarioRequest request) {
        }

        @EasyLog(
                module = "p2",
                type = "READ",
                bizNo = "{{#request.bizNo}}",
                success = "{{NOT_REGISTERED(#request.bizNo)}}"
        )
        public void unknownFunctionInTemplate(ScenarioRequest request) {
        }
    }

    static class ScenarioRequest {
        private String operator;
        private String bizNo;
        private String platform;
        private String address;
        private boolean enabled = true;
        private String labelId;

        static ScenarioRequest base() {
            ScenarioRequest request = new ScenarioRequest();
            request.operator = "alice";
            request.bizNo = "B-1";
            request.platform = "web";
            request.address = "road-0";
            request.labelId = "L-0";
            return request;
        }

        ScenarioRequest withOperator(String operator) {
            this.operator = operator;
            return this;
        }

        ScenarioRequest withBizNo(String bizNo) {
            this.bizNo = bizNo;
            return this;
        }

        ScenarioRequest withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        ScenarioRequest withAddress(String address) {
            this.address = address;
            return this;
        }

        ScenarioRequest withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        ScenarioRequest withLabelId(String labelId) {
            this.labelId = labelId;
            return this;
        }

        public String getOperator() {
            return operator;
        }

        public String getBizNo() {
            return bizNo;
        }

        public String getPlatform() {
            return platform;
        }

        public String getAddress() {
            return address;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getLabelId() {
            return labelId;
        }
    }

    @EasyLogDiffObject(alias = "user")
    static class DiffUser {
        @EasyLogDiffField(alias = "年龄")
        private final int age;
        private final String name;

        DiffUser(int age, String name) {
            this.age = age;
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public String getName() {
            return name;
        }
    }

    static class ScenarioStore {
        private final Map<String, String> values = new ConcurrentHashMap<>();

        void put(String key, String value) {
            values.put(key, value);
        }

        String get(String key) {
            return values.get(key);
        }

        void clear() {
            values.clear();
        }
    }

    static class LabelService {
        public String label(String id) {
            return "label-" + id;
        }
    }

    static class FixedOperatorService implements IOperatorService {
        @Override
        public String getOperator() {
            return "default-op";
        }

        @Override
        public String getPlatform() {
            return "default-plat";
        }
    }

    static class CapturingLogRecordService implements ILogRecordService {
        private final CopyOnWriteArrayList<EasyLogInfo> logs = new CopyOnWriteArrayList<>();

        @Override
        public void record(EasyLogInfo easyLogInfo) {
            String resolved = PlaceholderResolver.getDefaultResolver()
                    .resolve(easyLogInfo.getContent(), easyLogInfo.getContentParam());
            easyLogInfo.setContent(resolved);
            logs.add(easyLogInfo);
        }

        void clear() {
            logs.clear();
        }

        List<EasyLogInfo> snapshot() {
            return new ArrayList<>(logs);
        }
    }
}
