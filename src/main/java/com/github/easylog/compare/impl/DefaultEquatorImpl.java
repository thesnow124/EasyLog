package com.github.easylog.compare.impl;

import com.github.easylog.compare.Equator;
import com.github.easylog.compare.FieldInfo;

import java.util.Collections;
import java.util.List;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 默认比较器实现
 * @date 2024/5/10 09:46:34
 */
public class DefaultEquatorImpl  implements Equator {

    @Override
    public List<FieldInfo> getDiffField(Object oldObj, Object newObj) {
//        BeanDiff beanDiff = new BeanDiff();
//        try {
//            if (oldBean != null && newBean != null) {
//                Class oldBeanClazz = oldBean.getClass();
//                Class newBeanClazz = newBean.getClass();
//                if (!oldBeanClazz.equals(newBeanClazz)) {
//                    throw new IllegalArgumentException("The objects being compared must be of the same class");
//                }
//                List<Field> fields = getCompareFields(newBeanClazz);
//                for (Field field : fields) {
//                    field.setAccessible(true);
//                    Object oldValue = field.get(oldBean);
//                    Object newValue = field.get(newBean);
//                    FieldAlias alias = field.getAnnotation(FieldAlias.class);
//                    if (!nullableEquals(oldValue, newValue)) {
//                        FieldDiff fieldDiff = new FieldDiff();
//                        fieldDiff.setAttributeAlias(alias.value());
//                        fieldDiff.setAttributeName(field.getName());
//                        fieldDiff.setAttributeType(field.getType().getTypeName());
//                        fieldDiff.setNewValue(JSON.toJSONString(newValue));
//                        fieldDiff.setOldValue(JSON.toJSONString(oldValue));
//                        beanDiff.addFieldDiff(fieldDiff);
//                    }
//                }
//            }
//        } catch (Exception exception) {
//            log.error("", exception);
//        }
//        if (CollectionUtils.isNotEmpty(beanDiff.getFieldDiffs())) {
//            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//            request.setAttribute("beanDiff", beanDiff);
//        }
//        return beanDiff;
        return Collections.emptyList();
    }
}
