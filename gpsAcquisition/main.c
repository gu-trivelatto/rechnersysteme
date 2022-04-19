
#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include "testData.h"
#include "acquisition.h"

volatile int32_t configTestCaseId = 0;
volatile int32_t configNrOfSamples = 1000;


int main(int argc, char ** argv) {
    int32_t testCaseId = configTestCaseId;
    int32_t nrOfSamples = configNrOfSamples;

    acquisition_t * acq = allocateAcquisition(nrOfSamples);

    testCase_t* testCase = &testCases[testCaseId];
    for(int i = 0; i < 2*nrOfSamples; i+=2){
        float real = testCase->inputSamples[i];
        float imag = testCase->inputSamples[i+1];
        enterSample(acq, real, imag);
    }

    float* inputCodes = testCase->inputCodes;
    for(int i = 0; i < 2*nrOfSamples; i+=2){
        float real = inputCodes[i];
        float imag = inputCodes[i+1];
        enterCode(acq, real, imag);
    }

    bool refAcquisition = testCase->acquisition;
    int32_t refCodePhase = testCase->codePhase;
    int32_t refDopplerFreq = testCase->dopplerFrequency;

    printf("Starting acquisition...\n");
    bool acquisitionResult = startAcquisition(acq, testCase->testFrequencies);

    int32_t codePhase = acq->codePhase;
    int32_t dopplerFreq = acq->dopplerFrequency;
    bool passed = acquisitionResult == refAcquisition && codePhase == refCodePhase && dopplerFreq == refDopplerFreq;

    puts((passed?"PASSED":"FAILED"));
    puts("\n");
    if(!passed){
        printf("Expected %d acquistion\n", refAcquisition);
        printf("Code Phase = %d\n", refCodePhase);
        printf("Doppler    = %d\n", refDopplerFreq);

        printf("Got %d acquisitionResult\n", acquisitionResult);
        printf("Code Phase = %d\n", codePhase);
        printf("Doppler    = %d\n", dopplerFreq);
    }

    deleteAcquisition(acq);

    return passed? 1 : 0;
}