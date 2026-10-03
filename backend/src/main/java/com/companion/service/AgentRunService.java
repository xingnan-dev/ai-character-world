package com.companion.service;
import com.companion.dto.request.AgentRunCreateRequest;import com.companion.dto.response.AgentRunPageResponse;import com.companion.dto.response.AgentRunResponse;
public interface AgentRunService {AgentRunResponse create(Long userId,AgentRunCreateRequest request);AgentRunResponse get(Long userId,Long runId);AgentRunPageResponse list(Long userId,int page,int pageSize);AgentRunResponse resume(Long userId,Long runId);}
