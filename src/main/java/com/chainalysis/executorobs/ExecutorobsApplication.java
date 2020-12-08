package com.chainalysis.executorobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.Callable;

@SpringBootApplication
@EnableScheduling
@RestController
@Slf4j
public class ExecutorobsApplication {
    @Autowired
    @Qualifier("ourExecutor")
    AsyncTaskExecutor executor;

    public static void main(String[] args) {
        SpringApplication.run(ExecutorobsApplication.class, args);
    }

    @Bean
    AsyncTaskExecutor ourExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        return executor;
    }

    @Component
    @RequiredArgsConstructor
    public class WebMvcConfig implements WebMvcConfigurer {

        @Override
        public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
            configurer.setTaskExecutor(ourExecutor());
            configurer.setDefaultTimeout(Duration.ofSeconds(85).toMillis());
            configurer.registerCallableInterceptors(new CallableProcessingInterceptor() {
                @Override
                public <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) {
                    log.info("beforeConcurrentHandling {}: {}", request, task);
                }

                @Override
                public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
                    log.info("preProcess {}: {}", request, task);
                }

                @Override
                public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
                    log.info("postProcess {}: {}", request, task);
                }

                @Override
                public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) {
                    log.info("handleTimeout {}: {}", request, task);
                    return null;
                }

                @Override
                public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) {
                    log.info("handleError {}: {}", request, task);
                    return null;
                }

                @Override
                public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) {
                    log.info("afterCompletion {}: {}", request, task);
                }
            });
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    log.info("Prehandle {}, {}", request, handler);
                    return true;
                }
            });
        }
    }

    @GetMapping("/api/cluster/info")
    public Callable<String> info() {
        return () -> "Hello async";
    }
}
