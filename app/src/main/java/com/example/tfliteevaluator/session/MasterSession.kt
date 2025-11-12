package com.example.tfliteevaluator.session

object MasterSession {

    var selectedModel: String? = null
        private set

    var evaluationResult: String? = null
        private set

    fun setSelectedModel(modelName: String) {
        selectedModel = modelName
    }

    fun setEvaluationResult(result: String) {
        evaluationResult = result
    }

    fun clear() {
        selectedModel = null
        evaluationResult = null
    }
}
