#Candlesticks service

### In order to start application please execute first

1. Build with gradle, java 17 required
   1. gradle shadowJar
2. Copy jar file to root folder, where compose file is present
   1.  cp build/libs/candlesticks-1.1.3-all.jar candlesticks.jar
3. docker-compose up -d

## Solution technical description
1. Candlestick service is an app, that accepts instruments and quotes for instruments and calculates 1 min candlesticks
2. In order to aggregate candlesticks TimescaleDB is used
   1. It allows clear and concise configuration on how to aggregate quotes
   2. Two types of aggregates are used: continuous and realtime
   3. Continuous aggregates are used for background recalculation of candlesticks
   4. Realtime aggregates are used when candlesticks for particular instrument are request
4. Instruments and quotes are stored in instruments and quotes tables respectively.
