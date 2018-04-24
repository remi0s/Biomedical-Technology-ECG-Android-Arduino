
#include <stdio.h>
#include <SoftwareSerial.h>
#define PROCESSING_VISUALIZER 1
#define SERIAL_PLOTTER  2
#define ANDROID  3
SoftwareSerial BTserial(6, 7); // Arduino RX- BluetoothTX     | Arduino TX -BluetoothRX

//  Variables
int pulsePin = 0;                 // Pulse Sensor purple wire connected to analog pin 0
int blinkPin = 13;                // pin to blink led at each beat
int fadePin = 5;                  // pin to do fancy classy fading blink at each beat
int fadeRate = 0;                 // used to fade LED on with PWM on fadePin

// Volatile Variables, used in the interrupt service routine!
volatile int BPM;                   // int that holds raw Analog in 0. updated every 2mS
volatile int Signal;                // holds the incoming raw data
volatile int IBI = 600;             // int that holds the time interval between beats! Must be seeded!
volatile boolean Pulse = false;     // "True" when User's live heartbeat is detected. "False" when not a "live beat".
volatile boolean QS = false;        // becomes true when Arduoino finds a beat.

static int outputType = ANDROID;


void setup(){
  pinMode(blinkPin,OUTPUT);         // pin that will blink to your heartbeat!
  pinMode(fadePin,OUTPUT);          // pin that will fade to your heartbeat!
  Serial.begin(115200);             // we agree to talk fast!
  BTserial.begin(115200);
  interruptSetup();                 // sets up to read Pulse Sensor signal every 2mS

}


//  Where the Magic Happens
void loop(){
  if (QS == true){     // A Heartbeat Was Found
                       // BPM and IBI have been Determined
                       // Quantified Self "QS" true when arduino finds a heartbeat
        fadeRate = 255;         // Makes the LED Fade Effect Happen
                                // Set 'fadeRate' Variable to 255 to fade LED with pulse
                                 // A Beat Happened, Output that to serial.
         serialOutputWhenBeatHappens();
        QS = false;                      // reset the Quantified Self flag for next time
  }
  
  serialOutput();
  delay(2);
  

  ledFadeToBeat();                      // Makes the LED Fade Effect Happen
                               //  take a break
}







void ledFadeToBeat(){
    fadeRate -= 15;                         //  set LED fade value
    fadeRate = constrain(fadeRate,0,255);   //  keep LED fade value from going into negative numbers!
    analogWrite(fadePin,fadeRate);          //  fade LED
  }
