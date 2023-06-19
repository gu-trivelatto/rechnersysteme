
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
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    float angle = 0.0;
    float cos = 0.0;
    float sin = 0.0;

    for (int32_t n = 0; n < a->testFreqCount; n++) {
        for (int32_t f = 0; f < a->sampleCount; f++) {
            angle = 2 * M_PI * a->testFrequencies[f] * n / F_S;
            cos = cosf(angle);
            sin = sinf(angle);

            xMatrixReal[n][f] = a->samplesReal[f] * cos + a->samplesImag[f] * sin;
            xMatrixImag[n][f] = -a->samplesReal[f] * sin + a->samplesImag[f] * cos;
        }
    }
}

void computeR(aquisition_t* acq, float *xMatrixReal, float *xMatrixImag, float *rMatrixReal, float *rMatrixImag) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    for (int32_t n = 0; n < a->testFreqCount; n++) {
        for (int32_t f = 0; f < a->codesCount; f++) {
            rMatrixReal[n][f] = xMatrixReal[n][f] * a->inputCodesReal[n][f] - xMatrixImag[n][f] * a->inputCodesImag[n][f];
            rMatrixImag[n][f] = xMatrixReal[n][f] * a->inputCodesImag[n][f] + xMatrixImag[n][f] * a->inputCodesReal[n][f];
        }
    }
}

void computeFourier(acquisitionInternal_t* acq) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;
}

void computeInvFourier(acquisitionInternal_t* acq) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;
}

int computeMaxValue(acquisitionInternal_t* acq, float* rMatrixReal, float* rMatrixImag, float sMax) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    float absValue = 0.0;

    for (int32_t n = 0; n < a->testFreqCount; n++) {
        for (int32_t f = 0; f < a->sampleCount; f++) {
            absValue = rMatrixReal[n][f] * rMatrixReal[n][f] + rMatrixImag[n][f] * rMatrixImag[n][f];
            if(absValue > sMax) {
                sMax = absValue;
                a->codePhase = a->sampleCount - f;
                a->dopplerFrequency = a->testFrequencies[n];
            }
        }
    }

    return sMax;
}

void estimatePIn(acquisitionInternal_t* acq) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;
    float pIn = 0.0;

    for (int i = 0; i < a->sampleCount; i++) {
        pIn += a->samplesReal[i] * a->samplesReal[i] + a->samplesImag[i] * a->samplesImag[i];
    }

    return (pIn / a->sampleCount);
}

__attribute__((noipa))
bool startAcquisition(acquisition_t* acq, int32_t testFreqCount, const int32_t* testFrequencies) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

	int xMatrixReal[a->testFreqCount][a->sampleCount];
    int xMatrixImag[a->testFreqCount][a->sampleCount];

    int rMatrixReal[a->testFreqCount][a->sampleCount];
    int rMatrixImag[a->testFreqCount][a->sampleCount];

    float sMax = 0.0;
    float pIn = 0.0;

    computeX(a, xMatrixReal, xMatrixImag);

    computeFourier(a, a->codesReal, a->codesImag);

    for(int32_t n = 0; n < a->sampleCount; n++) {
        a->codesImag[n] = -a->codesImag[n];
    }

    for(int32_t f = 0; n < testFreqCount; f++) {
        computeFourier(a, xMatrixReal[f], xMatrixImag[f]);
    }
    
    computeR(a, xMatrixReal, xMatrixImag, xMatrixReal, xMatrixImag);

    computeInvFourier(a, rMatrixReal, rMatrixImag);

    for (int32_t n = 0; n < testFreqCount; n++) {
        for (int32_t f = 0; f < sampleCount; f++) {
            rMatrixReal[n][f] /= sampleCount;
            rMatrixImag[n][f] /= sampleCount;
        }
    }

    sMax = computeMaxValue(a, rMatrixReal, rMatrixImag, sMax);

    pIn = estimatePIn(a);

    bool result = ((sMax / pIn) > a->gamma);

	return result; // return whether acquisition was achieved or not!
}