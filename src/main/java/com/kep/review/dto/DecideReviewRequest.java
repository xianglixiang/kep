package com.kep.review.dto;

public record DecideReviewRequest(long reviewId, boolean approved, String opinion) {}
