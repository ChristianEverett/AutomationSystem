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
#include <string>
#include <stdexcept>

#define DEFAULT_INTERFACE	"eth0"
#define MAX_ETHERNET_FRAME_SIZE	1530

std::string lookForMACAddresses(const struct ether_header *);
void string_to_mac(std::string const& s, u_int8_t*);
void shutdownScanner();

std::list<std::string> macAddresses;

static int sockfd;
static ssize_t numbytes;
static uint8_t buf[MAX_ETHERNET_FRAME_SIZE];
/* Header structures */
static struct ether_header *ethernetHeader = (struct ether_header *) buf;
static struct iphdr *ipHeader = (struct iphdr *) (buf + sizeof(struct ether_header));
static struct udphdr *udpHeader = (struct udphdr *) (buf + sizeof(struct iphdr) + sizeof(struct ether_header));

void setup()
{
		char sender[INET6_ADDRSTRLEN];
		int ret, i;
		int sockopt;

		struct ifreq networkInterfaceOptions; /* set promiscuous mode */
		struct ifreq if_ip; /* get ip addr */
		struct sockaddr_storage their_addr;

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
	close(sockfd);
}

void registerMACAddress(std::string address)
{
	macAddresses.push_back(address);
}

std::string scan()
{
	numbytes = recvfrom(sockfd, buf, MAX_ETHERNET_FRAME_SIZE, 0, NULL, NULL);

	return lookForMACAddresses(ethernetHeader);
}

std::string lookForMACAddresses(const struct ether_header *ethernetHeader)
{
	const int MAC_BYTES = 6;
	bool addressFound;
	u_int8_t address[ETH_ALEN];

	for (auto iter = macAddresses.begin(); iter != macAddresses.end(); iter++)
	{
		addressFound = true;
		string_to_mac(*iter, address);

		for (int y = 0; y < MAC_BYTES; y++)
		{
			if (ethernetHeader->ether_shost[y] != address[y])
			{
				addressFound = false;
				break;
			}
		}
		if (addressFound)
		{
			return *iter;
		}
	}

	return "";
}

void string_to_mac(std::string const& s, u_int8_t* a)
{
	int last = -1;
	int rc = sscanf(s.c_str(), "%x:%x:%x:%x:%x:%x%n", a + 0, a + 1, a + 2, a + 3, a + 4, a + 5, &last);
	if (rc != 6 || s.size() != last)
		throw std::runtime_error("invalid mac address format " + s);
}
