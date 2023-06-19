
#include "acquisition.h"

#include <malloc.h>
#include <string.h>
#include <math.h>

typedef struct {
    int32_t codePhase;
    int32_t dopplerFrequency;
    int32_t sampleCount;
    float* samples;
    const float* inputCodes;
    int32_t testFreqCount;
    const int32_t* const testFrequencies;
    float maxMagnitude;
    float inputPower;
    float gamma;
} acquisitionInternal_t;

acquisition_t* allocateAcquisition(int32_t nrOfSamples) {
    acquisitionInternal_t * a = malloc(sizeof(acquisitionInternal_t));

    memset(a, 0, sizeof(acquisitionInternal_t)); // to initialize everything into a definitive state (=0)

    a->codePhase = malloc(sizeof(int32_t));
    a->dopplerFrequency = malloc(sizeof(int32_t));
    a->sampleCount = malloc(sizeof(int32_t));
    a->samples = malloc(nrOfSamples * 2 * sizeof(float));
    a->codeCount = malloc(sizeof(int32_t));
    a->inputCodes = malloc(nrOfSamples * 2 * sizeof(float));
    a->testFreqCount = malloc(sizeof(int32_t));
    a->testFrequencies = malloc(4 * sizeof(int32_t));
    a->maxMagnitude = malloc(sizeof(float));
    a->inputPower = malloc(sizeof(float));
    a->gamma = malloc(sizeof(float));

    return (acquisition_t*)a;
}

void deleteAcquisition(acquisition_t* acq) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    // free also everything else that was allocated in [allocateAcquisition]
    free(a->samples);
    free(a->codePhase);
    free(a->dopplerFrequency);
    free(a->sampleCount);
    free(a->samples);
    free(a->codeCount);
    free(a->inputCodes);
    free(a->testFreqCount);
    free(a->testFrequencies);
    free(a->maxMagnitude);
    free(a->inputPower);
    free(a->gamma);
    // after freeing all contained structures on heap, free acq itself
    free(acq);
}

void enterSample(acquisition_t* acq, float real, float imag) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    // put a sample-entry into the state in [a]

    a->samples[a->sampleCount] = real;
    a->samples[a->sampleCount+1] = imag;
    a->sampleCount += 2;
}

void enterCode(acquisition_t* acq, float real, float imag) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    // put a code-entry into the state in [a]

    a->inputCodes[a->codeCount] = real;
    a->inputCodes[a->codeCount+1] = imag;
    a->codeCount += 2;
}

__attribute__((noipa))
bool startAcquisition(acquisition_t* acq, int32_t testFreqCount, const int32_t* testFrequencies) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

	//
	// do actual calculation
	//

	bool result;

	return result; // return whether acquisition was achieved or not!
}