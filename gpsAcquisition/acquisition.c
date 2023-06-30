
#include "acquisition.h"
#include <stdio.h> // apagar depois

#include <malloc.h>
#include <string.h>
#include <math.h>

#define F_S 2000000 // sample frequency of 2MHz
#define M 2048      // array size for chirp Z-transform (M >= samples * 2 - 1)
                    // assuming samples will always be 1000

typedef struct
{
    // final results
    int32_t codePhase;
    int32_t dopplerFrequency;
    // sample arrays and counter
    int32_t sampleCount;
    float *samplesReal;
    float *samplesImag;
    // code arrays and counter
    int32_t codesCount;
    float *inputCodesReal;
    float *inputCodesImag;
    // test freq array and counter
    int32_t testFreqCount;
    int32_t *testFrequencies;

    // array used in radix-2 algorithm
    float *data;
    // arrays used in complex convolution
    float *xr;
    float *xi;
    float *yr;
    float *yi;
    // arrays used in bluestein algorithm
    float *cosTable;
    float *sinTable;
    float *areal;
    float *aimag;
    float *breal;
    float *bimag;
    float *creal;
    float *cimag;
} acquisitionInternal_t;

acquisition_t *allocateAcquisition(int32_t nrOfSamples)
{
    acquisitionInternal_t *a = malloc(sizeof(acquisitionInternal_t));

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

    a->data = malloc(M * 2 * sizeof(float));

    a->xr = malloc(M * sizeof(float));
    a->xi = malloc(M * sizeof(float));
    a->yr = malloc(M * sizeof(float));
    a->yi = malloc(M * sizeof(float));

    a->cosTable = malloc(nrOfSamples * sizeof(float));
    a->sinTable = malloc(nrOfSamples * sizeof(float));
    a->areal = malloc(M * sizeof(float));
    a->aimag = malloc(M * sizeof(float));
    a->breal = malloc(M * sizeof(float));
    a->bimag = malloc(M * sizeof(float));
    a->creal = malloc(M * sizeof(float));
    a->cimag = malloc(M * sizeof(float));
    memset(a->areal, 0, M * sizeof(float));
    memset(a->aimag, 0, M * sizeof(float));
    memset(a->breal, 0, M * sizeof(float));
    memset(a->bimag, 0, M * sizeof(float));
    memset(a->creal, 0, M * sizeof(float));
    memset(a->cimag, 0, M * sizeof(float));

    return (acquisition_t *)a;
}

void deleteAcquisition(acquisition_t *acq)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;
    free(a->data);
    free(a->xr);
    free(a->xi);
    free(a->yr);
    free(a->yi);
    free(a->cosTable);
    free(a->sinTable);
    free(a->areal);
    free(a->aimag);
    free(a->breal);
    free(a->bimag);
    free(a->creal);
    free(a->cimag);

    // free also everything else that was allocated in [allocateAcquisition]
    free(a->samplesReal);
    free(a->samplesImag);

    free(a->inputCodesReal);
    free(a->inputCodesImag);

    free(a->testFrequencies);

    // after freeing all contained structures on heap, free acq itself
    free(acq);
}

void enterSample(acquisition_t *acq, float real, float imag)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;

    // put a sample-entry into the state in [a]

    a->samplesReal[a->sampleCount] = real;
    a->samplesImag[a->sampleCount] = imag;
    a->sampleCount += 1;
}

void enterCode(acquisition_t *acq, float real, float imag)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;

    // put a code-entry into the state in [a]

    a->inputCodesReal[a->codesCount] = real;
    a->inputCodesImag[a->codesCount] = imag;
    a->codesCount += 1;
}

void computeX(acquisitionInternal_t *a, float **xMatrixReal, float **xMatrixImag)
{
    float angle = 0.0;
    float cos = 0.0;
    float sin = 0.0;
    float multiplier = 2 * M_PI / F_S;
    float *xReal, *xImag;

    for (int32_t f = 0; f < a->testFreqCount; f++)
    {
        float freqMult = multiplier * a->testFrequencies[f];
        xReal = xMatrixReal[f];
        xImag = xMatrixImag[f];

        for (int32_t n = 0; n < a->sampleCount; n++)
        {
            angle = freqMult * n;
            cos = cosf(angle);
            sin = sinf(angle);

            xReal[n] = a->samplesReal[n] * cos + a->samplesImag[n] * sin;
            xImag[n] = -a->samplesReal[n] * sin + a->samplesImag[n] * cos;
        }
    }
}

