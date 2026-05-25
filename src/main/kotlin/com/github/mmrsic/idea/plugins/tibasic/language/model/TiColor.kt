package com.github.mmrsic.idea.plugins.tibasic.language.model

class BadValue(message: String) : Exception(message)

enum class TiColor(
    val rgbValue: Int,
    val displayName: String,
) {
    Transparent(-1, "Transparent"),
    Black(0x000000, "Black"),
    MediumGreen(0x21C842, "Medium Green"),
    LightGreen(0x5EDC78, "Light Green"),
    DarkBlue(0x5455ED, "Dark Blue"),
    LightBlue(0x7D76FC, "Light Blue"),
    DarkRed(0xD4524D, "Dark Red"),
    Cyan(0x42EBF5, "Cyan"),
    MediumRed(0xFC5554, "Medium Red"),
    LightRed(0xFF7978, "Light Red"),
    DarkYellow(0xD4C154, "Dark Yellow"),
    LightYellow(0xE6CE80, "Light Yellow"),
    DarkGreen(0x21B03B, "Dark Green"),
    Magenta(0xC95BBA, "Magenta"),
    Gray(0xCCCCCC, "Gray"),
    White(0xFFFFFF, "White");

    companion object {
        fun at(index: Int): TiColor {
            val offset = index - 1
            if (offset !in entries.indices) throw BadValue("Color code=$index")
            return entries[offset]
        }
    }
}
