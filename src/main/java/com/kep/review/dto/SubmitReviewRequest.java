package com.kep.review.dto;

public record SubmitReviewRequest(long knowledgeId, Long versionId,
                                   String reviewerSubjectType, long reviewerSubjectId) {}
