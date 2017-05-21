/*
 * PWMDriver.cpp
 *
 *  Created on: Jan 13, 2017
 *      Author: Christian Everett
 */

#include <iostream>
#include <string>
#include <cstdlib>
#include <map>
#include <utility>
#include <jni.h>
#include "RCSwitch.h"

static RCSwitch mySwitch = RCSwitch();

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL
Java_com_pi_devices_Outlet_initilizeIR(JNIEnv *env, jobject obj, jint pulseLength, jint PIN)
{
	 mySwitch.setPulseLength(pulseLength);
	 mySwitch.enableTransmit(PIN);
}

JNIEXPORT void JNICALL
Java_com_pi_devices_Outlet_send(JNIEnv *env, jobject obj, jint code)
{
	 mySwitch.send(code, 24);
}

#ifdef __cplusplus
}

#endif
