package com.github.easylog.diff;

import java.util.List;

/**
 * Diff结果实体
 * @author gaoshuanglong
 */
public class DiffDTO {

    /**
     * 实体类名
     */
    private String oldClassName;

    /**
     * 实体类别名
     */
    private String oldClassAlias;

    /**
     * 实体类名
     */
    private String newClassName;

    /**
     * 实体类别名
     */
    private String newClassAlias;

    /**
     * 字段Diff列表
     */
    private List<DiffFieldDTO> diffFieldDTOList;

    public String getOldClassName() {
        return oldClassName;
    }

    public void setOldClassName(String oldClassName) {
        this.oldClassName = oldClassName;
    }

    public String getOldClassAlias() {
        return oldClassAlias;
    }

    public void setOldClassAlias(String oldClassAlias) {
        this.oldClassAlias = oldClassAlias;
    }

    public String getNewClassName() {
        return newClassName;
    }

    public void setNewClassName(String newClassName) {
        this.newClassName = newClassName;
    }

    public String getNewClassAlias() {
        return newClassAlias;
    }

    public void setNewClassAlias(String newClassAlias) {
        this.newClassAlias = newClassAlias;
    }

    public List<DiffFieldDTO> getDiffFieldDTOList() {
        return diffFieldDTOList;
    }

    public void setDiffFieldDTOList(List<DiffFieldDTO> diffFieldDTOList) {
        this.diffFieldDTOList = diffFieldDTOList;
    }
}
