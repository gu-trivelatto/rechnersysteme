#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef struct {
    bool acquisition;
    int32_t codePhase;
    int32_t dopplerFrequency;
} acquisition_t;

/**
 * allocate all data structures for acquisition. Extend struct acquisition_t for this.
 */
acquisition_t* allocateAcquisition(int32_t nrOfSamples);
/**
 * free everything allocated for acquisition. Basically inverse of [allocateAcquisition]
 */
void deleteAcquisition(acquisition_t* acq);

/**
 * add a sample to the acquisition data
 */
void enterSample(acquisition_t* acq, float real, float imag);
/**
 * add a code to the acquisition data
 */
void enterCode(acquisition_t* acq, float real, float imag);

/**
 * the actual process of gpsAcquisition. All calculation should go here. This will be accelerated with the CGRA
 */
bool startAcquisition(acquisition_t* acq, int32_t* testFrequencies);
