
#include "acquisition.h"

#include <malloc.h>
#include <string.h>
#include <math.h>

#define F_S 2000000 // sample frequency of 2MHz

typedef struct {
    int32_t codePhase;
    int32_t dopplerFrequency;

    int32_t sampleCount;
    float* samplesReal;
    float* samplesImag;

    int32_t codesCount;
    float* inputCodesReal;
    float* inputCodesImag;

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

    a->sampleCount = 0;
    a->samplesReal = malloc(nrOfSamples * sizeof(float));
    a->samplesImag = malloc(nrOfSamples * sizeof(float));
    
    a->codeCount = 0;
    a->inputCodesReal = malloc(nrOfSamples * sizeof(float));
    a->inputCodesImag = malloc(nrOfSamples * sizeof(float));

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
    free(a->codePhase);
    free(a->dopplerFrequency);

    free(a->sampleCount);
    free(a->samplesReal);
    free(a->samplesImag);

    free(a->codeCount);
    free(a->inputCodesReal);
    free(a->inputCodesImag);

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

    a->samplesReal[a->sampleCount] = real;
    a->samplesImag[a->sampleCount] = imag;
    a->sampleCount += 1;
}

void enterCode(acquisition_t* acq, float real, float imag) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    // put a code-entry into the state in [a]

    a->inputCodesReal[a->codeCount] = real;
    a->inputCodesImag[a->codeCount] = imag;
    a->codeCount += 1;
}

void computeX(aquisition_t* acq, float *xMatrixReal, float *xMatrixImag) {
    float angle = 0.0;
    float cos = 0.0;
    float sin = 0.0;

    for (int32_t n = 0; n <= acq->testFreqCount; n++) {
        for (int32_t f = 0; f <= acq->sampleCount; f++) {
            angle = 2 * M_PI * acq->testFrequencies[f] * n / F_S;
            cos = cosf(angle);
            sin = sinf(angle);

            xMatrixReal[n][f] = acq->samplesReal[f] * cos + acq->samplesImag[f] * sin;
            xMatrixImag[n][f] = -acq->samplesReal[f] * sin + acq->samplesImag[f] * cos;
        }
    }
}

void computeR(aquisition_t* acq, float *xMatrixReal, float *xMatrixImag, float *rMatrixReal, float *rMatrixImag) {

    for (int32_t n = 0; n <= acq->codesCount; n++) {
        rMatrixReal[n] = xMatrixReal[n] * acq->inputCodesReal[n] - xMatrixImag[n] * acq->inputCodesImag[n];
        rMatrixImag[n] = xMatrixReal[n] * acq->inputCodesImag[n] + xMatrixImag[n] * acq->inputCodesReal[n];
    }
}

void computeFourier() {}

void computeMaxValue(acquisitionInternal_t* acq, float* rMatrixReal, float* rMatrixImag, float sMax) {
    float absValue = 0.0;

	for(int32_t n = 0; n < acq->sampleCount; n++) {
        absValue = rMatrixReal[n] * rMatrixReal[n] + rMatrixImag[n] * rMatrixImag[n];
        if(absValue > sMax) {
            sMax = absValue;
            a->codePhase = acq->sampleCount - n;
            a->dopplerFrequency = acq->testFrequencies[n];
        }
    }

    return sMax;
}

void estimateSignalPower() {}

__attribute__((noipa))
bool startAcquisition(acquisition_t* acq, int32_t testFreqCount, const int32_t* testFrequencies) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

	int xMatrixReal[acq->testFreqCount][acq->sampleCount];
    int xMatrixImag[acq->testFreqCount][acq->sampleCount];

    computeX(acq, xMatrixReal, xMatrixImag);

	bool result;

	return result; // return whether acquisition was achieved or not!
}