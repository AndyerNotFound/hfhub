package com.hfhub.android.data






object FilterData {

    
    val MODEL_TASKS = listOf(
        "text-generation", "image-text-to-text", "any-to-any", "text-to-image",
        "image-to-image", "image-to-text", "text-to-video", "text-to-speech",
        "text-to-audio", "audio-text-to-text", "image-to-video", "video-to-video",
        "text-to-3d", "image-to-3d", "audio-to-audio", "automatic-speech-recognition",
        "audio-classification", "voice-activity-detection", "image-classification",
        "object-detection", "image-segmentation", "depth-estimation", "video-classification",
        "zero-shot-image-classification", "zero-shot-object-detection", "mask-generation",
        "unconditional-image-generation", "keypoint-detection", "visual-question-answering",
        "document-question-answering", "video-text-to-text", "image-feature-extraction",
        "text-classification", "token-classification", "table-question-answering",
        "question-answering", "zero-shot-classification", "translation", "summarization",
        "feature-extraction", "fill-mask", "sentence-similarity", "text-ranking",
        "text-to-video", "tabular-classification", "tabular-regression",
        "time-series-forecasting", "reinforcement-learning", "robotics",
        "graph-machine-learning", "video-to-text", "text-retrieval"
    )

    
    val DATASET_TASKS = listOf(
        "text-classification", "text-generation", "question-answering", "summarization",
        "translation", "token-classification", "sentence-similarity", "fill-mask",
        "feature-extraction", "zero-shot-classification", "text-retrieval", "text-ranking",
        "image-classification", "object-detection", "image-segmentation", "depth-estimation",
        "image-to-text", "text-to-image", "image-to-image", "video-classification",
        "automatic-speech-recognition", "audio-classification", "text-to-speech",
        "audio-to-audio", "text-to-video", "tabular-classification", "tabular-regression",
        "time-series-forecasting", "reinforcement-learning", "robotics",
        "graph-machine-learning", "visual-question-answering", "document-question-answering",
        "video-text-to-text", "image-feature-extraction", "multiple-choice", "other"
    )

    
    val LIBRARIES = listOf(
        "transformers", "pytorch", "tensorflow", "jax", "safetensors", "diffusers",
        "gguf", "mlx", "transformers.js", "peft", "sentence-transformers", "timm",
        "setfit", "openvino", "sample-factory", "coreml", "litert", "nemo", "flair",
        "fastai", "espnet", "rust", "scikit-learn", "bertopic", "spacy", "fasttext",
        "open_clip", "executorch", "keras", "asteroid", "speechbrain", "llamafile",
        "paddlepaddle", "paddlenlp", "fairseq", "stanza", "pyannote-audio", "habana",
        "span-marker", "graphcore", "unity-sentis", "dduf", "univa", "tensorboard",
        "ml-agents", "adapters", "keras-hub", "stable-baselines3", "sentencepiece",
        "onnx", "mlx-lm", "vllm", "ollama"
    )

    
    val LANGUAGES = listOf(
        "en", "zh", "fr", "es", "de", "ja", "ko", "pt", "it", "ru",
        "ar", "hi", "th", "tr", "vi", "nl", "pl", "id", "sv", "uk",
        "ro", "fi", "cs", "fa", "bn", "da", "el", "he", "ms", "hu",
        "ne", "ta", "ur", "te", "bg", "ca", "no", "sw", "mr", "sl",
        "sk", "et", "sr", "gu", "lt", "hr", "lv", "my", "ml", "pa",
        "is", "kn", "gl", "km", "tl", "kk", "eu", "af", "am", "ka",
        "mn", "ha", "as", "lo", "hy", "mk", "cy", "yo", "si", "be",
        "az", "uz", "sq", "mt", "bs", "sd", "sa", "ga", "so", "la",
        "jv", "su", "ps", "ceb", "haw", "yi", "bo", "dv", "fy", "lb",
        "multilingual"
    )

    
    val LICENSES = listOf(
        "apache-2.0", "mit", "openrail", "creativeml-openrail-m", "cc-by-nc-4.0",
        "openrail++", "cc-by-4.0", "gemma", "llama2", "llama3", "llama3.1", "llama3.2",
        "llama4", "cc-by-nc-sa-4.0", "afl-3.0", "cc-by-sa-4.0", "bsd-3-clause",
        "gpl-3.0", "bigscience-bloom-rail-1.0", "artistic-2.0", "agpl-3.0",
        "bigcode-openrail-m", "cc", "cc-by-nc-nd-4.0", "cc0-1.0", "wtfpl",
        "unlicense", "bsl-1.0", "bsd-2-clause", "bsd", "gpl-2.0", "gpl",
        "cc-by-nc-3.0", "apple-amlr", "lgpl-3.0", "mpl-2.0", "osl-3.0",
        "cc-by-nc-nd-3.0", "cc-by-sa-3.0", "cc-by-2.0", "cc-by-3.0", "gpl-2.0",
        "lgpl-2.1", "lgpl-2.0", "isc", "zlib", "odbl", "pddl", "etalab-2.0",
        "cdla-permissive-2.0", "cdla-sharing-1.0", "epl-2.0", "epl-1.0",
        "eupl-1.1", "eupl-1.2", "c-uda", "gfdl", "ms-pl", "ofl-1.1", "postgresql",
        "nvidia-open-model-license", "other"
    )

    
    val PARAM_BUCKETS: List<Pair<String, String>> = listOf(
        "< 1B" to "max:1B",
        "1B – 10B" to "min:1B,max:10B",
        "10B – 100B" to "min:10B,max:100B",
        "100B – 500B" to "min:100B,max:500B",
        "> 500B" to "min:500B"
    )

    
    fun valuesFor(kind: String, category: String): List<String> = when (category) {
        CAT_TASK -> if (kind == "datasets") DATASET_TASKS else MODEL_TASKS
        CAT_LIBRARY -> LIBRARIES
        CAT_LANGUAGE -> LANGUAGES
        CAT_LICENSE -> LICENSES
        CAT_PARAMS -> PARAM_BUCKETS.map { it.first }
        else -> emptyList()
    }

    
    fun toFilter(kind: String, category: String, value: String): String = when (category) {
        CAT_TASK -> if (kind == "datasets") "task_categories:$value" else value
        CAT_LIBRARY -> value
        CAT_LANGUAGE -> "language:$value"
        CAT_LICENSE -> "license:$value"
        CAT_PARAMS -> "params:" + (PARAM_BUCKETS.firstOrNull { it.first == value }?.second ?: value)
        else -> value
    }

    
    fun parseFilter(kind: String, f: String): Pair<String, String>? = when {
        f.startsWith("task_categories:") -> CAT_TASK to f.removePrefix("task_categories:")
        f.startsWith("language:") -> CAT_LANGUAGE to f.removePrefix("language:")
        f.startsWith("license:") -> CAT_LICENSE to f.removePrefix("license:")
        f.startsWith("params:") -> {
            val spec = f.removePrefix("params:")
            CAT_PARAMS to (PARAM_BUCKETS.firstOrNull { it.second == spec }?.first ?: spec)
        }
        kind == "models" -> {
            
            
            if (MODEL_TASKS.contains(f)) CAT_TASK to f else CAT_LIBRARY to f
        }
        else -> CAT_TASK to f
    }

    
    fun categories(kind: String): List<String> = when (kind) {
        "models" -> listOf(CAT_TASK, CAT_LIBRARY, CAT_LANGUAGE, CAT_LICENSE, CAT_PARAMS)
        "datasets" -> listOf(CAT_TASK, CAT_LANGUAGE, CAT_LICENSE)
        else -> emptyList()
    }

    fun categoryLabel(cat: String): String = when (cat) {
        CAT_TASK -> "任务"
        CAT_LIBRARY -> "库"
        CAT_LANGUAGE -> "语言"
        CAT_LICENSE -> "许可"
        CAT_PARAMS -> "参数量"
        else -> cat
    }

    const val CAT_TASK = "task"
    const val CAT_LIBRARY = "library"
    const val CAT_LANGUAGE = "language"
    const val CAT_LICENSE = "license"
    const val CAT_PARAMS = "params"
}
