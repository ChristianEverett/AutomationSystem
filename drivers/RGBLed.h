/*
 * RGBDriver.h
 *
 *  Created on: Apr 28, 2017
 *      Author: Christian Everett
 */

#ifndef RGBLED_H_
#define RGBLED_H_

#include <stdio.h>
#include <pigpio.h>
#include <atomic>
#include <wiringPi.h>
#include <softPwm.h>

class RGBLed
{
public:
	RGBLed(int, int, int);
	virtual ~RGBLed();

	void setPWM(int, int, int);
private:
	int _redPin, _greenPin, _bluePin;
	static std::atomic<int> references;
};

#endif /* RGBLED_H_ */


