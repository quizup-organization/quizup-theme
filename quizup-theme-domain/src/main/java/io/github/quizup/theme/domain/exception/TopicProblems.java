package io.github.quizup.theme.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

/**
 * Exceptions spécifiques au domaine des thèmes, encapsulant les problèmes liés à la gestion des thèmes.
 */
public final class TopicProblems {

    private TopicProblems() {
        // Classe utilitaire
    }

    public static class TopicNotFoundProblem extends TopicProblem {
        public TopicNotFoundProblem(String topicId) {
            super(topicId, "urn:quizup:topic:notFound",
                  ProblemCategory.BUSINESS_RESOURCE_MISSING,
                  "Topic not found",
                  "The topic " + topicId + " was not found", null);
        }
    }

    public static class TopicCategoryEmptyProblem extends TopicProblem {
        public TopicCategoryEmptyProblem(String topicId) {
            super(topicId, "urn:quizup:topic:categoryEmpty", "Topic category cannot be empty");
        }
    }

    public static class TopicNameEmptyProblem extends TopicProblem {
        public TopicNameEmptyProblem(String topicId) {
            super(topicId, "urn:quizup:topic:nameEmpty", "Topic name cannot be empty");
        }
    }

    public static class CreatorIdEmptyProblem extends TopicProblem {
        public CreatorIdEmptyProblem(String topicId) {
            super(topicId, "urn:quizup:topic:creatorIdEmpty", "Creator ID cannot be empty");
        }
    }

    public static class TopicNotInDraftProblem extends TopicProblem {
        public TopicNotInDraftProblem(String topicId) {
            super(topicId, "urn:quizup:topic:notInDraft",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Topic not in draft status",
                    "The topic " + topicId + " must be in DRAFT status to be published", null);
        }
    }
}
