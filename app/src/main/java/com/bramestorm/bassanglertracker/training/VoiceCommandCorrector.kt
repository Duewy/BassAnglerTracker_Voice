package com.bramestorm.bassanglertracker.training

private const val CLEANUP_THRESHOLD = 3  // pick a sensible max-distance

object VoiceCommandCorrector {
    // Simple Levenshtein
    private fun distance(a: String, b: String): Int {
        val dp = Array(a.length+1){ IntArray(b.length+1) }
        for(i in 0..a.length) dp[i][0]=i
        for(j in 0..b.length) dp[0][j]=j
        for(i in 1..a.length) for(j in 1..b.length){
            dp[i][j] = listOf(
                dp[i-1][j]+1,
                dp[i][j-1]+1,
                dp[i-1][j-1] + if(a[i-1]==b[j-1]) 0 else 1
            ).minOrNull()!!
        }
        return dp[a.length][b.length]
    }

    fun bestMatch(input: String, options: List<String>): String? {
        val cleaned = input.lowercase().trim()
        return options
            .map { it to distance(cleaned, it.lowercase()) }
            .minByOrNull { it.second }
            ?.takeIf { it.second < CLEANUP_THRESHOLD }
            ?.first
    }
}