void computeR(acquisitionInternal_t *a, float **xMatrixReal, float **xMatrixImag, float **rMatrixReal, float **rMatrixImag)
{
    float *rReal, *rImag;
    float *xReal, *xImag;

    for (int32_t f = 0; f < a->testFreqCount; f++)
    {
        rReal = rMatrixReal[f];
        rImag = rMatrixImag[f];
        xReal = xMatrixReal[f];
        xImag = xMatrixImag[f];

        for (int32_t n = 0; n < a->codesCount; n++)
        {
            rReal[n] = xReal[n] * a->inputCodesReal[n] - xImag[n] * a->inputCodesImag[n];
            rImag[n] = xReal[n] * a->inputCodesImag[n] + xImag[n] * a->inputCodesReal[n];
        }
    }
}

int reverseBits(int num, int levels) {
    int reverseNum = 0;
    for (int i = 0; i < levels; i++) {
        reverseNum <<= 1;
        if (num & 1)
            reverseNum |= 1;
        num >>= 1;
    }
    return reverseNum;
}

int flipArray(float *array, int i, int j) {
    float temp = array[i];
    array[i] = array[j];
    array[j] = temp;
}

void radix2(acquisitionInternal_t *a, float *real, float *imag, int isign)
{
    float wtemp, wr, wpr, wpi, wi, theta;
    float tempr, tempi;

    int n = M * 2;

    // perform bit reversal (according to "butterfly diagram"),
    // with the real part on the even indexes and the complex
    // part on the odd indexes
    for (int i = 0; i < M; i++)
    {
        int j = reverseBits(i, 11);
        if (j > i)
        {
            flipArray(real, i, j);
            flipArray(imag, i, j);
        }
    }

    // copy input data (real and imaginary parts) into a single array
    for (int i = 0, j = 0; i < n; i += 2, j++)
    {
        a->data[i] = real[j];
        a->data[i + 1] = imag[j];
    }

    // Danielson-Lanzcos routine
    int mmax = 2;
    // external loop
    while (n > mmax)
    {
        int istep = mmax << 1;
        theta = isign * (2 * M_PI / mmax);
        wtemp = sinf(0.5f * theta);
        wpr = -2.0f * wtemp * wtemp;
        wpi = sinf(theta);
        wr = 1.0f;
        wi = 0.0f;
        // internal loops
        for (int m = 1; m < mmax; m += 2)
        {
            for (int i = m; i <= n; i += istep)
            {
                int j = i + mmax;
                tempr = wr * a->data[j - 1] - wi * a->data[j];
                tempi = wr * a->data[j] + wi * a->data[j - 1];
                a->data[j - 1] = a->data[i - 1] - tempr;
                a->data[j] = a->data[i] - tempi;
                a->data[i - 1] += tempr;
                a->data[i] += tempi;
            }
            wtemp = wr;
            wr += wtemp * wpr - wi * wpi;
            wi += wi * wpr + wtemp * wpi;
        }
        mmax = istep;
    }

    for (int i = 0, j = 0; i < n; i += 2, j++)
    {
        real[j] = a->data[i];
        imag[j] = a->data[i + 1];
    }
}

/*
    performs the complex convolution of the two sequences used
    in the chirp Z-transform
*/
void complexConvolution(acquisitionInternal_t *a,
                        const float *xreal, const float *ximag,
                        const float *yreal, const float *yimag,
                        float *outreal, float *outimag)
{

    float temp = 0.0;
    int size = M * sizeof(float);

    memcpy(a->xr, xreal, size);
    memcpy(a->xi, ximag, size);
    memcpy(a->yr, yreal, size);
    memcpy(a->yi, yimag, size);

    // compute FFTs of arrays needed for convolution
    radix2(a, a->xr, a->xi, -1);
    radix2(a, a->yr, a->yi, -1);

    for (int i = 0; i < M; i++)
    {
        temp = a->xr[i] * a->yr[i] - a->xi[i] * a->yi[i];
        a->xi[i] = a->xi[i] * a->yr[i] + a->xr[i] * a->yi[i];
        a->xr[i] = temp;
    }

    // compute inverse FFT needed for convolution
    radix2(a, a->xr, a->xi, 1);

    // scale the result
    for (int i = 0; i < M; i++)
    {
        outreal[i] = a->xr[i] / M;
        outimag[i] = a->xi[i] / M;
    }
}

