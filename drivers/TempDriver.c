#include "TempDriver.h"
#include "pi_2_dht_read.h"

JNIEXPORT jobject JNICALL
Java_com_pi_devices_TemperatureSensor_readSensor(JNIEnv *env, jobject obj, jint pin, jint type)
{
	float temperature = 0, humidity = 0;
	int result;
	
	jclass sesnorReadingClass = (*env)->FindClass(env, "com/pi/devices/TemperatureSensor$SensorReading");
	
	if(sesnorReadingClass == NULL)
		printf("jclass error");
	
	jmethodID constructor = (*env)->GetMethodID(env, sesnorReadingClass, "<init>", "(Lcom/pi/devices/TemperatureSensor;FF)V");
	
	while((result = pi_2_dht_read(type, pin, &humidity, &temperature)) != 0)
	{
		
	}
	
	jobject object = (*env)->NewObject(env, sesnorReadingClass, constructor, obj, temperature, humidity);
	
	if(object == NULL)
		printf("jobject error");
	
	return object;
}
