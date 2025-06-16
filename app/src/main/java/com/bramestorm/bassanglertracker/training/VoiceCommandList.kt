package com.bramestorm.bassanglertracker.training

//--------------------- List of Words and Phrases to Practice for User and Computer --------

object VoiceCommandList {
    val phraseList = mutableListOf(

                 //------📃 User Action Words / Phrases
        PracticePhrase("Catch", false),
        PracticePhrase("Caught", false),
        PracticePhrase("Log Entry", false),
        PracticePhrase("New Fish", false),
        PracticePhrase("Clear List", false),
        PracticePhrase("Correct an Error", false),
        PracticePhrase("Delete Last Catch", false),
        PracticePhrase("Edit Last Catch", false),
        PracticePhrase("Smallest Catch", false),
        PracticePhrase("Smallest Catch on the List", false),
        PracticePhrase("Largest Catch", false),
        PracticePhrase("Read Board List", false),
        PracticePhrase("Time Now", false),
        PracticePhrase("Time Left", false),
        PracticePhrase("Weight Total", false),

            //------🦈 Fish Species
        PracticePhrase("Bowfin", false),
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

        // Location for 📃 Commands Used
    val knownTournamentCommands = listOf(
        "add a catch",
        "save the catch",
        "edit last catch",
        "delete last catch",
        "save that",
        "tag catch",
        "record catch",
        "catch caddy",
        "query question",
        "list of tagged catches",
        "time remaining",
        "smallest catch on the list",
        "shortest catch on the list",
        "longest catch on the list",
        "largest catch on the list",
        "smallest catch for today",
        "time since last catch",
        "time remaining",
        "how many are on the list",
        "what is the total weight",
        "what is the total length",
        "log catch"
    )

                //todo this is for the misspoken words due to accents and dialects
    fun isKnownTournamentCommand(command: String): Boolean {
        return knownTournamentCommands.contains(command.lowercase())
    }
}



