
#include "acquisition.h"
#include <stdio.h> // apagar depois

#include <malloc.h>
#include <string.h>
#include <math.h>

#define F_S 2000000 // sample frequency of 2MHz
#define M 2048      // array size for chirp Z-transform (M >= samples * 2 - 1)
                    // assuming samples will always be 1000
#define N_SAMPLES 1000
#define N_FREQ 4

// complex variable structure
typedef struct
{
    float Re;
    float Im;
} complex;

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

    // arrays for performing the acquisition algorithm itself
    float (*xMatrixReal)[N_SAMPLES];
    float (*xMatrixImag)[N_SAMPLES];
    float (*rMatrixReal)[N_SAMPLES];
    float (*rMatrixImag)[N_SAMPLES];

    // array used in radix-2 algorithm
    float *data;

    // arrays used in complex convolution
    float *xrBuffer;
    float *xiBuffer;
    float *yrBuffer;
    float *yiBuffer;
    
    // arrays used in bluestein algorithm
    float *cosTable;
    float *sinTable;
    float *oneRTemp;
    float *oneITemp;
    float *twoRTemp;
    float *twoITemp;
    float *threeRTemp;
    float *threeITemp;
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

    a->xMatrixReal = malloc(sizeof(float[N_FREQ][nrOfSamples]));
    a->xMatrixImag = malloc(sizeof(float[N_FREQ][nrOfSamples]));
    a->rMatrixReal = malloc(sizeof(float[N_FREQ][nrOfSamples]));
    a->rMatrixImag = malloc(sizeof(float[N_FREQ][nrOfSamples]));
    memset(a->xMatrixReal, 0, (N_FREQ * nrOfSamples) * sizeof(float));
    memset(a->xMatrixImag, 0, (N_FREQ * nrOfSamples) * sizeof(float));
    memset(a->rMatrixReal, 0, (N_FREQ * nrOfSamples) * sizeof(float));
    memset(a->rMatrixImag, 0, (N_FREQ * nrOfSamples) * sizeof(float));

    a->data = malloc(M * 2 * sizeof(float));

    a->xrBuffer = malloc(M * sizeof(float));
    a->xiBuffer = malloc(M * sizeof(float));
    a->yrBuffer = malloc(M * sizeof(float));
    a->yiBuffer = malloc(M * sizeof(float));

    a->cosTable = malloc(nrOfSamples * sizeof(float));
    a->sinTable = malloc(nrOfSamples * sizeof(float));
    a->oneRTemp = malloc(M * sizeof(float));
    a->oneITemp = malloc(M * sizeof(float));
    a->twoRTemp = malloc(M * sizeof(float));
    a->twoITemp = malloc(M * sizeof(float));
    a->threeRTemp = malloc(M * sizeof(float));
    a->threeITemp = malloc(M * sizeof(float));
    memset(a->oneRTemp, 0, M * sizeof(float));
    memset(a->oneITemp, 0, M * sizeof(float));
    memset(a->twoRTemp, 0, M * sizeof(float));
    memset(a->twoITemp, 0, M * sizeof(float));
    memset(a->threeRTemp, 0, M * sizeof(float));
    memset(a->threeITemp, 0, M * sizeof(float));

    return (acquisition_t *)a;
}

void deleteAcquisition(acquisition_t *acq)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;
    free(a->data);
    free(a->xrBuffer);
    free(a->xiBuffer);
    free(a->yrBuffer);
    free(a->yiBuffer);
    free(a->cosTable);
    free(a->sinTable);
    free(a->oneRTemp);
    free(a->oneITemp);
    free(a->twoRTemp);
    free(a->twoITemp);
    free(a->threeRTemp);
    free(a->threeITemp);

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

