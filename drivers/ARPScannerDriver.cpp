/*
 * ARPScannerDriver.cpp
 *
 *  Created on: Jan 13, 2017
 *      Author: Christian Everett
 */

#include <jni.h>
#include <iostream>
#include <string>
#include <cstdlib>

void setup();
std::string scan();
void registerMACAddress(std::string address);
void shutdownScanner();
const char *name;

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL
Java_com_pi_devices_asynchronousdevices_DeviceDetector_setupScanner(JNIEnv *env, jobject obj)
{
	setup();
}

JNIEXPORT void JNICALL
Java_com_pi_devices_asynchronousdevices_DeviceDetector_registerAddress(JNIEnv *env, jobject obj, jstring address)
{
	name = env->GetStringUTFChars(address, NULL);
	registerMACAddress(name);
}

JNIEXPORT jstring JNICALL
Java_com_pi_devices_asynchronousdevices_DeviceDetector_scan(JNIEnv *env, jobject obj)
{
	std::string mac = scan();
	jstring result = env->NewStringUTF(mac.c_str());
	return result;
}

JNIEXPORT void JNICALL
Java_com_pi_devices_asynchronousdevices_DeviceDetector_stopScanning(JNIEnv *env, jobject obj)
{
	shutdownScanner();
}

#ifdef __cplusplus
}

#endif
