package com.github.easylog.diff;

/**
 * DIFF字段实体
 * @author gaoshuanglong
 */
public class DiffFieldDTO {

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 字段别名
     */
    private String oldFieldAlias;

    /**
     * 字段别名
     */
    private String newFieldAlias;

    /**
     * 旧值
     */
    private Object oldValue;

    /**
     * 新值
     */
    private Object newValue;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldFieldAlias() {
        return oldFieldAlias;
    }

    public void setOldFieldAlias(String oldFieldAlias) {
        this.oldFieldAlias = oldFieldAlias;
    }

    public String getNewFieldAlias() {
        return newFieldAlias;
    }

    public void setNewFieldAlias(String newFieldAlias) {
        this.newFieldAlias = newFieldAlias;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
}
