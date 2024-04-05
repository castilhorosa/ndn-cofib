#include <stdio.h>
#include <net/ethernet.h>
#include <string.h>
#include <linux/if_packet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <netinet/ether.h>
#include <linux/if_packet.h>
#include <sys/ioctl.h>
#include <stdlib.h>
#include <math.h>
#include <unistd.h>
#include <time.h>
#include <pcap.h>
#include <netinet/in.h>
#include <netinet/if_ether.h>

void send_ethernet_frame(int sock_s, uint8_t *ether_header, uint8_t* payload, uint16_t packet_size);
void packet_handler(u_char *args, const struct pcap_pkthdr *header, const u_char *packet);

void do_some_task(long x) {
	while(x>0){
		x--;
	}
}

int main(int a, char *args[]){

    if(a == 2) {
	int time_interval = atoi(args[1]);
	char *device = "eth0";
	char error_buffer[PCAP_ERRBUF_SIZE];

	printf("Interface: %s\n", device);

        int sock_s;
        uint8_t *payload;
        uint16_t payload_size;

	//opening the raw socket...
        sock_s = socket(AF_PACKET, SOCK_RAW, ETH_P_ALL);
        if(sock_s < 0){
                printf("Socket can't be open!!\n");
		exit(1);
        }

        //destination mac address
        uint8_t* mac_d = (uint8_t*)malloc(sizeof(uint8_t)*6);
        mac_d[0] = 0xaa;
        mac_d[1] = 0xbb;
        mac_d[2] = 0xcc;
        mac_d[3] = 0xdd;
        mac_d[4] = 0xee;
        mac_d[5] = 0xff;

        //source mac address
        uint8_t* mac_s = (uint8_t*)malloc(sizeof(uint8_t)*6);
        mac_s[0] = 0x00;
        mac_s[1] = 0x00;
        mac_s[2] = 0x00;
        mac_s[3] = 0x00;
        mac_s[4] = 0x00;
        mac_s[5] = 0x01;

        //ether type
        uint8_t* ether_type = (uint8_t*)malloc(sizeof(uint8_t)*2);
        ether_type[0] = 0xff;
        ether_type[1] = 0xff;

	//ethernet header
	uint8_t* header = (uint8_t*)malloc(14);
	memset(header, 0, 14);
        memcpy(header, mac_d, 6);
        memcpy(header+6, mac_s, 6);
        memcpy(header+12, ether_type, 2);

	struct timeval start_time, end_time;
        long elapsed_time;


        struct timespec time1, time2;
        time1.tv_sec = 0;
        time1.tv_nsec = (time_interval*1000);


        FILE *traffic = fopen("packets.bin", "rb+");

        if(traffic != NULL) {
        	int count=0;
        	size_t bytesRead;
        	do {
        	    // Record the start time
                    //gettimeofday(&start_time, NULL);
        	    // Read 2 bytes from the file
                    bytesRead = fread(&payload_size, 1, sizeof(payload_size), traffic);
                    // Record the end time
                    //gettimeofday(&end_time, NULL);
                    // Calculate the elapsed time in milliseconds
                    //unsigned long long start = (start_time.tv_sec * 1000000 + start_time.tv_usec);
                    //unsigned long long end = (end_time.tv_sec * 1000000 + end_time.tv_usec);

                    //elapsed_time = end-start;
                    //printf("time to read a packet from file us: %ld\n", elapsed_time);
		    if(bytesRead>0){
		        //count++;
		        payload_size = ntohs(payload_size);

		        //printing the ethernet header
		        //printf("pkt %d, len %d: \n\t", count, 14+payload_size);
		        //for(int i=0; i<14; i++)
		    	//    printf("0x%.2x ", header[i]);
		    	//printf("\n\t");

		    	payload = (uint8_t *)malloc(payload_size);
		    	if(payload != NULL) {

		    	    // Record the start time
                            //gettimeofday(&start_time, NULL);
		    	    fread(payload, 1, payload_size, traffic);
		    	    // Record the end time
                            //gettimeofday(&end_time, NULL);
                             // Calculate the elapsed time in microseconds
                            //start = (start_time.tv_sec * 1000000 + start_time.tv_usec);
                            //end = (end_time.tv_sec * 1000000 + end_time.tv_usec);

                            //elapsed_time = end-start;

                            //printf("time to read a packet from file us: %ld\n", elapsed_time);


		    	    //printing the packet payload
		    	    /*int col=0;
		    	    for(int i=0; i<payload_size; i++) {
		    	       if(col < 14) {
		    	           printf("0x%.2x ", payload[i]);
		    	           col++;
		    	       }
		    	       else {
		    	           col=0;
		    	           printf("\n\t");
		    	       }
		    	    }*/



		    	    // Record the start time
                            //gettimeofday(&start_time, NULL);

		    	        send_ethernet_frame(sock_s, header, payload, payload_size);


		    	    // Record the end time
                            //gettimeofday(&end_time, NULL);
                             // Calculate the elapsed time in microseconds
                            //start = (start_time.tv_sec * 1000000 + start_time.tv_usec);
                            //end = (end_time.tv_sec * 1000000 + end_time.tv_usec);

                            //elapsed_time = end-start;

                            //printf("time to send packet in us: %ld\n", elapsed_time);
		    	    count++;
		    	    
		    	    if(count==160000)
		    	        break;

		    	    //int x = usleep(time_interval);
		    	    //if(x == -1){
		    	    //	printf("Error when calling ussleep function..");
		    	    //	return 0;
		    	    //}
		    	    //getchar();


                            //if(nanosleep(&time1, NULL) < 0 ) {
                            //    printf("Nano sleep system call failed \n");
                            //    return -1;
                            //}
		    	} else {
		    	    printf("Error when allocating memory...\n");
		    	    fclose(traffic);
		    	}
		    }
        	} while(bytesRead > 0);
        	printf("%d has been sent succesfully..\n", count);
        	fclose(traffic);
        } else {
        	printf("Traffic file packets.bin cant be opened!!");
        }
    } else {
    	printf("You must provide the 'packet time interval' argument..\n");
    }
    return 0;
}


