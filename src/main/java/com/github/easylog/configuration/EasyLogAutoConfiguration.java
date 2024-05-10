package com.github.easylog.configuration;


import com.github.easylog.function.CustomFunctionFactory;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.function.ICustomFunction;
import com.github.easylog.function.IFunctionService;
import com.github.easylog.function.impl.DefaultCustomFunction;
import com.github.easylog.function.impl.DefaultFunctionServiceImpl;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.service.impl.DefaultLogRecordServiceImpl;
import com.github.easylog.service.impl.DefaultOperatorServiceImpl;
import com.github.easylog.util.EasyLogVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author Gaosl
 */
@Configuration
@ComponentScan("com.github.easylog")
@ConditionalOnProperty(prefix = "easylog", name = "enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({EasyLogProperties.class})
public class EasyLogAutoConfiguration {

    @Autowired
    private EasyLogProperties easyLogProperties;

    @PostConstruct
    public void printBanner() {
        if (!easyLogProperties.isBanner()){
            return;
        }
        System.out.println("                        _             \n" +
                "                       | |            \n" +
                "  ___  __ _ ___ _   _  | | ___   __ _ \n" +
                " / _ \\/ _` / __| | | | | |/ _ \\ / _` |\n" +
                "|  __/ (_| \\__ \\ |_| | | | (_) | (_| |\n" +
                " \\___|\\__,_|___/\\__, | |_|\\___/ \\__, |\n" +
                "                 __/ |           __/ |\n" +
                "                |___/           |___/ \n");
        System.out.println("  <<easy-log>>            " + EasyLogVersion.getVersion() + " ");
    }

    @Bean
    @ConditionalOnMissingBean(ICustomFunction.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public ICustomFunction customFunction() {
        return new DefaultCustomFunction();
    }

    @Bean
    public CustomFunctionFactory CustomFunctionRegistrar(@Autowired List<ICustomFunction> iCustomFunctionList) {
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
        return new DefaultOperatorServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(ILogRecordService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public ILogRecordService recordService() {
        return new DefaultLogRecordServiceImpl();
    }
}
