/*
 * ARPScannerDriver.h
 *
 *  Created on: Jan 13, 2017
 *      Author: Christian Everett
 */

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>

#ifndef ARPSCANNERDRIVER_H_
#define ARPSCANNERDRIVER_H_

/* Header for class Test */

#ifdef __cplusplus
extern "C"
{
#endif
/*
 * Class:
 * Method:
 * Signature:
 */
JNIEXPORT void JNICALL Java_com_pi_devices_DeviceDetector_setupScanner(JNIEnv *, jobject);
JNIEXPORT void JNICALL Java_com_pi_devices_DeviceDetector_registerAddress(JNIEnv *, jobject, jstring);
JNIEXPORT jstring JNICALL Java_com_pi_devices_DeviceDetector_scan(JNIEnv *, jobject);
JNIEXPORT void JNICALL Java_com_pi_devices_DeviceDetector_stopScanning(JNIEnv *, jobject);

#ifdef __cplusplus
}

#endif

#endif /* ARPSCANNERDRIVER_H_ */