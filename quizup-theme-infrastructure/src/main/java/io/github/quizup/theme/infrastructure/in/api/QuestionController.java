package io.github.quizup.theme.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.IdResponse;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.infrastructure.security.SecurityHelper;
import io.github.quizup.theme.domain.port.in.*;
import io.github.quizup.theme.infrastructure.in.api.mapper.QuestionResponseMapper;
import io.github.quizup.theme.infrastructure.in.api.request.QuestionRequest;
import io.github.quizup.theme.infrastructure.in.api.response.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * QuestionController - API REST pour la gestion des questions d'un thème
 */
@RestController
@RequestMapping(QuestionController.QUESTIONS_ENDPOINT)
public class QuestionController {

    public static final String QUESTIONS_ENDPOINT = "/api/questions";

    private final GetQuestionUseCase getQuestionUseCase;
    private final SearchQuestionUseCase searchQuestionUseCase;
    private final CreateQuestionUseCase createQuestionUseCase;
    private final ApproveQuestionUseCase approveQuestionUseCase;
    private final RejectQuestionUseCase rejectQuestionUseCase;

    public QuestionController(SearchQuestionUseCase searchQuestionUseCase,
                              CreateQuestionUseCase createQuestionUseCase,
                              ApproveQuestionUseCase approveQuestionUseCase,
                              RejectQuestionUseCase rejectQuestionUseCase,
                              GetQuestionUseCase getQuestionUseCase) {
        this.searchQuestionUseCase = searchQuestionUseCase;
        this.createQuestionUseCase = createQuestionUseCase;
        this.approveQuestionUseCase = approveQuestionUseCase;
        this.rejectQuestionUseCase = rejectQuestionUseCase;
        this.getQuestionUseCase = getQuestionUseCase;
    }


    /**
     * Search topics with pagination and sorting
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<QuestionResponse>>> search(
            @RequestBody SearchRequest searchRequest
    ) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchQuestionUseCase.search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(QuestionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> createQuestion(
            @RequestBody @Valid QuestionRequest.CreateQuestionRequest request
    ) {
        String questionId = UUID.randomUUID().toString();
        String creatorId = SecurityHelper.getUserId();
        return createQuestionUseCase
                .create(
                        questionId,
                        request.topicId(),
                        request.text(),
                        request.answers(),
                        request.correctAnswer(),
                        creatorId
                )
                .thenApply(_ -> ResponseEntityBuilder.creation(QUESTIONS_ENDPOINT,questionId));
    }

    @GetMapping("/{questionId}")
    public CompletableFuture<ResponseEntity<QuestionResponse>> getQuestionById(
            @PathVariable String questionId
    ) {
        return getQuestionUseCase.getById(questionId)
                .thenApply(QuestionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{questionId}/approve")
    public CompletableFuture<ResponseEntity<IdResponse>> approveQuestion(
            @PathVariable String questionId
    ) {
        String requesterId = SecurityHelper.getUserId();
        return approveQuestionUseCase.approve(questionId, requesterId)
                .thenApply(ResponseEntityBuilder::ok);
    }

    @PostMapping("/{questionId}/reject")
    public CompletableFuture<ResponseEntity<IdResponse>> rejectQuestion(
            @PathVariable String questionId,
            @RequestBody @Valid QuestionRequest.RejectQuestionRequest request
    ) {
        String requesterId = SecurityHelper.getUserId();
        return rejectQuestionUseCase.reject(questionId, requesterId, request.reason())
                .thenApply(ResponseEntityBuilder::ok);
    }
}
