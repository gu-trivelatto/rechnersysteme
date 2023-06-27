
#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include "testData.h"
#include "acquisition.h"
#include <stdlib.h>
#include <errno.h>

void loadSamplesAndCodes(acquisition_t* acq, const testCase_t* testCase, int32_t nrOfSamples);

/**
 * All Args optional.
 * If present argv[1] = testCaseId
 * If present argv[2] = nrOfSamples
 */
int main(int argc, char ** argv) {
	int32_t testCaseId = 2; // Default Test-Case without any args

	if (argc >= 2) {
		testCaseId = strtol(argv[1], NULL, 10);
	}

	const testCase_t* testCase = getTestCase(testCaseId);

	int32_t nrOfSamples;
	if (argc >= 3) {
		nrOfSamples = strtol(argv[2], NULL, 10);
		printf("Overriding to %d samples\n", nrOfSamples);
	} else {
		nrOfSamples = testCase->complexSampleCount;
	}

    acquisition_t * acq = allocateAcquisition(nrOfSamples);

    loadSamplesAndCodes(acq, testCase, nrOfSamples);

    bool refAcquisition = testCase->acquisition;
    int32_t refCodePhase = testCase->codePhase;
    int32_t refDopplerFreq = testCase->dopplerFrequency;

    printf("Starting acquisition...\n");
    bool acquisitionResult = startAcquisition(acq, testCase->testFreqCount, testCase->testFrequencies);

    int32_t codePhase = acq->codePhase;
    int32_t dopplerFreq = acq->dopplerFrequency;
    bool passed = acquisitionResult == refAcquisition && codePhase == refCodePhase && dopplerFreq == refDopplerFreq;

    puts((passed?"PASSED":"FAILED"));
    puts("\n");
    if(!passed){
        printf("Expected acquistion: %d\n", refAcquisition);
        printf("Code Phase = %d\n", refCodePhase);
        printf("Doppler    = %d\n", refDopplerFreq);

        printf("Got acquisitionResult: %d\n", acquisitionResult);
        printf("Code Phase = %d\n", codePhase);
        printf("Doppler    = %d\n", dopplerFreq);
    }

    deleteAcquisition(acq);

    return passed? 0 : 1;
}

__attribute__((noipa)) // so it never gets inlined and remains as identifiable kernel
void loadSamplesAndCodes(acquisition_t* acq, const testCase_t* testCase, int32_t nrOfSamples) {
    for(int i = 0; i < 2*nrOfSamples; i+=2){
        float real = testCase->inputSamples[i];
        float imag = testCase->inputSamples[i+1];
        enterSample(acq, real, imag);
    }

    for(int i = 0; i < 2*nrOfSamples; i+=2){
        float real = testCase->inputCodes[i];
        float imag = testCase->inputCodes[i+1];
        enterCode(acq, real, imag);
    }
}