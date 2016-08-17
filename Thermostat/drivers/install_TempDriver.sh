sudo gcc -std=gnu99 -D_POSIX_C_SOURCE=199309L -Wall -shared -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include/linux TempDriver.c pi_2_dht_read.c common_dht_read.c pi_2_mmio.c -fPIC -o libTempDriver.so
sudo cp libTempDriver.so /lib
#java -XshowSettings:properties