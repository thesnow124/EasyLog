package com.github.easylog.aop;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyLogAspectHelperTest {

    @EasyLog(
            platform = "platform",
            operator = "operator",
            module = "module",
            type = "type",
            bizNo = "biz",
            success = "success",
            successParamList = {"sp1"},
            fail = "fail",
            failParamList = {"fp1"},
            detail = "detail",
            condition = "cond"
    )
    void annotatedMethod() {}

    static class SimpleMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final byte[] content;

        SimpleMultipartFile(String name, String originalFilename, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) {
            throw new UnsupportedOperationException("not needed in tests");
        }
    }

    @Test
    void parseLogAnnotationAndCollectTemplates() throws Exception {
        Method method = EasyLogAspectHelperTest.class.getDeclaredMethod("annotatedMethod");
        EasyLog annotation = method.getAnnotation(EasyLog.class);
        EasyLogOps ops = EasyLogAspectHelper.parseLogAnnotation(annotation);

        assertEquals("platform", ops.getPlatform());
        assertEquals("operator", ops.getOperator());
        assertEquals("module", ops.getModule());
        assertEquals("type", ops.getType());
        assertEquals("biz", ops.getBizNo());
        assertEquals("success", ops.getSuccess());
        assertEquals("fail", ops.getFail());
        assertEquals("detail", ops.getDetails());
        assertEquals("cond", ops.getCondition());

        List<String> templates = EasyLogAspectHelper.getExpressTemplate(Collections.singletonList(ops));
        assertTrue(templates.contains("biz"));
        assertTrue(templates.contains("success"));
        assertTrue(templates.contains("fail"));
        assertTrue(templates.contains("detail"));
        assertTrue(templates.contains("cond"));
        assertTrue(templates.contains("sp1"));
        assertTrue(templates.contains("fp1"));
    }

    @Test
    void buildRequestParamAndExtractClientIp() {
        MultipartFile file = new SimpleMultipartFile("file", "note.txt", "data".getBytes(StandardCharsets.UTF_8));
        Map<String, String> forwarded = new HashMap<>();
        forwarded.put("X-Forwarded-For", "1.1.1.1, 2.2.2.2");
        HttpServletRequest request = requestWithHeaders(forwarded, "3.3.3.3");
        Map<String, Object> params = EasyLogAspectHelper.buildRequestParam(
                new String[]{"file", "req", "count"},
                new Object[]{file, request, 2});

        assertEquals("note.txt", params.get("file"));
        assertEquals("HttpServletRequest can not serializable", params.get("req"));
        assertEquals(2, params.get("count"));

        assertEquals("1.1.1.1", EasyLogAspectHelper.extractClientIp(request));
        Map<String, String> realIpHeader = new HashMap<>();
        realIpHeader.put("X-Real-IP", "9.9.9.9");
        HttpServletRequest realIp = requestWithHeaders(realIpHeader, "3.3.3.3");
        assertEquals("9.9.9.9", EasyLogAspectHelper.extractClientIp(realIp));
        HttpServletRequest fallback = requestWithHeaders(Collections.emptyMap(), "7.7.7.7");
        assertEquals("7.7.7.7", EasyLogAspectHelper.extractClientIp(fallback));
    }

    @Test
    void createEasyLogInfoBuildsSuccessLog() {
        EasyLogOps ops = new EasyLogOps();
        ops.setBizNo("bizKey");
        ops.setDetails("detailKey");
        ops.setOperator("opKey");
        ops.setPlatform("platKey");
        ops.setSuccess("successKey");
        ops.setSuccessParamList(new String[]{"param1", "missing"});
        ops.setFail("failKey");
        ops.setFailParamList(new String[]{"failParam"});
        ops.setModule("m");
        ops.setType("t");
        ops.setCondition("condKey");

        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("bizKey", "BIZ-1");
        templateMap.put("detailKey", "[{\"a\":1},{\"a\":2}]");
        templateMap.put("successKey", "success-msg");
        templateMap.put("param1", "P1");
        templateMap.put("condKey", "true");

        MethodExecuteResult executeResult = new MethodExecuteResult(true);
        IOperatorService operatorService = new FixedOperatorService();

        List<EasyLogInfo> infos = EasyLogAspectHelper.createEasyLogInfo(
                templateMap, Collections.singletonList(ops), executeResult, operatorService);

        assertEquals(1, infos.size());
        EasyLogInfo info = infos.get(0);
        assertEquals("BIZ-1", info.getBizNo());
        assertEquals("success-msg", info.getContent());
        assertEquals("P1", info.getContentParam()[0]);
        assertEquals("", info.getContentParam()[1]);
        assertEquals("fixed-op", info.getOperator());
        assertEquals("fixed-plat", info.getPlatform());
        assertEquals("true", info.getCondition());
        assertNotNull(info.getFieldInfoList());
        assertFalse(info.getFieldInfoList().isEmpty());
    }

    @Test
    void createEasyLogInfoSkipsWhenConditionFalse() {
        EasyLogOps ops = new EasyLogOps();
        ops.setBizNo("bizKey");
        ops.setSuccess("successKey");
        ops.setCondition("condKey");

        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("bizKey", "BIZ-1");
        templateMap.put("successKey", "success-msg");
        templateMap.put("condKey", "false");

        List<EasyLogInfo> infos = EasyLogAspectHelper.createEasyLogInfo(
                templateMap, Collections.singletonList(ops), new MethodExecuteResult(true), new FixedOperatorService());
        assertTrue(infos.isEmpty());
    }

    @Test
    void createEasyLogInfoUsesFailTemplateAndFallbackDetail() {
        EasyLogOps ops = new EasyLogOps();
        ops.setBizNo("bizKey");
        ops.setDetails("detailKey");
        ops.setFail("failKey");
        ops.setFailParamList(new String[]{"failParam"});

        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("bizKey", "BIZ-1");
        templateMap.put("detailKey", "not-json");
        templateMap.put("failKey", "fail-msg");
        templateMap.put("failParam", "F1");

        MethodExecuteResult executeResult = new MethodExecuteResult(true);
        executeResult.exception(new IllegalStateException("boom"));

        List<EasyLogInfo> infos = EasyLogAspectHelper.createEasyLogInfo(
                templateMap, Collections.singletonList(ops), executeResult, new FixedOperatorService());

        assertEquals(1, infos.size());
        EasyLogInfo info = infos.get(0);
        assertEquals("fail-msg", info.getContent());
        assertEquals("F1", info.getContentParam()[0]);
        assertNotNull(info.getFieldInfoList());
        assertEquals("not-json", info.getFieldInfoList().get(0).getVal());
    }

    private static HttpServletRequest requestWithHeaders(Map<String, String> headers, String remoteAddr) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName())) {
                        return headers.get(String.valueOf(args[0]));
                    }
                    if ("getRemoteAddr".equals(method.getName())) {
                        return remoteAddr;
                    }
                    return null;
                }
        );
    }

    static class FixedOperatorService implements IOperatorService {
        @Override
        public String getOperator() {
            return "fixed-op";
        }

        @Override
        public String getPlatform() {
            return "fixed-plat";
        }
    }
}
