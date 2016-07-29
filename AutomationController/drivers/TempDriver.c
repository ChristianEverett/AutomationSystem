#include "TempDriver.h"
#include "pi_2_dht_read.h"

JNIEXPORT jdouble JNICALL
Java_com_pi_devices_TempatureSensor_readSensor(JNIEnv *env, jobject obj, jint pin)
{
	float temperature = 0, humidity = 0;
	int result;
	
	while((result = pi_2_dht_read(11, pin, &humidity, &temperature)) != 0)
	{
		
	}
	
	return (jdouble)temperature;
}
