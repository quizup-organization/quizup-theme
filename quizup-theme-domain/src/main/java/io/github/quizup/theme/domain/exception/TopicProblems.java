package io.github.quizup.theme.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;

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

    public static class TopicNotOwnerProblem extends TopicProblem {
        public TopicNotOwnerProblem(String topicId, String requestedBy) {
            super(topicId, "urn:quizup:topic:notOwner",
                    ProblemCategory.PERMISSION,
                    "Topic update not allowed",
                    "Only the creator of the topic can update it",
                    Map.of("requestedBy", requestedBy));
        }
    }

    public static class TopicNameTooLongProblem extends TopicProblem {
        public TopicNameTooLongProblem(String topicId, int maxLength) {
            super(topicId, "urn:quizup:topic:nameTooLong",
                    ProblemCategory.VALIDATION,
                    "Topic name too long",
                    "The topic name must not exceed " + maxLength + " characters", null);
        }
    }

    public static class TopicDescriptionTooLongProblem extends TopicProblem {
        public TopicDescriptionTooLongProblem(String topicId, int maxLength) {
            super(topicId, "urn:quizup:topic:descriptionTooLong",
                    ProblemCategory.VALIDATION,
                    "Topic description too long",
                    "The topic description must not exceed " + maxLength + " characters", null);
        }
    }

    public static class TopicEmojiTooLongProblem extends TopicProblem {
        public TopicEmojiTooLongProblem(String topicId, int maxLength) {
            super(topicId, "urn:quizup:topic:emojiTooLong",
                    ProblemCategory.VALIDATION,
                    "Topic emoji too long",
                    "The topic emoji must not exceed " + maxLength + " characters", null);
        }
    }

    public static class TopicColorTooLongProblem extends TopicProblem {
        public TopicColorTooLongProblem(String topicId, int maxLength) {
            super(topicId, "urn:quizup:topic:colorTooLong",
                    ProblemCategory.VALIDATION,
                    "Topic color too long",
                    "The topic color must not exceed " + maxLength + " characters", null);
        }
    }

    public static class TopicImageUrlTooLongProblem extends TopicProblem {
        public TopicImageUrlTooLongProblem(String topicId, int maxLength) {
            super(topicId, "urn:quizup:topic:imageUrlTooLong",
                    ProblemCategory.VALIDATION,
                    "Topic image URL too long",
                    "The topic image URL must not exceed " + maxLength + " characters", null);
        }
    }
}
