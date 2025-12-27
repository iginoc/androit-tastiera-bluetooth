package com.igino.tastiera

object HidReportConstants {

    // Gamepad Report Descriptor
    // Describes a gamepad with 16 buttons and a D-pad (as a hat switch).
    val GAMEPAD_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x05,       // Usage (Gamepad)
        0xA1.toByte(), 0x01,       // Collection (Application)
        0x85.toByte(), 0x01,       //   Report ID (1)
        0x09, 0x01,       //   Usage (Pointer)
        0xA1.toByte(), 0x00,       //   Collection (Physical)
        0x09, 0x30,       //     Usage (X)
        0x09, 0x31,       //     Usage (Y)
        0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7F,       //     Logical Maximum (127)
        0x75.toByte(), 0x08,       //     Report Size (8)
        0x95.toByte(), 0x02,       //     Report Count (2)
        0x81.toByte(), 0x02,       //     Input (Data,Var,Abs)
        0xC0.toByte(),            //   End Collection
        0x05, 0x09,       //   Usage Page (Button)
        0x19.toByte(), 0x01,       //   Usage Minimum (Button 1)
        0x29.toByte(), 0x10,       //   Usage Maximum (Button 16)
        0x15.toByte(), 0x00,       //   Logical Minimum (0)
        0x25.toByte(), 0x01,       //   Logical Maximum (1)
        0x75.toByte(), 0x01,       //   Report Size (1)
        0x95.toByte(), 0x10,       //   Report Count (16)
        0x81.toByte(), 0x02,       //   Input (Data,Var,Abs)
        0xC0.toByte()             // End Collection
    )

    const val REPORT_ID = 1
}
