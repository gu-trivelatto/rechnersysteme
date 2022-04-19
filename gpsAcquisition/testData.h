#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef struct {
    float* inputCodes;
    float* inputSamples;
    int32_t* testFrequencies;
    int32_t dopplerFrequency;
    bool acquisition;
    int32_t codePhase;
    float maxMagnitude;
    float inputPower;
    float gamma;
} testCase_t;

extern testCase_t testCases[];
