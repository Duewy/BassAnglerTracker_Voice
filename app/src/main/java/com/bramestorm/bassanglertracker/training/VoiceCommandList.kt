package com.bramestorm.bassanglertracker.training

//--------------------- List of Words and Phrases to Practice for User and Computer --------

object VoiceCommandList {

    val phraseList = mutableListOf(

        //🎣 Core Catch Commands
        PracticePhrase("Add a catch", false),
        PracticePhrase("Save the catch", false),

        // ✅ Confirmation Commands (must include "Over" — matches real VCC flow)
        PracticePhrase("Yes Over", false),
        PracticePhrase("No Over", false),
        PracticePhrase("Cancel Over", false),

        //🧠 Question Mode Triggers
        PracticePhrase("Question Over", false),
        PracticePhrase("What is the total weight", false),
        PracticePhrase("What is the total length", false),
        PracticePhrase("Smallest catch", false),
        PracticePhrase("Largest catch", false),
        PracticePhrase("Smallest fish", false),
        PracticePhrase("Largest fish", false),
        PracticePhrase("Smallest catch on the list", false),
        PracticePhrase("Largest catch on the list", false),
        PracticePhrase("Smallest fish on the list", false),
        PracticePhrase("Largest fish on the list", false),
        PracticePhrase("Time since last catch", false),
        PracticePhrase("How many", false),
        PracticePhrase("Average", false),
        PracticePhrase("Position", false),
        PracticePhrase("What time is it", false),

        // 🎨 Clip Colors (tournament mode)
        PracticePhrase("Blue clip", false),
        PracticePhrase("Yellow clip", false),
        PracticePhrase("Green clip", false),
        PracticePhrase("Orange clip", false),
        PracticePhrase("White clip", false),
        PracticePhrase("Red clip", false),

        // 🔢 Number Phrases (weight/length examples)
        PracticePhrase("3 pounds 12 ounces", false),
        PracticePhrase("5 point 26 pounds", false),
        PracticePhrase("4 point 15 kilograms", false),
        PracticePhrase("18 inches 2 quarters", false),
        PracticePhrase("42 point 5 centimeters", false),

        //🐟 Species Examples (should match testable voice input)
        PracticePhrase("Largemouth", false),
        PracticePhrase("Smallmouth", false),
        PracticePhrase("Crappie", false),
        PracticePhrase("Walleye", false),
        PracticePhrase("Perch", false),
        PracticePhrase("Catfish", false),
        PracticePhrase("Pike", false),
        PracticePhrase("Sunfish", false),
        PracticePhrase("Rock Bass", false),
        PracticePhrase("White Bass", false),
        PracticePhrase("Spotted Bass", false),
        PracticePhrase("Gar Pike", false),
        PracticePhrase("Bowfin", false),
        PracticePhrase("Bullhead", false),
        PracticePhrase("Red Drum", false),
        PracticePhrase("Muskie", false),
        PracticePhrase("Carp", false),

        // 🗣️ Full Catch Phrases (combines weight + species + color — real VCC input)
        PracticePhrase("3 pounds 8 ounces largemouth blue", false),
        PracticePhrase("2 pounds 14 ounces smallmouth green", false),
        PracticePhrase("18 inches 3 quarters walleye yellow", false),
        PracticePhrase("45 point 2 centimeters pike orange", false)
    )


    // Location for 📃 Commands Used — matches what TournamentVoiceHandler + FunDayVoiceHandler listen for
    private val supportedVoiceCommands = listOf(
        "add a catch",
        "save the catch",
        "record catch",
        "log catch",
        "edit last catch",
        "delete last catch",
        "yes over",
        "no over",
        "cancel over",
        "question over",
        "smallest catch",
        "largest catch",
        "smallest catch on the list",
        "largest catch on the list",
        "time since last catch",
        "how many",
        "average",
        "position",
        "what time is it",
        "what is the total weight",
        "what is the total length",
        "what is my position"
    )


    //todo this is for the misspoken words due to accents and dialects
    fun isKnownTournamentCommand(command: String): Boolean {
        return supportedVoiceCommands.contains(command.lowercase())
    }

    fun isKnownPhrase(command: String): Boolean {
        return phraseList.any { it.text.equals(command, ignoreCase = true) }
    }

}