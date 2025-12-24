package com.norrisjackson.jsnippets.data;

/**
 * Status of email processing attempts.
 */
public enum ProcessingStatus {
    SUCCESS,           // Snippet created successfully
    USER_NOT_FOUND,    // Sender email not in users table
    PROCESSING_ERROR,  // Error during processing
    EMPTY_CONTENT      // Email had no usable content
}

