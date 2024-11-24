package com.example.autotaskapp.model;

import android.view.accessibility.AccessibilityEvent;

import java.io.Serializable;

public class RecordedAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private long eventTime;
    private AccessibilityEvent accessibilityEvent;

    public RecordedAction(long eventTime, AccessibilityEvent accessibilityEvent) {
        this.eventTime = eventTime;
        this.accessibilityEvent = accessibilityEvent;
    }

    public long getEventTime() {
        return eventTime;
    }

    public AccessibilityEvent getAccessibilityEvent() {
        return accessibilityEvent;
    }
}