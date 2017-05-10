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
#include <vector>

void setupBluetooth();
const std::vector<std::string> bluetoothScan();
const std::string ping(const char* address);
void close();

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL
Java_com_pi_devices_asynchronousdevices_BluetoothAdapter_setupBluetooth(JNIEnv *env, jobject obj)
{
	setupBluetooth();
}

JNIEXPORT jobjectArray JNICALL
Java_com_pi_devices_asynchronousdevices_BluetoothAdapter_scanForBluetoothDevices(JNIEnv *env, jobject obj)
{
	std::vector<std::string> results = bluetoothScan();
	
	jobjectArray javaArray = (jobjectArray)env->NewObjectArray(results.size(),
							env->FindClass("java/lang/String"),
							env->NewStringUTF(""));
							
	for(int x = 0; x < results.size(); x++)
	{
		env->SetObjectArrayElement(javaArray, x, env->NewStringUTF(results.at(x).c_str()));
	}
	
	return javaArray;
}

JNIEXPORT jstring JNICALL
Java_com_pi_devices_asynchronousdevices_BluetoothAdapter_ping(JNIEnv *env, jobject obj, jstring mac)
{
	//std::string macString(env->GetStringUTFChars(mac, NULL));
	const char* macString = env->GetStringUTFChars(mac, NULL);
	
	std::string result = ping(macString);
	jstring javaString = env->NewStringUTF(result.c_str());
	env->ReleaseStringUTFChars(mac, macString);
	
	return javaString;
}

JNIEXPORT void JNICALL
Java_com_pi_devices_asynchronousdevices_BluetoothAdapter_closeBluetooth(JNIEnv *env, jobject obj)
{
	close();
}

#ifdef __cplusplus
}

#endif