void computeX(acquisitionInternal_t *a)
{
    float angle = 0.0;
    float cos = 0.0;
    float sin = 0.0;
    float multiplier = 2 * M_PI / F_S;
    float *xReal, *xImag;

    for (int32_t f = 0; f < a->testFreqCount; f++)
    {
        float freqMult = multiplier * a->testFrequencies[f];
        xReal = a->xMatrixReal[f];
        xImag = a->xMatrixImag[f];

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

void computeR(acquisitionInternal_t *a)
{
    float *rReal, *rImag;
    float *xReal, *xImag;

    for (int32_t f = 0; f < a->testFreqCount; f++)
    {
        rReal = a->rMatrixReal[f];
        rImag = a->rMatrixImag[f];
        xReal = a->xMatrixReal[f];
        xImag = a->xMatrixImag[f];

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

void swapArray(float *array, int i, int j) {
    float temp = array[i];
    array[i] = array[j];
    array[j] = temp;
}

// Perform bit reversal on the input arrays
void bitReversal(float *real, float *imag) {
    for (int i = 0; i < M; i++) {
        int j = reverseBits(i, 11); // Calculate the reversed index
        if (j > i) {
            swapArray(real, i, j);  // Swap real values
            swapArray(imag, i, j);  // Swap imaginary values
        }
    }
}

// Concatenate the real and imaginary arrays into a single array
void concatArrays(float *target, float *real, float *imag, int n) {
    for (int i = 0, j = 0; i < n; i += 2, j++) {
        target[i] = real[j];      // Store real values
        target[i + 1] = imag[j];  // Store imaginary values
    }
}

// Separate the concatenated array into real and imaginary arrays
void separateArrays(float *targetReal, float *targetImag, float *source, int n) {
    for (int i = 0, j = 0; i < n; i += 2, j++) {
        targetReal[j] = source[i];      // Retrieve real values
        targetImag[j] = source[i + 1];  // Retrieve imaginary values
    }
}

void radix2(acquisitionInternal_t *a, float *real, float *imag, int isign)
{
    int n = M * 2;

    float wtemp, wr, wpr, wpi, wi, theta, tempr, tempi;

    // Perform bit reversal on the input arrays
    bitReversal(real, imag);

    // Copy input data (real and imaginary parts) into a single array
    concatArrays(a->data, real, imag, n);

    int mmax = 2;
    // External loop
    while (n > mmax)
    {
        int istep = mmax << 1;
        theta = isign * (2 * M_PI / mmax);
        wtemp = sinf(0.5f * theta);
        wpr = -2.0f * wtemp * wtemp;
        wpi = sinf(theta);
        wr = 1.0f;
        wi = 0.0f;
        // Internal loops
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

    // Separate the concatenated array into real and imaginary arrays
    separateArrays(real, imag, a->data, n);
}

// Performs complex convolution of two sequences used in the chirp Z-transform.
void complexConvolution(acquisitionInternal_t *a,
                        const float *xreal, const float *ximag,
                        const float *yreal, const float *yimag,
                        float *outreal, float *outimag)
{
    float temp = 0.0;
    int size = M * sizeof(float);

    // Copy input arrays to internal buffers
    memcpy(a->xrBuffer, xreal, size);
    memcpy(a->xiBuffer, ximag, size);
    memcpy(a->yrBuffer, yreal, size);
    memcpy(a->yiBuffer, yimag, size);

    // Compute FFTs of arrays needed for convolution
    radix2(a, a->xrBuffer, a->xiBuffer, -1); // Forward FFT for x
    radix2(a, a->yrBuffer, a->yiBuffer, -1); // Forward FFT for y

    // Perform complex multiplication
    for (int i = 0; i < M; i++)
    {
        temp = a->xrBuffer[i] * a->yrBuffer[i] - a->xiBuffer[i] * a->yiBuffer[i];
        a->xiBuffer[i] = a->xiBuffer[i] * a->yrBuffer[i] + a->xrBuffer[i] * a->yiBuffer[i];
        a->xrBuffer[i] = temp;
    }

    // Compute inverse FFT needed for convolution
    radix2(a, a->xrBuffer, a->xiBuffer, 1); // Inverse FFT for the result

    // Scale the result
    for (int i = 0; i < M; i++)
    {
        outreal[i] = a->xrBuffer[i] / M;
        outimag[i] = a->xiBuffer[i] / M;
    }
}


// Bluestein's algorithm for calculating FFTs for an array
// of samples of arbitrary length, using a complex convolution
void bluestein(acquisitionInternal_t *a, float *real, float *imag)
{
    int temp = 0;
    float angle = 0.0;

    // Temporary arrays and preprocessing
    for (int i = 0, j = 1; i < a->sampleCount; i++, j++)
    {
        temp = (i * i) % (a->sampleCount * 2);
        angle = M_PI * temp / a->sampleCount;
        a->cosTable[i] = cosf(angle); // Compute cosine values
        a->sinTable[i] = sinf(angle); // Compute sine values
        a->oneRTemp[i] = real[i] * a->cosTable[i] + imag[i] * a->sinTable[i];    // Apply rotation to real part
        a->oneITemp[i] = -real[i] * a->sinTable[i] + imag[i] * a->cosTable[i];   // Apply rotation to imaginary part
    }

    a->twoRTemp[0] = a->cosTable[0];   // Set first element of b to cosine value
    a->twoITemp[0] = a->sinTable[0];   // Set first element of b to sine value

    // Precompute cosine and sine values for the complex weights
    for (int i = 1; i < a->sampleCount; i++)
    {
        a->twoRTemp[i] = a->twoRTemp[M - i] = a->cosTable[i];   // Set cosine values symmetrically in b
        a->twoITemp[i] = a->twoITemp[M - i] = a->sinTable[i];   // Set sine values symmetrically in b
    }

    // Perform complex convolution
    complexConvolution(a, a->oneRTemp, a->oneITemp, a->twoRTemp, a->twoITemp, a->threeRTemp, a->threeITemp);

    // Postprocessing
    for (int i = 0; i < a->sampleCount; i++)
    {
        real[i] = a->threeRTemp[i] * a->cosTable[i] + a->threeITemp[i] * a->sinTable[i];    // Apply inverse rotation to real part
        imag[i] = -a->threeRTemp[i] * a->sinTable[i] + a->threeITemp[i] * a->cosTable[i];   // Apply inverse rotation to imaginary part
    }
}

// Perform the Fast Fourier Transform (FFT) on the given arrays
void fft(acquisitionInternal_t *a, float *real, float *imag)
{
    // If the sample count is a power of two
    if ((a->sampleCount & (a->sampleCount - 1)) == 0)
    {
        radix2(a, real, imag, -1);
    }
    // If the sample count is arbitrary
    else
    {
        bluestein(a, real, imag);
    }
}
void inverseFft(acquisitionInternal_t *a, float *real, float *imag)
{
    fft(a, imag, real);
}

float computeMaxValue(acquisitionInternal_t *acq)
{
    acquisitionInternal_t *a = (acquisitionInternal_t *)acq;

    float absValue = 0.0;
    float *rReal, *rImag;
    float testFreq;
    float sMax = 0.0;

    for (int32_t f = 0; f < a->testFreqCount; f++)
    {
        rReal = a->rMatrixReal[f];
        rImag = a->rMatrixImag[f];
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

float estimatePIn(acquisitionInternal_t *a)
{
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

    float sMax = 0.0;
    float pIn = 0.0;

    computeX(a);

    fft(a, a->inputCodesReal, a->inputCodesImag);
    for (int32_t n = 0; n < a->sampleCount; n++)
    {
        a->inputCodesImag[n] = -a->inputCodesImag[n];
    }

    for (int32_t f = 0; f < testFreqCount; f++)
    {
        fft(a, a->xMatrixReal[f], a->xMatrixImag[f]);
    }

    computeR(a);

    for (int32_t f = 0; f < testFreqCount; f++)
    {
        inverseFft(a, a->rMatrixReal[f], a->rMatrixImag[f]);
    }
    
    for (int32_t f = 0; f < testFreqCount; f++)
    {
        scaleIdft(a, a->rMatrixReal[f], a->rMatrixImag[f]);
    }
    

    sMax = computeMaxValue(a);

    pIn = estimatePIn(a);



    bool result = ((sMax / pIn) > 0.015);

    return result; // return whether acquisition was achieved or not!
}