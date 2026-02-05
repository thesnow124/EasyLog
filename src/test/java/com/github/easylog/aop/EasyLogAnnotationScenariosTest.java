package com.github.easylog.aop;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.annotation.EasyLogs;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.context.EasyLogContext;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.function.ParseFunction;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @DisplayName("Functions and Beans")
    class FunctionsAndBeans {
        @Test
        void resolves_custom_function() {
            ScenarioRequest request = ScenarioRequest.base().withOperator("alice");

            scenarioService.upper(request);

            EasyLogInfo info = singleLog();
            assertEquals("upper ALICE", info.getContent());
        }

        @Test
        void uses_before_function_value() {
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
    @DisplayName("Before/After Diff")
    class BeforeAfterDiff {
        @Test
        void builds_field_info_list_from_before_after_diff() {
            ScenarioRequest request = ScenarioRequest.base()
                    .withOldNew("{\"age\":1}", "{\"age\":2}");

            scenarioService.diff(request);

            EasyLogInfo info = singleLog();
            assertEquals("{\"age\":1}", info.getBefore());
            assertEquals("{\"age\":2}", info.getAfter());
            List<FieldInfo> fields = info.getFieldInfoList();
            assertEquals(1, fields.size());
            assertTrue(fields.get(0).getFieldName().contains("age"));
            assertEquals("1", fields.get(0).getOldFieldVal());
            assertEquals("2", fields.get(0).getNewFieldVal());
        }

        @Test
        void builds_field_info_list_from_before_after_object() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-201");

            scenarioService.diffObject(request);

            EasyLogInfo info = singleLog();
            assertNotNull(info.getBefore());
            assertNotNull(info.getAfter());
            List<FieldInfo> fields = info.getFieldInfoList();
            assertEquals(1, fields.size());
            assertTrue(fields.get(0).getFieldName().contains("age"));
            assertEquals("1", fields.get(0).getOldFieldVal());
            assertEquals("2", fields.get(0).getNewFieldVal());
        }

        @Test
        void builds_field_info_list_from_array_before_after() {
            ScenarioRequest request = ScenarioRequest.base().withOldNew("[1,2]", "[2,3,4]");

            scenarioService.diff(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> "1".equals(f.getOldFieldVal()) && "2".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "2".equals(f.getOldFieldVal()) && "3".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "".equals(f.getOldFieldVal()) && "4".equals(f.getNewFieldVal())));
        }

        @Test
        void builds_field_info_list_from_scalar_before_after() {
            ScenarioRequest request = ScenarioRequest.base().withOldNew("1", "2");

            scenarioService.diff(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> "1".equals(f.getOldFieldVal()) && "2".equals(f.getNewFieldVal())));
        }

        @Test
        void falls_back_for_non_json_before_after() {
            ScenarioRequest request = ScenarioRequest.base().withOldNew("plain", "next");

            scenarioService.diff(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertEquals(1, fields.size());
            assertEquals("plain", fields.get(0).getOldFieldVal());
            assertEquals("next", fields.get(0).getNewFieldVal());
        }

        @Test
        void builds_field_info_list_from_reference_object() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-202");

            scenarioService.diffRefObject(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> f.getFieldName() != null
                    && f.getFieldName().contains("detail")
                    && f.getOldFieldVal() != null
                    && f.getOldFieldVal().contains("old")
                    && f.getNewFieldVal() != null
                    && f.getNewFieldVal().contains("new")));
        }

        @Test
        void builds_field_info_list_from_map_entry_changes() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-203");

            scenarioService.diffMap(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> "1".equals(f.getOldFieldVal()) && "".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "2".equals(f.getOldFieldVal()) && "3".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "".equals(f.getOldFieldVal()) && "4".equals(f.getNewFieldVal())));
        }

        @Test
        void builds_field_info_list_from_list_element_change_and_add() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-204");

            scenarioService.diffListAdd(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> "2".equals(f.getOldFieldVal()) && "3".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "".equals(f.getOldFieldVal()) && "4".equals(f.getNewFieldVal())));
        }

        @Test
        void builds_field_info_list_from_list_element_remove() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-205");

            scenarioService.diffListRemove(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> "2".equals(f.getOldFieldVal()) && "".equals(f.getNewFieldVal())));
        }

        @Test
        void builds_field_info_list_from_complex_object_fields() {
            ScenarioRequest request = ScenarioRequest.base().withBizNo("B-206");

            scenarioService.diffAllFields(request);

            EasyLogInfo info = singleLog();
            List<FieldInfo> fields = info.getFieldInfoList();
            assertTrue(fields.stream().anyMatch(f -> "b".equals(f.getOldFieldVal()) && "c".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "v1".equals(f.getOldFieldVal()) && "".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "v2".equals(f.getOldFieldVal()) && "v3".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> "".equals(f.getOldFieldVal()) && "v4".equals(f.getNewFieldVal())));
            assertTrue(fields.stream().anyMatch(f -> f.getFieldName() != null && f.getFieldName().contains("objectList")));
            assertTrue(fields.stream().anyMatch(f -> f.getFieldName() != null && f.getFieldName().contains("child")));
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
        ParseFunction upperFunction() {
            return new UpperFunction();
        }

        @Bean
        ParseFunction oldValueFunction(ScenarioStore scenarioStore) {
            return new OldValueFunction(scenarioStore);
        }

        @Bean
        ParseFunctionFactory parseFunctionFactory(List<ParseFunction> parseFunctions) {
            return new ParseFunctionFactory(parseFunctions);
        }

        @Bean
        EasyLogParser easyLogParser(ParseFunctionFactory parseFunctionFactory) {
            return new EasyLogParser(parseFunctionFactory);
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
                bizNo = "{{#request.bizNo}}",
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
                bizNo = "{{#request.bizNo}}",
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
                before = "{{#request.oldJson}}",
                after = "{{#request.newJson}}",
                success = "diff"
        )
        public void diff(ScenarioRequest request) {
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                before = "{{#oldObj}}",
                after = "{{#newObj}}",
                success = "diff-obj"
        )
        public void diffObject(ScenarioRequest request) {
            Map<String, Object> oldObj = new HashMap<>();
            oldObj.put("age", 1);
            Map<String, Object> newObj = new HashMap<>();
            newObj.put("age", 2);
            EasyLogContext.put("oldObj", oldObj);
            EasyLogContext.put("newObj", newObj);
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                before = "{{#oldRef}}",
                after = "{{#newRef}}",
                success = "diff-ref"
        )
        public void diffRefObject(ScenarioRequest request) {
            EasyLogContext.put("oldRef", new RefObject("old"));
            EasyLogContext.put("newRef", new RefObject("new"));
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                before = "{{#oldMap}}",
                after = "{{#newMap}}",
                success = "diff-map"
        )
        public void diffMap(ScenarioRequest request) {
            Map<String, Object> oldMap = new HashMap<>();
            oldMap.put("a", 1);
            oldMap.put("b", 2);
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("b", 3);
            newMap.put("c", 4);
            EasyLogContext.put("oldMap", oldMap);
            EasyLogContext.put("newMap", newMap);
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                before = "{{#oldList}}",
                after = "{{#newList}}",
                success = "diff-list-add"
        )
        public void diffListAdd(ScenarioRequest request) {
            List<Integer> oldList = new ArrayList<>();
            oldList.add(1);
            oldList.add(2);
            List<Integer> newList = new ArrayList<>();
            newList.add(1);
            newList.add(3);
            newList.add(4);
            EasyLogContext.put("oldList", oldList);
            EasyLogContext.put("newList", newList);
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                before = "{{#oldList}}",
                after = "{{#newList}}",
                success = "diff-list-remove"
        )
        public void diffListRemove(ScenarioRequest request) {
            List<Integer> oldList = new ArrayList<>();
            oldList.add(1);
            oldList.add(2);
            List<Integer> newList = new ArrayList<>();
            newList.add(1);
            EasyLogContext.put("oldList", oldList);
            EasyLogContext.put("newList", newList);
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                before = "{{#oldAll}}",
                after = "{{#newAll}}",
                success = "diff-all"
        )
        public void diffAllFields(ScenarioRequest request) {
            HashMap<String, Object> oldChildAttrs = new HashMap<>();
            oldChildAttrs.put("ck", "cv1");
            AllFieldObjects oldChild = new AllFieldObjects(
                    new ArrayList<>(Arrays.asList("c1", "c2")),
                    new ArrayList<>(),
                    null,
                    oldChildAttrs
            );

            HashMap<String, Object> newChildAttrs = new HashMap<>();
            newChildAttrs.put("ck", "cv2");
            AllFieldObjects newChild = new AllFieldObjects(
                    new ArrayList<>(Arrays.asList("c1", "c3")),
                    new ArrayList<>(),
                    null,
                    newChildAttrs
            );

            HashMap<String, Object> oldAttrs = new HashMap<>();
            oldAttrs.put("k1", "v1");
            oldAttrs.put("k2", "v2");
            AllFieldObjects oldAll = new AllFieldObjects(
                    new ArrayList<>(Arrays.asList("a", "b")),
                    new ArrayList<>(Arrays.asList(oldChild)),
                    oldChild,
                    oldAttrs
            );

            HashMap<String, Object> newAttrs = new HashMap<>();
            newAttrs.put("k2", "v3");
            newAttrs.put("k3", "v4");
            AllFieldObjects newAll = new AllFieldObjects(
                    new ArrayList<>(Arrays.asList("a", "c", "d")),
                    new ArrayList<>(Arrays.asList(newChild, oldChild)),
                    newChild,
                    newAttrs
            );

            EasyLogContext.put("oldAll", oldAll);
            EasyLogContext.put("newAll", newAll);
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
                type = "READ",
                bizNo = "{{#request.bizNo}}",
                success = "upper {upper{#request.operator}}"
        )
        public void upper(ScenarioRequest request) {
        }

        @EasyLog(
                module = "user",
                type = "UPDATE",
                bizNo = "{{#request.bizNo}}",
                success = "old {oldValue{#request.bizNo}} new {{#request.address}}"
        )
        public void updateAddress(ScenarioRequest request) {
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
    }

    static class ScenarioRequest {
        private String operator;
        private String bizNo;
        private String platform;
        private String address;
        private boolean enabled = true;
        private String oldJson;
        private String newJson;
        private String labelId;

        static ScenarioRequest base() {
            ScenarioRequest request = new ScenarioRequest();
            request.operator = "alice";
            request.bizNo = "B-1";
            request.platform = "web";
            request.address = "road-0";
            request.labelId = "L-0";
            request.oldJson = "{\"age\":1}";
            request.newJson = "{\"age\":2}";
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

        ScenarioRequest withOldNew(String oldJson, String newJson) {
            this.oldJson = oldJson;
            this.newJson = newJson;
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

        public String getOldJson() {
            return oldJson;
        }

        public String getNewJson() {
            return newJson;
        }

        public String getLabelId() {
            return labelId;
        }
    }

    static class RefObject {
        private final RefDetail detail;

        RefObject(String code) {
            this.detail = new RefDetail(code);
        }

        public RefDetail getDetail() {
            return detail;
        }
    }

    static class RefDetail {
        private final String code;

        RefDetail(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    static class AllFieldObjects {
        private final List<String> stringList;
        private final List<AllFieldObjects> objectList;
        private final AllFieldObjects child;
        private final HashMap<String, Object> attributes;

        AllFieldObjects(List<String> stringList,
                        List<AllFieldObjects> objectList,
                        AllFieldObjects child,
                        HashMap<String, Object> attributes) {
            this.stringList = stringList;
            this.objectList = objectList;
            this.child = child;
            this.attributes = attributes;
        }

        public List<String> getStringList() {
            return stringList;
        }

        public List<AllFieldObjects> getObjectList() {
            return objectList;
        }

        public AllFieldObjects getChild() {
            return child;
        }

        public HashMap<String, Object> getAttributes() {
            return attributes;
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

    static class UpperFunction implements ParseFunction {
        @Override
        public String functionName() {
            return "upper";
        }

        @Override
        public String apply(String value) {
            return value == null ? "" : value.toUpperCase();
        }
    }

    static class OldValueFunction implements ParseFunction {
        private final ScenarioStore store;

        OldValueFunction(ScenarioStore store) {
            this.store = store;
        }

        @Override
        public String functionName() {
            return "oldValue";
        }

        @Override
        public String apply(String value) {
            String oldValue = store.get(value);
            return oldValue == null ? "" : oldValue;
        }

        @Override
        public boolean executeBefore() {
            return true;
        }
    }
}
