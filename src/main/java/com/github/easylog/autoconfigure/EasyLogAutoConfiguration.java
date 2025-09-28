package com.github.easylog.autoconfigure;


import com.github.easylog.aop.EasyLogAspect;
import com.github.easylog.autoconfigure.EasyLogProperties;
import com.github.easylog.function.CustomFunctionFactory;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.function.ICustomFunction;
import com.github.easylog.function.IFunctionService;
import com.github.easylog.function.impl.DefaultFunctionServiceImpl;
import com.github.easylog.api.ILogRecordService;
import com.github.easylog.api.IOperatorService;
import com.github.easylog.support.DefaultLogRecordServiceImpl;
import com.github.easylog.support.JdbcLogRecordServiceImpl;
import com.github.easylog.support.DefaultOperatorServiceImpl;
import com.github.easylog.util.EasyLogVersion;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author Gaosl
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "easylog", name = "enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({EasyLogProperties.class})
@RequiredArgsConstructor
@Slf4j
public class EasyLogAutoConfiguration {

    private final EasyLogProperties easyLogProperties;

    @PostConstruct
    public void printBanner() {
        if (!easyLogProperties.isBanner()){
            return;
        }
        String banner = "                        _             \n" +
                "                       | |            \n" +
                "  ___  __ _ ___ _   _  | | ___   __ _ \n" +
                " / _ \\/ _` / __| | | | | |/ _ \\ / _` |\n" +
                "|  __/ (_| \\__ \\ |_| | | | (_) | (_| |\n" +
                " \\___|\\__,_|___/\\__, | |_|\\___/ \\__, |\n" +
                "                 __/ |           __/ |\n" +
                "                |___/           |___/ \n" +
                "  <<easy-log>>            " + EasyLogVersion.getVersion();
        log.info("\n{}", banner);
    }

    @Bean
    public CustomFunctionFactory customFunctionFactory(List<ICustomFunction> iCustomFunctionList) {
        return new CustomFunctionFactory(iCustomFunctionList);
    }

    @Bean
    public IFunctionService customFunctionService(CustomFunctionFactory customFunctionFactory) {
        return new DefaultFunctionServiceImpl(customFunctionFactory);
    }

    @Bean
    public EasyLogParser easyLogParser() {
        return new EasyLogParser();
    }

    @Bean
    @ConditionalOnMissingBean(IOperatorService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IOperatorService operatorGetService() {
        return new DefaultOperatorServiceImpl(easyLogProperties);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(ILogRecordService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public ILogRecordService jdbcRecordService(DataSource dataSource) {
        return new JdbcLogRecordServiceImpl(dataSource);
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
        return new EasyLogAspect(logRecordService, operatorService, easyLogParser);
    }
}