void packet_handler(u_char *args, const struct pcap_pkthdr *header, const u_char *packet){
	printf("%s \n", packet);
}

void send_ethernet_frame(int sock_s, uint8_t *ether_header, uint8_t *payload, uint16_t payload_size) {

        //struct timeval start_time, end_time;
        //long elapsed_time;


	      struct ifreq if_index;
        struct sockaddr_ll socket_address;

        memset(&if_index, 0, sizeof(struct ifreq));
        strncpy(if_index.ifr_name, "eth0", IFNAMSIZ-1);

        //setting the interface index
        if(ioctl(sock_s, SIOCGIFINDEX, &if_index) < 0){
                perror("SIOCGIFINDEX");
        }

        /* Index of the network device */
        socket_address.sll_ifindex = if_index.ifr_ifindex;
        /* Address length*/
        socket_address.sll_halen = ETH_ALEN;

        int pdu_size = 14 + payload_size;
	      char sendbuf[pdu_size];
	      memset(sendbuf, 0, pdu_size);
	      memcpy(sendbuf, ether_header, 14);
        memcpy(sendbuf+14, payload, payload_size);

	// Record the start time
        //gettimeofday(&start_time, NULL);
        int status = sendto(sock_s, sendbuf, sizeof(sendbuf), 0, (struct sockaddr*)&socket_address, sizeof(struct sockaddr_ll));
        //do_some_task(247500);  2000 pps`
				do_some_task(260000);

        // Record the end time
        //gettimeofday(&end_time, NULL);
        // Calculate the elapsed time in milliseconds
        //unsigned long long start = (start_time.tv_sec * 1000000 + start_time.tv_usec);
        //unsigned long long end = (end_time.tv_sec * 1000000 + end_time.tv_usec);

        //elapsed_time = end-start;
        //printf("time to send a packet using the sendto function in us: %ld\n", elapsed_time);

        if(status < 0)
        {
                printf("Failed\n");
                exit(0);
        }

	//printf("Packet sent!!\n");
}
