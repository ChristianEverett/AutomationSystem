#include <jni.h>
#include "pi_2_dht_read.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL
Java_com_pi_devices_asynchronousdevices_TemperatureSensor_readSensor(JNIEnv *env, jobject obj, jint pin, jint type)
{
	float temperature = 0, humidity = 0;
	int result;
	
	jclass sesnorReadingClass = env->FindClass("com/pi/devices/asynchronousdevices/TemperatureSensor$SensorReading");
	
	if(sesnorReadingClass == NULL)
		printf("jclass error");
	
	jmethodID constructor = env->GetMethodID(sesnorReadingClass, "<init>", "(Lcom/pi/devices/asynchronousdevices/TemperatureSensor;FF)V");
	
	while((result = pi_2_dht_read(type, pin, &humidity, &temperature)) != 0)
	{
		
	}
	
	jobject object = env->NewObject(sesnorReadingClass, constructor, obj, temperature, humidity);
	
	if(object == NULL)
		printf("jobject error");
	
	return object;
}

#ifdef __cplusplus
}
#endif
