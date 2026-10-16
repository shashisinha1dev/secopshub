package com.secopshub.alert.dto;

public record AlertSubmissionResult(AlertResponse alert, boolean acceptedOrRetried, boolean publishSucceeded) { }
