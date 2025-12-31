package com.bramestorm.bassanglertracker.training

//--------------------- List of Words and Phrases to Practice for User and Computer --------

object VoiceCommandList {

    val phraseList = mutableListOf(

        //🎣 Core Catch Commands
        PracticePhrase("Add a catch", false),
        PracticePhrase("Save the catch", false),
        PracticePhrase("Record catch", false),
        PracticePhrase("Log catch", false),
        PracticePhrase("Delete last catch", false),
        PracticePhrase("Edit last catch", false),

        //🧠 Question Mode Triggers
        PracticePhrase("Question", false),
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
        PracticePhrase("Time remaining", false),
        PracticePhrase("What is my position", false),

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
        PracticePhrase("Carp", false)
    )


        // Location for 📃 Commands Used
        private val supportedVoiceCommands = listOf(
            "add a catch",
            "save the catch",
            "record catch",
            "log catch",
            "edit last catch",
            "delete last catch",
            "question",
            "smallest catch",
            "largest catch",
            "smallest catch on the list",
            "largest catch on the list",
            "time since last catch",
            "time remaining",
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



