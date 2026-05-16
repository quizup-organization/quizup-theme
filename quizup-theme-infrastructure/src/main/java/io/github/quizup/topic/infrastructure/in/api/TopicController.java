package io.github.quizup.topic.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.IdResponse;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.infrastructure.security.SecurityHelper;
import io.github.quizup.topic.domain.port.in.CreateTopicUseCase;
import io.github.quizup.topic.domain.port.in.GetTopicUseCase;
import io.github.quizup.topic.domain.port.in.PublishTopicUseCase;
import io.github.quizup.topic.domain.port.in.SearchTopicUseCase;
import io.github.quizup.topic.infrastructure.in.api.mapper.TopicResponseMapper;
import io.github.quizup.topic.infrastructure.in.api.request.TopicRequest;
import io.github.quizup.topic.infrastructure.in.api.response.TopicResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * TopicController - API REST pour la gestion des thèmes
 */
@RestController
@RequestMapping(TopicController.ENDPOINT)
public class TopicController {

    public static final String ENDPOINT = "/api/topics";

    private final GetTopicUseCase getTopicUseCase;
    private final CreateTopicUseCase createTopicUseCase;
    private final PublishTopicUseCase publishTopicUseCase;
    private final SearchTopicUseCase searchTopicUseCase;

    public TopicController(CreateTopicUseCase createTopicUseCase,
                           PublishTopicUseCase publishTopicUseCase,
                           GetTopicUseCase getTopicUseCase,
                           SearchTopicUseCase searchTopicUseCase) {
        this.createTopicUseCase = createTopicUseCase;
        this.publishTopicUseCase = publishTopicUseCase;
        this.getTopicUseCase = getTopicUseCase;
        this.searchTopicUseCase = searchTopicUseCase;
    }

    /**
     * Search topics with pagination and sorting
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<TopicResponse>>> search(
            @RequestBody SearchRequest searchRequest
    ) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchTopicUseCase.search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(TopicResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Créer un nouveau thème
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> createTopic(
            @RequestBody @Valid TopicRequest.CreateTopicRequest request
    ) {
        String topicId = UUID.randomUUID().toString();
        String creatorId = SecurityHelper.getUserId();
        return createTopicUseCase.create(
                        topicId,
                        request.name(),
                        request.description(),
                        request.category(),
                        creatorId
                )
                .thenApply(_ -> ResponseEntityBuilder.creation(ENDPOINT, topicId));
    }

    /**
     * Publier un thème (passage DRAFT -> PUBLISHED)
     */
    @PostMapping("/{topicId}/publish")
    public CompletableFuture<ResponseEntity<IdResponse>> publishTopic(
            @PathVariable String topicId
    ) {
        String requesterId = SecurityHelper.getUserId();
        return publishTopicUseCase.publish(topicId, requesterId)
                .thenApply(ResponseEntityBuilder::ok);
    }

    /**
     * Récupérer un thème par son ID
     */
    @GetMapping("/{topicId}")
    public CompletableFuture<ResponseEntity<TopicResponse>> getTopicById(
            @PathVariable String topicId
    ) {
        return getTopicUseCase.getById(topicId)
                .thenApply(TopicResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}

