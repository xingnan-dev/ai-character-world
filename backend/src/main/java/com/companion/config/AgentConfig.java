package com.companion.config;
import org.springframework.context.annotation.*;import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;import java.util.concurrent.*;
@Configuration @org.springframework.boot.context.properties.EnableConfigurationProperties(AgentProperties.class)
public class AgentConfig {
 @Bean(name="agentToolWorkerExecutor",destroyMethod="shutdown") ExecutorService agentToolWorkerExecutor(AgentProperties p){
  ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();e.setCorePoolSize(p.getToolThreads());e.setMaxPoolSize(p.getToolThreads());e.setQueueCapacity(p.getToolQueueCapacity());e.setThreadNamePrefix("agent-tool-");e.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());e.initialize();return e.getThreadPoolExecutor();
 }
 @Bean(name="agentExecutionExecutor") Executor agentExecutionExecutor(AgentProperties p){
  ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();e.setCorePoolSize(p.getExecutionThreads());e.setMaxPoolSize(p.getExecutionThreads());e.setQueueCapacity(p.getExecutionQueueCapacity());e.setThreadNamePrefix("agent-run-");e.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());e.setWaitForTasksToCompleteOnShutdown(true);e.setAwaitTerminationSeconds(p.getShutdownAwaitSeconds());e.initialize();return e;
 }
}
