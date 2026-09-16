package com.company.ipcamera.server.stub.opencv

// Net stub - simplified to avoid overload conflicts
class Net {
    fun setInput(blob: Any, name: String? = null, scaleval: Double = 1.0, swapRB: Boolean = false, crop: Boolean = false, ddepth: Int = 0) {}
    fun forward(): Any = Any()
    fun forward(outputName: String): Any = Any()
    fun forward(outputBlobs: List<Any>, outBlobNames: List<String> = emptyList()) {}
    fun getLayerIndex(nameOrType: String): Int = 0
    fun getLayerShapes(layerId: Int, nInputs: Int = 1): List<Any> = emptyList()
    fun getMemoryShapes(inputs: List<List<Int>>): List<List<Int>> = emptyList()
    fun setPreferableBackend(preferableBackend: Int) {}
    fun setPreferableTarget(preferableTarget: Int) {}
    fun getUnconnectedOutLayers(): IntArray = intArrayOf()
    fun getUnconnectedOutLayersNames(): List<String> = emptyList()
    fun getBlobSize(net: Net, layerIdx: Int, blobIdx: Int): Any = Any()
    fun blobFromImage(image: Any, scalefactor: Double = 1.0, size: Any, mean: Any = Any(), swapRB: Boolean = false, crop: Boolean = false, ddepth: Int = 0): Any = Any()
    fun blobFromImages(images: List<Any>, scalefactor: Double = 1.0, size: Any, mean: Any = Any(), swapRB: Boolean = false, crop: Boolean = false, ddepth: Int = 0): Any = Any()
    fun readFromModelOptimizer(model_xml: String, model_bin: String, weights: String? = null, config: String? = null): Net = Net()
    fun readTensorFromONNX(filename: String): Any = Any()
    fun dnn_NMSBoxes(bboxes: List<Any>, scores: List<Float>, scoreThreshold: Float, nmsThreshold: Float, indices: List<Int>, eta: Double = -1.0, topK: Int = 0) {}
    fun dnn_NMSBoxesRotated(boxes: List<Any>, scores: List<Float>, scoreThreshold: Float, nmsThreshold: Float, indices: List<Int>) {}
    fun dnn_formatNMSBoxesByImageWidth(bboxes: List<Any>, scoreThreshold: Float, nmsThreshold: Float, imageWidth: Int): List<Any> = emptyList()
}

// Dnn object stub
object Dnn {
    fun readNetFromCaffe(prototxt: String, caffeModel: String? = null): Net = Net()
    fun readNetFromTensorflow(model: String, config: String? = null): Net = Net()
    fun readNetFromTorch(model: String, isBinary: Boolean = true, compute: Int = 0): Net = Net()
    fun readNetFromDarknet(configFile: String, darknetModel: String? = null, forceBackend: Int = 0, forceTarget: Int = 0): Net = Net()
    fun readNet(model: String, config: String? = null, framework: String? = null, backend: Int = 0, target: Int = 0): Net = Net()
    fun readNetFromONNX(model: String): Net = Net()
    fun blobFromImage(image: Any, scalefactor: Double = 1.0, size: Any, mean: Any = Any(), swapRB: Boolean = false, crop: Boolean = false, ddepth: Int = 0): Any = Any()
    fun blobFromImages(images: List<Any>, scalefactor: Double = 1.0, size: Any, mean: Any = Any(), swapRB: Boolean = false, crop: Boolean = false, ddepth: Int = 0): Any = Any()
    fun readFromModelOptimizer(model_xml: String, model_bin: String, weights: String? = null, config: String? = null): Net = Net()
    fun readTensorFromONNX(filename: String): Any = Any()
    fun NMSBoxes(bboxes: List<Any>, scores: List<Float>, scoreThreshold: Float, nmsThreshold: Float, indices: List<Int>, eta: Double = -1.0, topK: Int = 0) {}
    fun NMSBoxesRotated(boxes: List<Any>, scores: List<Float>, scoreThreshold: Float, nmsThreshold: Float, indices: List<Int>) {}
    fun formatNMSBoxesByImageWidth(bboxes: List<Any>, scoreThreshold: Float, nmsThreshold: Float, imageWidth: Int): List<Any> = emptyList()
}

// Layer stub
class Layer {
    fun getLayerType(): String = ""
    fun getName(): String = ""
    fun getBaseLayerType(): String = ""
    fun getFlops(): Double = 0.0
    fun getMemoryAfterForward(): Int = 0
    fun getMemoryBeforeForward(): Int = 0
    fun getMemoryFootprint(): Int = 0
    fun getMemoryFootprintBottom(): Int = 0
    fun getMemoryFootprintTop(): Int = 0
    fun getMemoryFootprintWeight(): Int = 0
    fun getMemoryFootprintWorkspace(): Int = 0
    fun getMemoryFootprintTotal(): Int = 0
    fun getMemoryFootprintActivation(): Int = 0
    fun getMemoryFootprintBias(): Int = 0
    fun getMemoryFootprintConstant(): Int = 0
}