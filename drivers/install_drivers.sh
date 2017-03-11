sudo gcc -std=gnu99 -D_POSIX_C_SOURCE=199309L -Wall -shared -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include/linux TempDriver.c pi_2_dht_read.c common_dht_read.c pi_2_mmio.c -fPIC -o libTempDriver.so
sudo g++ -std=c++11 -D_POSIX_C_SOURCE=199309L -Wall -shared -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include/linux ARPScannerDriver.cpp ARPScanner.cpp -fPIC -o libARPDriver.so
sudo g++ -std=c++11 -D_POSIX_C_SOURCE=199309L -Wall -shared -I./ -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include -I/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/include/linux BluetoothDriver.cpp BluetoothScan.cpp -fPIC -o libBluetoothDriver.so libbluetooth.so
sudo cp libTempDriver.so /lib
sudo cp libARPDriver.so /lib
sudo cp libBluetoothDriver.so /lib
#java -XshowSettings:properties