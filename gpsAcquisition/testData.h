#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef struct {
	int32_t complexSampleCount;
    const float* inputCodes;
    const float* inputSamples;
    const int32_t* const testFrequencies;
    int32_t dopplerFrequency;
    bool acquisition;
    int32_t codePhase;
    float maxMagnitude;
    float inputPower;
    float gamma;
} testCase_t;

/**
 * Requests testData.
 * May be hooked by simulator to check / mock / replace test-data
 */
__attribute__((noipa))
const testCase_t* getTestCase(int testId);
