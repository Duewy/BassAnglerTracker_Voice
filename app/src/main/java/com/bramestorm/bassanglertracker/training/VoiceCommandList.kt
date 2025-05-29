package com.bramestorm.bassanglertracker.training

//--------------------- List of Words and Phrases to Practice for User and Computer --------

object VoiceCommandList {
    val phraseList = mutableListOf(
                                                            //------ User Action Words / Phrases
        PracticePhrase("Save the Catch", false),
        PracticePhrase("Caught", false),
        PracticePhrase("Log Entry", false),
        PracticePhrase("New Fish", false),
        PracticePhrase("Clear List", false),

        PracticePhrase("Bowfin", false),        //------ Fish Species
        PracticePhrase("Bullhead", false),
        PracticePhrase("Carp", false),
        PracticePhrase("Cat Fish", false),
        PracticePhrase("Crappie", false),
        PracticePhrase("Gar Pike", false),
        PracticePhrase("Largemouth", false),
        PracticePhrase("Smallmouth", false),
        PracticePhrase("Spotted Bass", false),
        PracticePhrase("Muskie", false),
        PracticePhrase("Perch", false),
        PracticePhrase("Pike", false),
        PracticePhrase("Red Drum", false),
        PracticePhrase("Rock Bass", false),
        PracticePhrase("Sunfish", false),
        PracticePhrase("Walleye", false),
        PracticePhrase("White Bass", false)

    )

    val knownTournamentCommands = listOf(
        "add a catch",
        "save fish",
        "save that",
        "tag fish",
        "record fish",
        "catch caddy",
        "query question",
        "list of tagged catches",
        "time remaining",
        "smallest fish on the list",
        "largest fish on the list",
        "smallest fish for today",
        "time since last catch",
        "how many are on the list",
        "what is the weight",
        "log catch"
    )

    fun isKnownTournamentCommand(command: String): Boolean {
        return knownTournamentCommands.contains(command.lowercase())
    }
}



