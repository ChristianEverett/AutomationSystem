#include "TempDriver.h"
#include "pi_2_dht_read.h"

JNIEXPORT jobject JNICALL
Java_com_pi_devices_TempatureSensor_readSensor(JNIEnv *env, jobject obj, jint pin)
{
	float temperature = 0, humidity = 0;
	int result;
	
	jclass sesnorReadingClass = (*env)->FindClass(env, "com/pi/devices/TempatureSensor$SensorReading");
	
	if(sesnorReadingClass == NULL)
		printf("jclass error");
	
	jmethodID constructor = (*env)->GetMethodID(env, sesnorReadingClass, "<init>", "(Lcom/pi/devices/TempatureSensor;FF)V");
	
	while((result = pi_2_dht_read(11, pin, &humidity, &temperature)) != 0)
	{
		
	}
	
	jobject object = (*env)->NewObject(env, sesnorReadingClass, constructor, obj, temperature, humidity);
	
	if(object == NULL)
		printf("jobject error");
	
	return object;
}
