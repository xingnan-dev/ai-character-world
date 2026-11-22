package com.companion.agent;
import lombok.Getter;
@Getter public class AgentExecutionException extends RuntimeException { private final String code;private final boolean retryable;private final int retryCount;
 public AgentExecutionException(String code,String message,boolean retryable){this(code,message,retryable,-1,null);}
 public AgentExecutionException(String code,String message,boolean retryable,Throwable cause){this(code,message,retryable,-1,cause);}
 public AgentExecutionException(String code,String message,boolean retryable,int retryCount,Throwable cause){super(message,cause);this.code=code;this.retryable=retryable;this.retryCount=retryCount;}
}
