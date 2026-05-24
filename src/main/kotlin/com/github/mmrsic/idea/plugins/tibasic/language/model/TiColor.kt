package com.github.mmrsic.idea.plugins.tibasic.language.model

class BadValue(message: String) : Exception(message)

enum class TiColor(val rgbValue: Int) {
    Transparent(-1),
    Black(0x000000),
    MediumGreen(0x21C842),
    LightGreen(0x5EDC78),
    DarkBlue(0x5455ED),
    LightBlue(0x7D76FC),
    DarkRed(0xD4524D),
    Cyan(0x42EBF5),
    MediumRed(0xFC5554),
    LightRed(0xFF7978),
    DarkYellow(0xD4C154),
    LightYellow(0xE6CE80),
    DarkGreen(0x21B03B),
    Magenta(0xC95BBA),
    Gray(0xCCCCCC),
    White(0xFFFFFF);

    companion object {
        fun at(index: Int): TiColor {
            val offset = index - 1
            if (offset !in entries.indices) throw BadValue("Color code=$index")
            return entries[offset]
        }
    }
}
