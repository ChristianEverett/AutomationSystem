/*
 * PWMDriver.cpp
 *
 *  Created on: Jan 13, 2017
 *      Author: Christian Everett
 */

#include "RGBLed.h"
#include <iostream>
#include <string>
#include <cstdlib>
#include <map>
#include <utility>
#include <jni.h>

std::map<int, RGBLed*> nameToDriver;

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL
Java_com_pi_devices_Led_initializeRGB(JNIEnv *env, jobject obj, jint id, jint redPin, jint greenPin, jint bluePin)
{
	std::pair<int, RGBLed*> mapEntry(id, new RGBLed(redPin, greenPin, bluePin));
	nameToDriver.insert(mapEntry);
}

JNIEXPORT void JNICALL
Java_com_pi_devices_Led_setRGBPWM(JNIEnv *env, jobject obj, jint id, jint red, jint green, jint blue)
{
	try
	{
		RGBLed* led = nameToDriver.at(id);
		led->setPWM(red, green, blue);
	}
	catch(const std::out_of_range& e)
	{

	}
}

JNIEXPORT void JNICALL
Java_com_pi_devices_Led_closeRGB(JNIEnv *env, jobject obj, jint id)
{
	try
	{
		delete nameToDriver.at(id);
		nameToDriver.erase(id);
	}
	catch(const std::out_of_range& e)
	{

	}
}

#ifdef __cplusplus
}

#endif
