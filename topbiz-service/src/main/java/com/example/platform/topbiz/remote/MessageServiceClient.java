package com.example.platform.topbiz.remote;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.topbiz.config.TopbizFeignConfig;
import com.example.platform.topbiz.remote.dto.RemoteMessageDraftRequest;
import com.example.platform.topbiz.remote.dto.RemoteMessageResponse;
import com.example.platform.topbiz.remote.dto.RemoteMessageSendRequest;
import com.example.platform.topbiz.remote.dto.RemoteMessageStatusResponse;
import com.example.platform.topbiz.remote.dto.RemoteTemplateDetailResponse;
import com.example.platform.topbiz.remote.dto.RemoteTemplatePreviewRequest;
import com.example.platform.topbiz.remote.dto.RemoteTemplatePreviewResponse;
import com.example.platform.topbiz.remote.dto.RemoteTemplateSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "messageServiceClient", url = "${topbiz.remote.message-service.base-url}", configuration = TopbizFeignConfig.class)
public interface MessageServiceClient {

    @PostMapping("/api/messages/send")
    ApiResponse<RemoteMessageResponse> send(@RequestBody RemoteMessageSendRequest request);

    @PostMapping("/api/messages/drafts")
    ApiResponse<RemoteMessageResponse> saveDraft(@RequestBody RemoteMessageDraftRequest request);

    @GetMapping("/api/messages/{messageId}")
    ApiResponse<RemoteMessageResponse> getMessage(@PathVariable String messageId);

    @GetMapping("/api/messages/{messageId}/status")
    ApiResponse<RemoteMessageStatusResponse> getStatus(@PathVariable String messageId);

    @GetMapping("/api/messages/templates")
    ApiResponse<List<RemoteTemplateSummaryResponse>> listTemplates(@RequestParam(required = false) String channel);

    @GetMapping("/api/messages/templates/{templateCode}")
    ApiResponse<RemoteTemplateDetailResponse> getTemplate(@PathVariable String templateCode);

    @PostMapping("/api/messages/templates/{templateCode}/preview")
    ApiResponse<RemoteTemplatePreviewResponse> preview(@PathVariable String templateCode,
                                                       @RequestBody RemoteTemplatePreviewRequest request);
}
