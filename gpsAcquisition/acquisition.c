
#include "acquisition.h"
#include <stdio.h>   // apagar depois

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
    int32_t* testFrequencies;

    //float gamma;
} acquisitionInternal_t;

acquisition_t* allocateAcquisition(int32_t nrOfSamples) {
    acquisitionInternal_t * a = malloc(sizeof(acquisitionInternal_t));

    memset(a, 0, sizeof(acquisitionInternal_t)); // to initialize everything into a definitive state (=0)

    a->codePhase = 0;
    a->dopplerFrequency = 0;

    a->sampleCount = 0;
    a->samplesReal = malloc(nrOfSamples * sizeof(float));
    a->samplesImag = malloc(nrOfSamples * sizeof(float));
    
    a->codesCount = 0;
    a->inputCodesReal = malloc(nrOfSamples * sizeof(float));
    a->inputCodesImag = malloc(nrOfSamples * sizeof(float));

    a->testFreqCount = 0;
    a->testFrequencies = malloc(4 * sizeof(int32_t));

    //a->gamma = malloc(sizeof(float));

    return (acquisition_t*)a;
}

void deleteAcquisition(acquisition_t* acq) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    // free also everything else that was allocated in [allocateAcquisition]
    free(a->samplesReal);
    free(a->samplesImag);

    free(a->inputCodesReal);
    free(a->inputCodesImag);

    free(a->testFrequencies);

    //free(a->gamma);

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

    a->inputCodesReal[a->codesCount] = real;
    a->inputCodesImag[a->codesCount] = imag;
    a->codesCount += 1;
}

void computeX(acquisitionInternal_t* a, float **xMatrixReal, float **xMatrixImag) {
    float angle = 0.0;
    float cos = 0.0;
    float sin = 0.0;
    float multiplier = 2 * M_PI / F_S;
    float *xReal, *xImag;

    for (int32_t f = 0; f < a->testFreqCount; f++) {
        float freqMult = multiplier * a->testFrequencies[f];
        xReal = xMatrixReal[f];
        xImag = xMatrixImag[f];

        for (int32_t n = 0; n < a->sampleCount; n++) {
            angle = freqMult * n;
            cos = cosf(angle);
            sin = sinf(angle);

            xReal[n] = a->samplesReal[n] * cos + a->samplesImag[n] * sin;
            xImag[n] = -a->samplesReal[n] * sin + a->samplesImag[n] * cos;
        }
    }
}

void computeR(acquisitionInternal_t* a, float **xMatrixReal, float **xMatrixImag, float **rMatrixReal, float **rMatrixImag) {
    float *rReal, *rImag;
    float *xReal, *xImag;

    for (int32_t f = 0; f < a->testFreqCount; f++) {
        rReal = rMatrixReal[f];
        rImag = rMatrixImag[f];
        xReal = xMatrixReal[f];
        xImag = xMatrixImag[f];

        for (int32_t n = 0; n < a->codesCount; n++) {
            rReal[n] = xReal[n] * a->inputCodesReal[n] - xImag[n] * a->inputCodesImag[n];
            rImag[n] = xReal[n] * a->inputCodesImag[n] + xImag[n] * a->inputCodesReal[n];
        }
    }
}

