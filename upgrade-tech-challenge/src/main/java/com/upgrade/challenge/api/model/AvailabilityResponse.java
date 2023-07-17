package com.upgrade.challenge.api.model;

import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
public class AvailabilityResponse {
    List<String> daysAvailableList;
    Optional<String> errorMessage;
}
