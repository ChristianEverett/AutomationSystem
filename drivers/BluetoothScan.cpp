/*
 * bluetoothScan.cpp
 *
 *  Created on: Feb 15, 2017
 *      Author: Christian Everett
 */

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <vector>
#include <atomic>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

static int dev_id, sock;
static const int len = 10;
static std::atomic<bool> stop(false);

void setupBluetooth()
{
	stop = false;
	dev_id = hci_get_route(NULL);
	//int dev_id = hci_devid( "01:23:45:67:89:AB");
	sock = hci_open_dev(dev_id);
	if (dev_id < 0 || sock < 0)
	{
		perror("opening socket");
	}
}

const std::vector<std::string> bluetoothScan()
{
	std::vector<std::string> addresses;
	char addr[19] = { 0 };

	int max_rsp = 255;
	int flags = IREQ_CACHE_FLUSH;
	inquiry_info *ii = (inquiry_info*) malloc(max_rsp * sizeof(inquiry_info));

	int num_rsp = hci_inquiry(dev_id, len, max_rsp, NULL, &ii, flags);
	if (num_rsp < 0 || stop.load())
		return addresses;

	for (int i = 0; i < num_rsp; i++)
	{
		ba2str(&(ii + i)->bdaddr, addr);
		addresses.push_back(std::string(addr));
	}

	free(ii);
	return addresses;
}

const std::string ping(const char* address)
{
	char name[248] = { 0 };

	bdaddr_t *bluetoothAddress = new bdaddr_t;

	if(str2ba( address, bluetoothAddress ) < 0)
		return std::string();

	int result = hci_read_remote_name(sock, bluetoothAddress, sizeof(name), name, len);

	if(result < 0 || stop.load())
		return std::string();

	std::string deviceName(name);
	delete bluetoothAddress;

	return deviceName;
}

void close()
{
	stop = true;
	close(sock);
}