void computeFourier(acquisitionInternal_t* acq, float *real, float *imag, int n) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    if (n <= 1) {
        return;
    }

    float *evenReal = malloc(n / 2 * sizeof(float));
    float *evenImag = malloc(n / 2 * sizeof(float));
    float *oddReal = malloc(n / 2 * sizeof(float));
    float *oddImag = malloc(n / 2 * sizeof(float));

    for (int i = 0; i < n / 2; i++) {
        evenReal[i] = real[2 * i];
        evenImag[i] = imag[2 * i];
        oddReal[i] = real[2 * i + 1];
        oddImag[i] = imag[2 * i + 1];
    }

    computeFourier(acq, evenReal, evenImag, n / 2);
    computeFourier(acq, oddReal, oddImag, n / 2);

    for (int k = 0; k < n / 2; k++) {
        float angle = -2 * M_PI * k / n;
        float cosVal = cos(angle);
        float sinVal = sin(angle);
    
        float re = oddReal[k] * cosVal - oddImag[k] * sinVal;
        float im = oddReal[k] * sinVal + oddImag[k] * cosVal;

        real[k] = evenReal[k] + re;
        imag[k] = evenImag[k] + im;
        real[k + n / 2] = evenReal[k] - re;
        imag[k + n / 2] = evenImag[k] - im;
    }

    free(evenReal);
    free(evenImag);
    free(oddReal);
    free(oddImag);

}

void computeInvFourier(acquisitionInternal_t* acq, float *real, float *imag, int n) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    computeFourier(acq, imag, real, n);

    for (int i = 0; i < n; i++) {
        real[i] /= n;
        imag[i] /= n;
    }
}

float computeMaxValue(acquisitionInternal_t* acq, float **rMatrixReal, float **rMatrixImag, float sMax) {
    acquisitionInternal_t * a = (acquisitionInternal_t*) acq;

    float absValue = 0.0;
    float *rReal, *rImag;
    float testFreq;

    for (int32_t f = 0; f < a->testFreqCount; f++) {
        rReal = rMatrixReal[f];
        rImag = rMatrixImag[f];
        testFreq = a->testFrequencies[f];
        for (int32_t n = 0; n < a->sampleCount; n++) {
            absValue = rReal[n] * rReal[n] + rImag[n] * rImag[n];
            if(absValue > sMax) {
                sMax = absValue;
                a->codePhase = a->sampleCount - n;
                a->dopplerFrequency = testFreq;
            }
        }
    }

    return sMax;
}

float estimatePIn(acquisitionInternal_t* acq) {
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

    float *rReal, *rImag;

    a->testFreqCount = testFreqCount;

    for (int i = 0; i < a->testFreqCount; i++) {
        a->testFrequencies[i] = testFrequencies[i];
    }

    float *xMatrixReal[a->testFreqCount];
    float *xMatrixImag[a->testFreqCount];

    float *rMatrixReal[a->testFreqCount];
    float *rMatrixImag[a->testFreqCount];

    for (int i = 0; i < a->testFreqCount; i++) {
        //printf("%i \n", i);
        float *rowXReal = malloc(a->sampleCount * sizeof(float));
        float *rowXImag = malloc(a->sampleCount * sizeof(float));
        float *rowRReal = malloc(a->sampleCount * sizeof(float));
        float *rowRImag = malloc(a->sampleCount * sizeof(float));
        xMatrixReal[i] = rowXReal;
        xMatrixImag[i] = rowXImag;
        rMatrixReal[i] = rowRReal;
        rMatrixImag[i] = rowRImag;
    }

    float sMax = 0.0;
    float pIn = 0.0;

    computeX(a, xMatrixReal, xMatrixImag);

    computeFourier(a, a->inputCodesReal, a->inputCodesImag, a->sampleCount);

    for(int32_t n = 0; n < a->sampleCount; n++) {
        a->inputCodesImag[n] = -a->inputCodesImag[n];
    }

    for(int32_t f = 0; f < testFreqCount; f++) {
        computeFourier(a, xMatrixReal[f], xMatrixImag[f], a->sampleCount);
    }
    
    computeR(a, xMatrixReal, xMatrixImag, rMatrixReal, rMatrixImag);

    for (int32_t f = 0; f < testFreqCount; f++) {
        computeInvFourier(a, rMatrixReal[f], rMatrixImag[f], a->sampleCount);
    }

    sMax = computeMaxValue(a, rMatrixReal, rMatrixImag, sMax);

    pIn = estimatePIn(a);

    bool result = ((sMax / pIn) > 0.015);

	return result; // return whether acquisition was achieved or not!
}