package com.fun.common.utils;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * spring工具类
 * 方便在非spring管理环境中获取bean
 *
 * @author DJun
 */
@Component
public final class SpringUtils implements BeanFactoryPostProcessor {
    /**
     * Spring应用上下文环境
     */
    private static ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringUtils.beanFactory = beanFactory;
    }

    /**
     * 获取对象
     *
     * @param name BeanName
     * @return Object 一个以所给名字注册的bean的实例
     * @throws BeansException
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T) beanFactory.getBean(name);
    }

    /**
     * 获取类型为requiredType的对象
     *
     * @param clz Class
     * @return Bean
     */
    public static <T> T getBean(Class<T> clz) throws BeansException {
        return (T) beanFactory.getBean(clz);
    }

    /**
     * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
     *
     * @param name
     * @return boolean
     */
    public static boolean containsBean(String name) {
        return beanFactory.containsBean(name);
    }

    /**
     * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
     *
     * @param name
     * @return boolean
     * @throws NoSuchBeanDefinitionException
     */
    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return beanFactory.isSingleton(name);
    }

    /**
     * @param name 注册对象名称
     * @return Class 注册对象的类型
     */
    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return beanFactory.getType(name);
    }

    /**
     * 如果给定的 bean 名字在 bean 定义中有别名，则返回这些别名
     *
     * @param name BeanName
     */
    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
        return beanFactory.getAliases(name);
    }

    /**
     * 获取aop代理对象
     *
     * @param invoker 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker) {
        return (T) AopContext.currentProxy();
    }
}
