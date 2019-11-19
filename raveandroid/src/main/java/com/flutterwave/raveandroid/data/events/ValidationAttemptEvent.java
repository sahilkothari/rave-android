package com.flutterwave.raveandroid.data.events;

import static com.flutterwave.raveandroid.data.events.Event.EVENT_TITLE_VALIDATE;

public class ValidationAttemptEvent {
    Event event;

    public ValidationAttemptEvent(String paymentMethod) {
        event = new Event(EVENT_TITLE_VALIDATE, "Attempting " + paymentMethod + " Payment Validation");
    }

    public Event getEvent() {
        return event;
    }
}
