package com.disheveled.bills.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A receipt photo ready to go.
 *
 * [base64] is what gets sent, already downscaled; [preview] is the same image decoded for
 * the scanning screen so it doesn't have to be decoded twice.
 */
data class CapturedReceipt(
    val base64: String,
    val mediaType: String,
    val preview: ImageBitmap?,
)
