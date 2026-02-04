package com.github.easylog.compare;


/**
 * @author Gaosl
 */
public class FieldInfo {

    /**
     * 属性名
     */
    private String fieldName;

    /**
     * 老的属性值
     */
    private String oldFieldVal;

    /**
     * 新的属性值
     */
    private String newFieldVal;


    /**
     * 详情属性值
     */
    private String val;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldFieldVal() {
        return oldFieldVal;
    }

    public void setOldFieldVal(String oldFieldVal) {
        this.oldFieldVal = oldFieldVal;
    }

    public String getNewFieldVal() {
        return newFieldVal;
    }

    public void setNewFieldVal(String newFieldVal) {
        this.newFieldVal = newFieldVal;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
