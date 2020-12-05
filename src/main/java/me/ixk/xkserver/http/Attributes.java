/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

/**
 * @author Otstar Lin
 * @date 2020/11/28 下午 2:54
 */
public interface Attributes {
    /**
     * 删除属性
     *
     * @param name 属性名
     */
    void removeAttribute(String name);

    /**
     * 设置属性
     *
     * @param name      属性名
     * @param attribute 属性值
     */
    void setAttribute(String name, Object attribute);

    /**
     * 获取属性
     *
     * @param name 属性名
     *
     * @return 属性值
     */
    Object getAttribute(String name);

    /**
     * 获取所有属性名
     *
     * @return 所有属性名
     */
    Set<String> getAttributeNameSet();

    /**
     * 获取所有属性名
     *
     * @return 所有属性名
     */
    default Enumeration<String> getAttributeNames() {
        return Collections.enumeration(getAttributeNameSet());
    }

    /**
     * 清空所有属性
     */
    void clearAttributes();
}
