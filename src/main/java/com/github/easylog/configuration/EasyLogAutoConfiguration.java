package com.github.easylog.configuration;


import com.github.easylog.aop.EasyLogAspect;
import com.github.easylog.diff.DefaultDiffEngine;
import com.github.easylog.function.DefaultFunctionServiceImpl;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.function.IFunctionService;
import com.github.easylog.function.IParseFunction;
import com.github.easylog.function.ParseFunctionFactory;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.support.DefaultLogRecordServiceImpl;
import com.github.easylog.support.DefaultOperatorServiceImpl;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;


/**
 * Auto-configuration entry for the EasyLog SDK.
 * <p>
 * This class wires the following pieces when {@code easylog.enable=true} (default):
 * <ul>
 *     <li>Template parsing infrastructure ({@link com.github.easylog.function.EasyLogParser}).</li>
 *     <li>Default operator/provider beans that can be overridden by user beans.</li>
 *     <li>Log storage selection：in-memory log printing by default; override via {@link com.github.easylog.service.ILogRecordService}.</li>
 *     <li>AOP aspect that captures method invocations and renders operation logs.</li>
 * </ul>
 * All beans are defined with {@code @ConditionalOnMissingBean} so business projects can provide their own implementations
 * without touching SDK code.
 *
 * @author Gaosl
 */
@Configuration
@ConditionalOnProperty(prefix = "easylog", name = "enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({EasyLogProperties.class})
public class EasyLogAutoConfiguration {

    private final EasyLogProperties easyLogProperties;

    public EasyLogAutoConfiguration(EasyLogProperties easyLogProperties) {
        this.easyLogProperties = easyLogProperties;
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDiffEngine.class)
    public IParseFunction diffParseFunction() {
        return new DefaultDiffEngine(easyLogProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ParseFunctionFactory.class)
    public ParseFunctionFactory parseFunctionFactory(java.util.List<IParseFunction> parseFunctions) {
        return new ParseFunctionFactory(parseFunctions);
    }

    @Bean
    @ConditionalOnMissingBean(IFunctionService.class)
    public IFunctionService functionService(ParseFunctionFactory parseFunctionFactory) {
        return new DefaultFunctionServiceImpl(parseFunctionFactory);
    }

    @Bean
    public EasyLogParser easyLogParser(IFunctionService functionService) {
        return new EasyLogParser(functionService);
    }

    @Bean
    @ConditionalOnMissingBean(IOperatorService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IOperatorService operatorGetService() {
        return new DefaultOperatorServiceImpl(easyLogProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ILogRecordService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public ILogRecordService recordService() {
        return new DefaultLogRecordServiceImpl();
    }

    @Bean
    @ConditionalOnClass(Aspect.class)
    @ConditionalOnMissingBean(EasyLogAspect.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public EasyLogAspect easyLogAspect(ILogRecordService logRecordService,
                                       IOperatorService operatorService,
                                       EasyLogParser easyLogParser) {
        return new EasyLogAspect(logRecordService, operatorService, easyLogParser, easyLogProperties.isAfterCommit());
    }
}
