package com.econoup.onboarding.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterestsRequest(@NotEmpty List<String> categoryCodes) {
}
