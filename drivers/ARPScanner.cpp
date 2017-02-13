/*
 * ARPScanner.c
 *
 *  Created on: Jan 11, 2017
 *      Author: Christian Everett
 */

#define _BSD_SOURCE

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <linux/ip.h>
#include <linux/udp.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <net/if.h>
#include <netinet/ether.h>
#include <stdbool.h>
#include <unistd.h>
#include <list>
#include <map>
#include <utility>
#include <string>
#include <iostream>
#include <stdexcept>
#include <atomic>

#define DEFAULT_INTERFACE	"eth0"
#define MAX_ETHERNET_FRAME_SIZE	1530

const std::string lookForMACAddresses(const struct ether_header *);
void string_to_mac(std::string const& s, u_int8_t*);
void shutdownScanner();

std::map<unsigned long long, std::string> macAddresses;
std::atomic<bool> stop(false);

static int sockfd;
static ssize_t numbytes;
static uint8_t buf[MAX_ETHERNET_FRAME_SIZE];
/* Header structures */
static struct ether_header *ethernetHeader = (struct ether_header *) buf;
//static struct iphdr *ipHeader = (struct iphdr *) (buf + sizeof(struct ether_header));
//static struct udphdr *udpHeader = (struct udphdr *) (buf + sizeof(struct iphdr) + sizeof(struct ether_header));

union AddressNumber
{
	u_int8_t address[ETH_ALEN];
	unsigned long long number;
};

void setup()
{
	//char sender[INET6_ADDRSTRLEN];
	int sockopt;

	struct ifreq networkInterfaceOptions; /* set promiscuous mode */
	struct ifreq if_ip; /* get ip addr */
	//struct sockaddr_storage their_addr;

	memset(&if_ip, 0, sizeof(struct ifreq));

	/* Open PF_PACKET socket, listening for EtherType (filter out non ARP packets) */
	if ((sockfd = socket(PF_PACKET, SOCK_RAW, htons(ETHERTYPE_ARP))) == -1)
	{
		perror("listener: socket");
		return;
	}

	/* Set interface to promiscuous mode - do we need to do this every time? */
	strncpy(networkInterfaceOptions.ifr_name, DEFAULT_INTERFACE, IFNAMSIZ - 1);
	ioctl(sockfd, SIOCGIFFLAGS, &networkInterfaceOptions);
	networkInterfaceOptions.ifr_flags |= IFF_PROMISC;
	ioctl(sockfd, SIOCSIFFLAGS, &networkInterfaceOptions);

	/* Allow the socket to be reused - incase connection is closed prematurely */
	if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &sockopt, sizeof sockopt) == -1)
	{
		perror("setsockopt");
		close(sockfd);
		exit(EXIT_FAILURE);
	}

	/* Bind to device */
	if (setsockopt(sockfd, SOL_SOCKET, SO_BINDTODEVICE, DEFAULT_INTERFACE, IFNAMSIZ - 1) == -1)
	{
		perror("SO_BINDTODEVICE");
		close(sockfd);
		exit(EXIT_FAILURE);
	}
}

void shutdownScanner()
{
	stop = true;
	close(sockfd);
}

void registerMACAddress(std::string stringAddress)
{
	u_int8_t address[ETH_ALEN];
	string_to_mac(stringAddress, address);

	AddressNumber addressNumber;

	for (int x = 0; x < ETH_ALEN; x++)
	{
		addressNumber.address[ETH_ALEN - x - 1] = address[x];
	}

	std::pair<unsigned long long, std::string> newPair(addressNumber.number, stringAddress);
	macAddresses.insert(newPair);
}

const std::string scan()
{
	std::string mac;

	do
	{
		numbytes = recvfrom(sockfd, buf, MAX_ETHERNET_FRAME_SIZE, 0, NULL, NULL);

		if (stop)
			return "";

		try
		{
			mac = lookForMACAddresses(ethernetHeader);
			stop = true;
		}
		catch (const std::out_of_range& e)
		{
		}
	}
	while (!stop);

	stop = false;

	return mac;
}

const std::string lookForMACAddresses(const struct ether_header *ethernetHeader)
{
	AddressNumber addressNumber;

	for (int x = 0; x < ETH_ALEN; x++)
	{
		addressNumber.address[ETH_ALEN - x - 1] = ethernetHeader->ether_shost[x];
	}

	std::string stringAddress = macAddresses.at(addressNumber.number);

	return stringAddress;
}

void string_to_mac(std::string const& s, u_int8_t* a)
{
	unsigned int last = -1;
	unsigned int rc = sscanf(s.c_str(), "%x:%x:%x:%x:%x:%x%n", a + 0, a + 1, a + 2, a + 3, a + 4, a + 5, &last);

	if (rc != 6 || s.size() != last)
		throw std::runtime_error("invalid mac address format " + s);
}
