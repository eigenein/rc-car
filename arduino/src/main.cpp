#include <Arduino.h>

// Define supported processors.
// -------------------------------------------------------------------------------------------------

#if !defined(__AVR_ATmega328P__)
#error "ATmega328 only is supported at the moment."
#endif

// Command definitions.
// -------------------------------------------------------------------------------------------------

const unsigned int BYTE_ORDER_MARK = 0xFEFF;

#define COMMAND_NOOP    0x01
#define COMMAND_STOP    0x02
#define COMMAND_MOVE    0x03

#define SPEED_FULL      255
#define SPEED_INVERSE   1

// Pins.
// -------------------------------------------------------------------------------------------------

#define PIN_RIGHT_1      2
#define PIN_RIGHT_SPEED  3
#define PIN_RIGHT_2      4
#define PIN_LEFT_SPEED   10
#define PIN_LEFT_1       5
#define PIN_LEFT_2       6
#define PIN_INDICATOR    13

// Main loop.
// -------------------------------------------------------------------------------------------------

int blockingRead() {
    while (!Serial.available());
    return Serial.read();
}

void stop() {
    // Block left wheel.
    digitalWrite(PIN_LEFT_1, LOW);
    digitalWrite(PIN_LEFT_2, LOW);
    analogWrite(PIN_LEFT_SPEED, SPEED_FULL);
    // Block right wheel.
    digitalWrite(PIN_RIGHT_1, LOW);
    digitalWrite(PIN_RIGHT_2, LOW);
    analogWrite(PIN_RIGHT_SPEED, SPEED_FULL);
}

bool receiveCommand() {
    switch (blockingRead()) {

        case COMMAND_NOOP:
            break;

        case COMMAND_MOVE:
            {
                int leftSpeed = blockingRead();
                int leftInverse = blockingRead();
                int rightSpeed = blockingRead();
                int rightInverse = blockingRead();
                // Left wheel.
                analogWrite(PIN_LEFT_SPEED, leftSpeed);
                digitalWrite(PIN_LEFT_1, leftInverse == SPEED_INVERSE ? HIGH : LOW);
                digitalWrite(PIN_LEFT_2, leftInverse == SPEED_INVERSE ? LOW : HIGH);
                // Right wheel.
                analogWrite(PIN_RIGHT_SPEED, rightSpeed);
                digitalWrite(PIN_RIGHT_1, rightInverse == SPEED_INVERSE ? HIGH : LOW);
                digitalWrite(PIN_RIGHT_2, rightInverse == SPEED_INVERSE ? LOW : HIGH);
            }
            break;

        case COMMAND_STOP:
            stop();
            break;

        default:
            return false;
    }
    return true;
}

long readVcc() {
    long vcc;

    ADMUX = _BV(REFS0) | _BV(MUX3) | _BV(MUX2) | _BV(MUX1);
    delay(1);
    ADCSRA |= _BV(ADSC);
    while (bit_is_set(ADCSRA, ADSC));
    vcc = ADCL;
    vcc |= ADCH << 8;
    vcc = 1126400L / vcc;

    return vcc;
}

void sendTelemetry() {
    digitalWrite(PIN_INDICATOR, HIGH);
    Serial.write((char*)&BYTE_ORDER_MARK, sizeof(BYTE_ORDER_MARK));
    long vcc = readVcc();
    Serial.write((char*)&vcc, sizeof(vcc));
    digitalWrite(PIN_INDICATOR, LOW);
}

// Entry point.
// -------------------------------------------------------------------------------------------------

void setup() {
    pinMode(PIN_LEFT_SPEED, OUTPUT);
    pinMode(PIN_LEFT_1, OUTPUT);
    pinMode(PIN_LEFT_2, OUTPUT);

    pinMode(PIN_RIGHT_SPEED, OUTPUT);
    pinMode(PIN_RIGHT_1, OUTPUT);
    pinMode(PIN_RIGHT_2, OUTPUT);

    pinMode(PIN_INDICATOR, OUTPUT);

    stop();
    Serial.begin(9600);
}

void loop() {
    if (receiveCommand()) {
        sendTelemetry();
    }
}
