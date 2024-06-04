package com.github.easylog.compare;


import lombok.Data;


/**
 * @author Gaosl
 */
@Data
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
}
