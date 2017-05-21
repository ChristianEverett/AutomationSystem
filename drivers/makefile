CC=gcc
CXX=g++
CPPFLAGS=-g -std=c++11 -D_POSIX_C_SOURCE=199309L -Wall -fPIC -shared -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include/linux -I./
LDFLAGS=-g -shared
LDLIBS=-lpthread libpigpio.so libbluetooth.so libwiringPi.so

INSTALL_LOCATION=/lib

FILES=*.cpp *.c

install: $(FILES)
	$(CXX) $(CPPFLAGS) $(FILES) -o libAutomationDriver.so $(LDLIBS)
	sudo cp libAutomationDriver.so $(INSTALL_LOCATION)
build: $(FILES)
	$(CXX) $(CPPFLAGS) $(FILES) -o libAutomationDriver.so $(LDLIBS)
clean:
	rm libAutomationDriver.so