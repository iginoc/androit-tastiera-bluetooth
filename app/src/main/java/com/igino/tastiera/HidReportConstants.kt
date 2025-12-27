package com.igino.tastiera

object HidReportConstants {

    val GAMEPAD_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x05,       // Usage (Gamepad)
        0xA1.toByte(), 0x01,       // Collection (Application)
        0x85.toByte(), 0x01,       //   Report ID (1)

        // Buttons (16 bits)
        0x05, 0x09,       //   Usage Page (Button)
        0x19.toByte(), 0x01,       //   Usage Minimum (Button 1)
        0x29.toByte(), 0x10,       //   Usage Maximum (Button 16)
        0x15.toByte(), 0x00,       //   Logical Minimum (0)
        0x25.toByte(), 0x01,       //   Logical Maximum (1)
        0x75.toByte(), 0x01,       //   Report Size (1)
        0x95.toByte(), 0x10,       //   Report Count (16)
        0x81.toByte(), 0x02,       //   Input (Data,Var,Abs) -> 2 bytes

        // D-Pad (Hat switch)
        0x05, 0x01,       //   Usage Page (Generic Desktop)
        0x09, 0x39,       //   Usage (Hat switch)
        0x15.toByte(), 0x00,       //   Logical Minimum (0)
        0x25.toByte(), 0x07,       //   Logical Maximum (7)
        0x35.toByte(), 0x00,       //   Physical Minimum (0)
        0x46, 0x3B, 0x01, //   Physical Maximum (315)
        0x65.toByte(), 0x14,       //   Unit (Eng Rot: Degree)
        0x75.toByte(), 0x04,       //   Report Size (4)
        0x95.toByte(), 0x01,       //   Report Count (1)
        0x81.toByte(), 0x42,       //   Input (Data,Var,Abs,Null) -> 0.5 byte

        // Padding to make the D-Pad a full byte
        0x75.toByte(), 0x04,       //   Report Size (4)
        0x95.toByte(), 0x01,       //   Report Count (1)
        0x81.toByte(), 0x01,       //   Input (Const,Array,Abs) -> 0.5 byte

        // Analog Joysticks (4 bytes)
        0x05, 0x01,       //   Usage Page (Generic Desktop)
        0x09, 0x30,       //   Usage (X)
        0x09, 0x31,       //   Usage (Y)
        0x09, 0x32,       //   Usage (Z)
        0x09, 0x35,       //   Usage (Rz)
        0x15.toByte(), 0x81.toByte(), //   Logical Minimum (-127)
        0x25.toByte(), 0x7F,       //   Logical Maximum (127)
        0x75.toByte(), 0x08,       //   Report Size (8)
        0x95.toByte(), 0x04,       //   Report Count (4)
        0x81.toByte(), 0x02,       //   Input (Data,Var,Abs)

        0xC0.toByte()        // End Collection
    )

    const val REPORT_ID = 1
}
