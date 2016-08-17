#include "TempDriver.h"
#include "pi_2_dht_read.h"

JNIEXPORT jobject JNICALL
Java_com_thermostat_Thermostat_readSensor(JNIEnv *env, jobject obj)
{
	int pin = 12;
	float temperature = 0, humidity = 0;
	int result;
	
	jclass sesnorReadingClass = (*env)->FindClass(env, "com/thermostat/Thermostat$SensorReading");
	
	if(sesnorReadingClass == NULL)
		printf("jclass error");
	
	jmethodID constructor = (*env)->GetMethodID(env, sesnorReadingClass, "<init>", "(Lcom/thermostat/Thermostat;FF)V");
	
	if(constructor == NULL)
		printf("jmethodID error");
	
	while((result = pi_2_dht_read(22, pin, &humidity, &temperature)) != 0)
	{

	}
	
	jobject object = (*env)->NewObject(env, sesnorReadingClass, constructor, obj, temperature, humidity);
	
	if(object == NULL)
		printf("jobject error");
	
	return object;
}
