package com.kep.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNodeRequest(Long parentId, @NotBlank String name) {}
