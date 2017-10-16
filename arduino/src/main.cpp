#include <Arduino.h>

// Define supported processors.
// -------------------------------------------------------------------------------------------------

#if !defined(__AVR_ATmega328P__)
#error "ATmega328 only is supported at the moment."
#endif

// Command definitions.
// -------------------------------------------------------------------------------------------------

const unsigned int BYTE_ORDER_MARK = 0xFEFF;

#define MESSAGE_NOOP    0x01
#define MESSAGE_BRAKE   0x02
#define MESSAGE_MOVE    0x03

#define SPEED_FULL      255
#define FLAG_INVERSE    1

// Pins.
// -------------------------------------------------------------------------------------------------

#define PIN_LEFT_OUTPUT   2
#define PIN_LEFT_PWM      3
#define PIN_RIGHT_OUTPUT  4
#define PIN_RIGHT_PWM     5
#define PIN_INDICATOR     13

// Main loop.
// -------------------------------------------------------------------------------------------------

int blockingRead() {
    while (!Serial.available());
    return Serial.read();
}

void brake() {
    digitalWrite(PIN_LEFT_OUTPUT, HIGH);
    digitalWrite(PIN_LEFT_PWM, HIGH);

    digitalWrite(PIN_RIGHT_OUTPUT, HIGH);
    digitalWrite(PIN_RIGHT_PWM, HIGH);
}

bool receiveCommand() {
    switch (blockingRead()) {

        case MESSAGE_NOOP:
            break;

        case MESSAGE_MOVE:
            {
                int leftSpeed = blockingRead();
                int leftInverse = blockingRead();
                int rightSpeed = blockingRead();
                int rightInverse = blockingRead();
                // Left wheel.
                digitalWrite(PIN_LEFT_OUTPUT, leftInverse != FLAG_INVERSE ? LOW : HIGH);
                analogWrite(PIN_LEFT_PWM, leftInverse != FLAG_INVERSE ? leftSpeed : 255 - leftSpeed);
                // Right wheel.
                digitalWrite(PIN_RIGHT_OUTPUT, rightInverse != FLAG_INVERSE ? LOW : HIGH);
                analogWrite(PIN_RIGHT_PWM, rightInverse != FLAG_INVERSE ? rightSpeed : 255 - rightSpeed);
            }
            break;

        case MESSAGE_BRAKE:
            brake();
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
    pinMode(PIN_LEFT_OUTPUT, OUTPUT);
    pinMode(PIN_LEFT_PWM, OUTPUT);

    pinMode(PIN_RIGHT_OUTPUT, OUTPUT);
    pinMode(PIN_RIGHT_PWM, OUTPUT);

    pinMode(PIN_INDICATOR, OUTPUT);

    brake();
    Serial.begin(9600);
}

void loop() {
    if (receiveCommand()) {
        sendTelemetry();
    }
}
