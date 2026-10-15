package com.threatguard.api;

public record ModelMetrics(
    String algorithm,
    int baselineSamples,
    int validationSamples,
    double threshold,
    double precision,
    double recall,
    double f1Score
) {}
