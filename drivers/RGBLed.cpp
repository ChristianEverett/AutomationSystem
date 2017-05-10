/*
 * RGBLed.cpp
 *
 *  Created on: Apr 28, 2017
 *      Author: Christian Everett
 */

#include "RGBLed.h"

std::atomic<int> RGBLed::references(0);

RGBLed::RGBLed(int redPin, int greenPin, int bluePin)
{
	if (RGBLed::references == 0 && gpioInitialise() < 0)
	{
	   perror("Failed to initialize gpio");
	}

	RGBLed::references++;

	_redPin = redPin;
	_greenPin = greenPin;
	_bluePin = bluePin;

	gpioSetMode(redPin, PI_ALT5);
	gpioSetMode(greenPin, PI_ALT5);
	gpioSetMode(bluePin, PI_ALT5);
}

RGBLed::~RGBLed()
{
	RGBLed::references--;

	if(RGBLed::references == 0)
		gpioTerminate();
}

void RGBLed::setPWM(int red, int green, int blue)
{
	gpioPWM(_redPin, red);
	gpioPWM(_greenPin, green);
	gpioPWM(_bluePin, blue);
}