/*
    Bluestein's algorithm for calculating FFTs for an array
    of samples of arbitrary length, using a complex convolution
*/
void bluestein(acquisitionInternal_t *a, float *real, float *imag)
{
    int temp = 0;
    float angle = 0.0;

    // temporary arrays and preprocessing
    for (int i = 0, j = 1; i < a->sampleCount; i++, j++)
    {
        temp = (i * i) % (a->sampleCount * 2);
        angle = M_PI * temp / a->sampleCount;
        a->cosTable[i] = cosf(angle);
        a->sinTable[i] = sinf(angle);
        a->areal[i] = real[i] * a->cosTable[i] + imag[i] * a->sinTable[i];
        a->aimag[i] = -real[i] * a->sinTable[i] + imag[i] * a->cosTable[i];
    }

    a->breal[0] = a->cosTable[0];
    a->bimag[0] = a->sinTable[0];

    for (int i = 1; i < a->sampleCount; i++)
    {
        a->breal[i] = a->breal[M - i] = a->cosTable[i];
        a->bimag[i] = a->bimag[M - i] = a->sinTable[i];
    }

    // convolution
    complexConvolution(a, a->areal, a->aimag, a->breal, a->bimag, a->creal, a->cimag);

    // postprocessing
    for (int i = 0; i < a->sampleCount; i++)
    {
        real[i] = a->creal[i] * a->cosTable[i] + a->cimag[i] * a->sinTable[i];
        imag[i] = -a->creal[i] * a->sinTable[i] + a->cimag[i] * a->cosTable[i];
    }
}

/*
    Free FFT and convolution (C)
    Copyright (c) 2021 Project Nayuki. (MIT License)
    https://www.nayuki.io/page/free-small-fft-in-multiple-languages

*/
void fft(acquisitionInternal_t *a, float *real, float *imag)
{
    // if it is a power of two, use radix 2 algorithm
    if ((a->sampleCount & (a->sampleCount - 1)) == 0)
    {
        radix2(a, real, imag, -1);
    }
    // otherwise, use bluestein algorithm
    else
    {
        bluestein(a, real, imag);
    }
}

void inverseFft(acquisitionInternal_t *a, float *real, float *imag)
{
    fft(a, imag, real);
}

float computeMaxValue(acquisitionInternal_t *acq, float **rMatrixReal, float **rMatrixImag)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;

    float absValue = 0.0;
    float *rReal, *rImag;
    float testFreq;
    float sMax = 0.0;

    for (int32_t f = 0; f < a->testFreqCount; f++)
    {
        rReal = rMatrixReal[f];
        rImag = rMatrixImag[f];
        testFreq = a->testFrequencies[f];
        for (int32_t n = 0; n < a->sampleCount; n++)
        {
            absValue = rReal[n] * rReal[n] + rImag[n] * rImag[n];
            if (absValue > sMax)
            {
                sMax = absValue;
                a->codePhase = n;
                a->dopplerFrequency = testFreq;
            }
        }
    }
    return sMax;
}

float estimatePIn(acquisitionInternal_t *acq)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;
    float pIn = 0.0;

    for (int i = 0; i < a->sampleCount; i++)
    {
        pIn += a->samplesReal[i] * a->samplesReal[i] + a->samplesImag[i] * a->samplesImag[i];
    }

    return (pIn / a->sampleCount);
}

void scaleIdft(acquisitionInternal_t *acq, float* realR, float* imagR) {
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;
	for(int32_t n = 0; n < a->sampleCount; n++) {
        realR[n] /= (a->sampleCount * a->sampleCount);
        imagR[n] /= (a->sampleCount * a->sampleCount);
    }
}

__attribute__((noipa)) bool startAcquisition(acquisition_t *acq, int32_t testFreqCount, const int32_t *testFrequencies)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;

    float *rReal, *rImag;

    a->testFreqCount = testFreqCount;

    for (int i = 0; i < a->testFreqCount; i++)
    {
        a->testFrequencies[i] = testFrequencies[i];
    }

    float *xMatrixReal[a->testFreqCount];
    float *xMatrixImag[a->testFreqCount];

    float *rMatrixReal[a->testFreqCount];
    float *rMatrixImag[a->testFreqCount];

    for (int i = 0; i < a->testFreqCount; i++)
    {
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

    fft(a, a->inputCodesReal, a->inputCodesImag);
    for (int32_t n = 0; n < a->sampleCount; n++)
    {
        a->inputCodesImag[n] = -a->inputCodesImag[n];
    }

    for (int32_t f = 0; f < testFreqCount; f++)
    {
        fft(a, xMatrixReal[f], xMatrixImag[f]);
    }

    computeR(a, xMatrixReal, xMatrixImag, rMatrixReal, rMatrixImag);

    for (int32_t f = 0; f < testFreqCount; f++)
    {
        inverseFft(a, rMatrixReal[f], rMatrixImag[f]);
    }
    
    for (int32_t f = 0; f < testFreqCount; f++)
    {
        scaleIdft(a, rMatrixReal[f], rMatrixImag[f]);
    }
    

    sMax = computeMaxValue(a, rMatrixReal, rMatrixImag);

    pIn = estimatePIn(a);



    bool result = ((sMax / pIn) > 0.015);

    return result; // return whether acquisition was achieved or not!
}