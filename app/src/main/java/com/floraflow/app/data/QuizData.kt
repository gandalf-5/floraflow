package com.floraflow.app.data

data class QuizData(
    val question: String,
    val options: List<String>,
    val correct: Int,
    val explanation: String,
    val dateKey: String
)
