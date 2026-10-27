package com.examlens.service;

import java.time.LocalDate;

/** One dated slice of study capacity computed by the application from the student's declared availability. */
public record AvailabilityDay(LocalDate date, int minutes, String label) {
}
