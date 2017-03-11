/*
 * ARPScannerDriver.cpp
 *
 *  Created on: Jan 13, 2017
 *      Author: Christian Everett
 */

#include "BluetoothDriver.h"
#include <iostream>
#include <string>
#include <cstdlib>
#include <vector>

void setupBluetooth();
const std::vector<std::string> bluetoothScan();
const std::string ping(const std::string address);
void close();

JNIEXPORT void JNICALL
Java_com_pi_devices_BluetoothAdapter_setupBluetooth(JNIEnv *env, jobject obj)
{
	setupBluetooth();
}

JNIEXPORT jobjectArray JNICALL
Java_com_pi_devices_BluetoothAdapter_scanForBluetoothDevices(JNIEnv *env, jobject obj)
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
Java_com_pi_devices_BluetoothAdapter_ping(JNIEnv *env, jobject obj, jstring mac)
{
	std::string macString(env->GetStringUTFChars(mac, NULL));
	
	std::string result = ping(macString);
	jstring javaString = env->NewStringUTF(result.c_str());
	//env->ReleaseStringUTFChars(mac, macString.c_str());
	
	return javaString;
}

JNIEXPORT void JNICALL
Java_com_pi_devices_BluetoothAdapter_closeBluetooth(JNIEnv *env, jobject obj)
{
	close();
}
