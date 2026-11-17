package com.htf.portal.dto;

/**
 * A picture ready to send to the browser: the raw bytes and their type (image/jpeg or image/png).
 */
public record PictureData(byte[] bytes, String contentType) {
}
