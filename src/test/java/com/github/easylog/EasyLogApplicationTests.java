package com.github.easylog;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.compare.Equator;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.constants.OperateType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

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

    @Test
    public void a(){
        String old="{\n" +
                "    \"purchasePaymentRequestCode\": \"PY20240604008\",\n" +
                "    \"requestDate\": \"2024-06-04 00:00:00\",\n" +
                "    \"paymentOrgId\": \"10\",\n" +
                "    \"invoiceNumber\": null,\n" +
                "    \"bizOccurrenceEndTime\": \"2024-06-03\",\n" +
                "    \"bizOccurrenceStartTime\": \"2024-06-03\",\n" +
                "    \"businessOccurrenceTimeRange\": \"2024-06-032024-06-03\",\n" +
                "    \"remarks\": \"自动化生成v24\",\n" +
                "    \"details\": [\n" +
                "        {\n" +
                "            \"purchasePaymentRequestCode\": \"PY20240604008\",\n" +
                "            \"purchaseItemCode\": \"A000001\",\n" +
                "            \"itemName\": \"A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述\",\n" +
                "            \"type\": \"Asset\",\n" +
                "            \"typeDesc\": \"Asset\",\n" +
                "            \"unit\": \"Trip\",\n" +
                "            \"purchaseQty\": 9,\n" +
                "            \"purchaseAmount\": 90,\n" +
                "            \"taxInclusivePurchaseAmount\": 94.5,\n" +
                "            \"cumulativeReceiptQty\": null,\n" +
                "            \"cumulativeReceiptAmount\": null,\n" +
                "            \"cumulativeTaxInclusiveReceiptAmount\": null,\n" +
                "            \"settlementCurrency\": \"USD\",\n" +
                "            \"price\": \"10\",\n" +
                "            \"taxInclusivePrice\": \"10.5\",\n" +
                "            \"receiptQty\": null,\n" +
                "            \"receiptAmount\": null,\n" +
                "            \"taxInclusiveReceiptAmount\": null,\n" +
                "            \"taxRate\": 5,\n" +
                "            \"remarks\": null\n" +
                "        }\n" +
                "    ],\n" +
                "    \"settlementMethod\": \"JSFS04_SYS\",\n" +
                "    \"settlementCurrency\": \"USD\",\n" +
                "    \"invoiceFiles\": [\n" +
                "        {\n" +
                "            \"fileName\": \"default_upload.png\",\n" +
                "            \"filePath\": \"hermes/ent/image/default/2024/6/202406041221678785821356032.png\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"allocationDetails\": [\n" +
                "        {\n" +
                "            \"purchasePaymentRequestCode\": \"PY20240604008\",\n" +
                "            \"id\": \"1221678786169356288\",\n" +
                "            \"country\": \"UAE\",\n" +
                "            \"subsidiaryId\": null,\n" +
                "            \"subsidiaryName\": null,\n" +
                "            \"deptId\": \"1034600\",\n" +
                "            \"deptName\": \"Commercial Solution Dep.\",\n" +
                "            \"stationCode\": \"S2102353\",\n" +
                "            \"stationName\": \"Dubai Return Center\",\n" +
                "            \"feeType\": \"CI026\",\n" +
                "            \"feeTypeDesc\": \"Employee welfare\",\n" +
                "            \"feeBearingItemCode\": \"XM001\",\n" +
                "            \"feeBearingItemName\": \"UAE-Amazon EDS Abu Dhabi\",\n" +
                "            \"feeBearingAmount\": 47.25,\n" +
                "            \"feeBearingPercentage\": 100\n" +
                "        }\n" +
                "    ],\n" +
                "    \"requiredPayment\": 47.25,\n" +
                "    \"dueDate\": \"2024-07-03\"\n" +
                "}";
        String newStr="{\n" +
                "    \"purchasePaymentRequestCode\": \"PY20240604008\",\n" +
                "    \"requestDate\": \"2024-06-04 00:00:00\",\n" +
                "    \"paymentOrgId\": \"10\",\n" +
                "    \"invoiceNumber\": null,\n" +
                "    \"bizOccurrenceEndTime\": \"2024-06-03\",\n" +
                "    \"bizOccurrenceStartTime\": \"2024-06-03\",\n" +
                "    \"businessOccurrenceTimeRange\": \"2024-06-032024-06-03\",\n" +
                "    \"remarks\": \"自动化生成v25\",\n" +
                "    \"details\": [\n" +
                "        {\n" +
                "            \"purchasePaymentRequestCode\": \"PY20240604008\",\n" +
                "            \"purchaseItemCode\": \"A000001\",\n" +
                "            \"itemName\": \"A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述A00001的描述\",\n" +
                "            \"type\": \"Asset\",\n" +
                "            \"typeDesc\": \"Asset\",\n" +
                "            \"unit\": \"Trip\",\n" +
                "            \"purchaseQty\": 9,\n" +
                "            \"purchaseAmount\": 90,\n" +
                "            \"taxInclusivePurchaseAmount\": 94.5,\n" +
                "            \"cumulativeReceiptQty\": null,\n" +
                "            \"cumulativeReceiptAmount\": null,\n" +
                "            \"cumulativeTaxInclusiveReceiptAmount\": null,\n" +
                "            \"settlementCurrency\": \"USD\",\n" +
                "            \"price\": \"10\",\n" +
                "            \"taxInclusivePrice\": \"10.5\",\n" +
                "            \"receiptQty\": null,\n" +
                "            \"receiptAmount\": null,\n" +
                "            \"taxInclusiveReceiptAmount\": null,\n" +
                "            \"taxRate\": 5,\n" +
                "            \"remarks\": null\n" +
                "        }\n" +
                "    ],\n" +
                "    \"settlementMethod\": \"JSFS04_SYS\",\n" +
                "    \"settlementCurrency\": \"USD\",\n" +
                "    \"invoiceFiles\": [\n" +
                "        {\n" +
                "            \"fileName\": \"default_uploa.png\",\n" +
                "            \"filePath\": \"hermes/ent/image/default/2024/6/202406041221678785821356032.png\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"allocationDetails\": [\n" +
                "        {\n" +
                "            \"purchasePaymentRequestCode\": \"PY20240604008\",\n" +
                "            \"id\": \"1221678786169356288\",\n" +
                "            \"country\": \"UAE\",\n" +
                "            \"subsidiaryId\": null,\n" +
                "            \"subsidiaryName\": null,\n" +
                "            \"deptId\": \"1034600\",\n" +
                "            \"deptName\": \"Commercial Solution Dep.\",\n" +
                "            \"stationCode\": \"S2102353\",\n" +
                "            \"stationName\": \"Dubai Return Center\",\n" +
                "            \"feeType\": \"CI026\",\n" +
                "            \"feeTypeDesc\": \"Employee welfare\",\n" +
                "            \"feeBearingItemCode\": \"XM001\",\n" +
                "            \"feeBearingItemName\": \"UAE-Amazon EDSP Abu Dhabi\",\n" +
                "            \"feeBearingAmount\": 47.25,\n" +
                "            \"feeBearingPercentage\": 100\n" +
                "        }\n" +
                "    ],\n" +
                "    \"requiredPayment\": 47.25,\n" +
                "    \"dueDate\": \"2024-07-03\"\n" +
                "}";
        String old1="{}";
        List<FieldInfo> diffField = Equator.getDiffField(old1, newStr);
        System.out.println(diffField);
    }



}
