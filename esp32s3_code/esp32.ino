#include <Wire.h>
#include <Adafruit_TCS34725.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEAdvertising.h>
#include <BLEBeacon.h>


#define SDA_PIN D4
#define SCL_PIN D5

#define LED_CTRL D6

#define PWR_BTN D0
#define SCAN_BTN D1

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 32
#define OLED_RESET -1

#define BLE_NAME = "cino"

Adafruit_TCS34725 tcs = Adafruit_TCS34725(TCS34725_INTEGRATIONTIME_50MS, TCS34725_GAIN_4X);
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
BLEAdvertising* pAdvertising;

volatile bool powerButtonPressed = false;
unsigned long pressStartTime = 0;
bool isSleeping = false;
bool isLedOn = true;

bool isFirstScan = true;
uint8_t scanIteration = 0;

uint16_t r, g, b, c;
float h, s, l;
uint8_t r8, g8, b8;


struct Colour {
  const char* name;
  float hue;
};

const Colour colours[8] = {
  { "Red",       11.0f },
  { "Green",       123.0f },
  { "Blue",         243.0f },
  { "Yellow",      60.0f },
  { "Orange",   27.0f },
  { "Light Blue",     181.0f },
  { "Magenta",     298.0f },
  { "Purple",       279.0f }
};

const char* findColour(float h, float s, float l) {
  if(s < 0.4) {
    if(l < 0.3) return "Black";
    if(l > 0.6) return "White";
    return "Gray";
  }

  float minDiff = 360.0f;
  const char* closest = "N/D";

  for (int i = 0; i < sizeof(colours) / sizeof(colours[0]); ++i) {
    float diff = fabs(h - colours[i].hue);
    if (diff > 180.0f) diff = 360.0f - diff;

    if (diff < minDiff) {
      minDiff = diff;
      closest = colours[i].name;
    }
  }

  return closest;
}


void rgbToHsl(uint8_t r, uint8_t g, uint8_t b, float &h, float &s, float &l) {
  float rf = r / 255.0, gf = g / 255.0, bf = b / 255.0;
  float max = fmax(rf, fmax(gf, bf)), min = fmin(rf, fmin(gf, bf));
  l = (max + min) / 2;

  if (max == min) {
    h = s = 0;
  } else {
    float d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

    if (max == rf)
      h = fmod(((gf - bf) / d + (gf < bf ? 6 : 0)), 6);
    else if (max == gf)
      h = ((bf - rf) / d + 2);
    else
      h = ((rf - gf) / d + 4);
    h *= 60;
  }
}

void showBootScreen() {
  display.clearDisplay();
  display.setTextSize(2);
  display.setCursor(0, 0);
  display.println("ColorShitmeter");
}

void printRGB(uint8_t r, uint8_t g, uint8_t b) {
  display.setCursor(0, 8);
  display.println("R: " + String(r));
  display.println("G: " + String(g));
  display.println("B: " + String(b));
}

void printHSL(float h, float s, float l) {
  display.setCursor(64, 8);
  display.print("H: " + String(h));
  display.setCursor(64, 16);
  display.print("S: " + String(s));
  display.setCursor(64, 24);
  display.print("L: " + String(l));
}

void advertiseBLE(uint8_t r, uint8_t g, uint8_t b) {
  pAdvertising->stop();
  char payload[32];
  snprintf(payload, sizeof(payload), "%d;%d;%d", r, g, b);

  BLEAdvertisementData advData;
  advData.setName(BLE_NAME);

  String fullPayload;
  fullPayload += (char)0xFF;
  fullPayload += (char)0xFF;
  fullPayload += payload;

  advData.setManufacturerData(fullPayload);

  pAdvertising->setAdvertisementData(advData);
  pAdvertising->start();
}

void IRAM_ATTR handlePowerButtonInterrupt() {
  powerButtonPressed = true;
  pressStartTime = millis();
}

void enterSleepState() {
  digitalWrite(LED_CTRL, LOW);
  digitalWrite(D6, LOW);
  isLedOn = false;
  isFirstScan = true;
  display.clearDisplay();
  display.display();

  pAdvertising->stop();
}

void exitSleepState() {
  digitalWrite(D6, HIGH);
  display.display();

  pAdvertising->start();
}

void setup() {
  pinMode(LED_CTRL, OUTPUT);
  pinMode(PWR_BTN, INPUT_PULLUP);
  pinMode(SCAN_BTN, INPUT_PULLUP);

  attachInterrupt(digitalPinToInterrupt(D0), handlePowerButtonInterrupt, FALLING);

  digitalWrite(LED_CTRL, (isLedOn) ? HIGH : LOW);
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);

  if (!tcs.begin()) {
    while (1);
  }

  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    for (;;);
  }

  BLEDevice::init(BLE_NAME);
  
  pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinInterval(0x20);
  pAdvertising->setMaxInterval(0x40); 

  display.setTextColor(SSD1306_WHITE);
  showBootScreen();

  delay(1000);
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0, 0);
}

void loop() {
  if (powerButtonPressed) {
    powerButtonPressed = false;

    unsigned long pressDuration = 0;

    while (digitalRead(D0) == LOW && pressDuration < 2850) {
      pressDuration = millis() - pressStartTime;
      delay(10);
    }

    if (pressDuration < 2750 && !isSleeping) {
      isLedOn = !isLedOn;
      digitalWrite(LED_CTRL, (isLedOn) ? HIGH : LOW);
    } else {
      isSleeping = !isSleeping;
      if (isSleeping) {
        enterSleepState();
      } else {
        exitSleepState();
      }
    }
  }

  if(!isSleeping) {
    display.clearDisplay();

    if(digitalRead(SCAN_BTN) == LOW) {
      display.setCursor(56, 0);
      display.print("SCANNING");

      if(scanIteration > 2) scanIteration = 0;
      else scanIteration++;

      for(uint8_t i = 0; i < scanIteration; i++) {
        display.print(".");
      }

      tcs.getRawData(&r, &g, &b, &c);

      float r_norm = r * 1.0 / c;
      float g_norm = g * 1.0 / c;
      float b_norm = b * 1.0 / c;

      r8 = (uint8_t)min(255.0f, r_norm * 255);
      g8 = (uint8_t)min(255.0f, g_norm * 255);
      b8 = (uint8_t)min(255.0f, b_norm * 255);

      rgbToHsl(r, g, b, h, s, l);

      isFirstScan = false;

      advertiseBLE(r8, g8, b8);
    }

    display.setCursor(0, 0);

    if(isFirstScan) {
      display.println("Keep pressed SCAN to start");
    } else {
      display.println(findColour(h, s, l));
      printRGB(r8, g8, b8);
      printHSL(h, s, l);
    }

    display.display();
  }

  delay(250);
}

