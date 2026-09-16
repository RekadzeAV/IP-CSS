package com.company.ipcamera.server.stub.opencv

// Mat stub
class Mat {
    constructor() {}
    constructor(rows: Int, cols: Int, type: Int) {}
    constructor(size: Any, type: Int) {}
    fun put(row: Int, col: Int, vararg data: Double) {}
    fun get(row: Int, col: Int): DoubleArray = doubleArrayOf()
    fun rows(): Int = 0
    fun cols(): Int = 0
    fun size(): Any = Any()
    fun type(): Int = 0
    fun empty(): Boolean = true
    companion object {
        fun zeros(rows: Int, cols: Int, type: Int): Mat = Mat()
        fun ones(rows: Int, cols: Int, type: Int): Mat = Mat()
    }
}

// Point stub
data class Point(val x: Double = 0.0, val y: Double = 0.0) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
}

// Size stub
data class Size(val width: Double = 0.0, val height: Double = 0.0) {
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())
}

// Scalar stub
data class Scalar(val v0: Double = 0.0, val v1: Double = 0.0, val v2: Double = 0.0, val v3: Double = 0.0)

// Rect stub
data class Rect(val x: Int = 0, val y: Int = 0, val width: Int = 0, val height: Int = 0)

// RotatedRect stub
data class RotatedRect(
    val center: Point = Point(),
    val size: Size = Size(),
    val angle: Double = 0.0
)

// KeyPoint stub
data class KeyPoint(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val size: Double = 0.0,
    val angle: Double = -1.0,
    val response: Double = 0.0,
    val octave: Int = 0,
    val classID: Int = -1
)

// DMatch stub
data class DMatch(
    val queryIdx: Int = 0,
    val trainIdx: Int = 0,
    val imgIdx: Int = -1,
    val distance: Float = 0f
)

// TermCriteria stub
data class TermCriteria(
    val type: Int = 0,
    val maxIter: Int = 0,
    val epsilon: Double = 0.0
)

// Imgproc stub
object Imgproc {
    fun cvtColor(src: Mat, dst: Mat, code: Int, dstCn: Int = 0) {}
    fun Canny(input: Mat, edges: Mat, threshold1: Double, threshold2: Double, apertureSize: Int = 3, l2Gradient: Boolean = false) {}
    fun GaussianBlur(src: Mat, dst: Mat, ksize: Size, sigmaX: Double, sigmaY: Double = 0.0, borderType: Int = 0) {}
    fun bilateralFilter(src: Mat, dst: Mat, d: Int, sigmaColor: Double, sigmaSpace: Double, borderType: Int = 0) {}
    fun blur(src: Mat, dst: Mat, ksize: Size, anchor: Point = Point(), borderType: Int = 0) {}
    fun boxFilter(src: Mat, dst: Mat, ddepth: Int, ksize: Size, anchor: Point = Point(), normalize: Boolean = true, borderType: Int = 0) {}
    fun medianBlur(src: Mat, dst: Mat, ksize: Int) {}
    fun threshold(src: Mat, dst: Mat, thresh: Double, maxval: Double, type: Int) {}
    fun adaptiveThreshold(src: Mat, dst: Mat, maxValue: Double, adaptiveMethod: Int, thresholdType: Int, blockSize: Int, C: Double) {}
    fun resize(src: Mat, dst: Mat, dsize: Size, fx: Double = 0.0, fy: Double = 0.0, interpolation: Int = 0) {}
    fun pyrDown(src: Mat, dst: Mat) {}
    fun pyrUp(src: Mat, dst: Mat) {}
    fun equalizeHist(src: Mat, dst: Mat) {}
    fun matchTemplate(image: Mat, templ: Mat, result: Mat, method: Int, mask: Mat? = null) {}
    fun minMaxLoc(src: Mat, minVal: DoubleArray, maxVal: DoubleArray, minLoc: Point, maxLoc: Point, mask: Mat? = null) {}
    fun connectedComponents(image: Mat, labels: Mat, connectivity: Int = 8, ltype: Int = 0, ccltype: Int = 0): Int = 0
    fun connectedComponentsWithStats(image: Mat, labels: Mat, stats: Mat, centroids: Mat, connectivity: Int = 8, ltype: Int = 0): Int = 0
    fun floodFill(image: Mat, mask: Mat, seedPoint: Point, newVal: Scalar, rect: Rect, loDiff: Scalar = Scalar(), upDiff: Scalar = Scalar(), flags: Int = 0) {}
    fun findContours(image: Mat, contours: List<Mat>, hierarchy: Mat, mode: Int, method: Int, offset: Point = Point()) {}
    fun approxPolyDP(curve: Mat, approxCurve: Mat, epsilon: Double, closed: Boolean) {}
    fun fitEllipse(points: Mat): RotatedRect = RotatedRect()
    fun minAreaRect(points: Mat): RotatedRect = RotatedRect()
    fun minEnclosingCircle(points: Mat, center: Point, radius: FloatArray) {}
    fun line(img: Mat, pt1: Point, pt2: Point, color: Scalar, thickness: Int = 1, lineType: Int = 8, shift: Int = 0) {}
    fun rectangle(img: Mat, rect: Rect, color: Scalar, thickness: Int = 1, lineType: Int = 8, shift: Int = 0) {}
    fun circle(img: Mat, center: Point, radius: Int, color: Scalar, thickness: Int = 1, lineType: Int = 8, shift: Int = 0) {}
    fun putText(img: Mat, text: String, org: Point, fontFace: Int, fontScale: Double, color: Scalar, thickness: Int = 1, lineType: Int = 8, bottomLeftOrigin: Boolean = false) {}
    fun getTextSize(text: String, fontFace: Int, fontScale: Double, thickness: Int, baseLine: IntArray): Size = Size()
    fun getRotationMatrix2D(center: Point, angle: Double, scale: Double): Mat = Mat()
    fun warpAffine(src: Mat, dst: Mat, M: Mat, dsize: Size, flags: Int = 0, borderMode: Int = 0, borderValue: Scalar = Scalar()) {}
    fun matchShapes(contour1: Mat, contour2: Mat, method: Int, parameter: Double): Double = 0.0
    fun moments(curve: Mat, binaryImage: Boolean = false): Mat = Mat()
    fun moments(array: Array<Point>): Mat = Mat()
    fun pointPolygonTest(contour: Mat, pt: Point, measureDist: Boolean): Double = 0.0
    fun fitLine(points: Mat, line: Mat, distType: Int, param: Double, reps: Double, aeps: Double) {}
    fun watershed(image: Mat, markers: Mat) {}
    fun drawMatches(img1: Mat, keypoints1: List<KeyPoint>, img2: Mat, keypoints2: List<KeyPoint>, matches1to2: List<DMatch>, outImg: Mat, matchColor: Scalar = Scalar(), singlePointColor: Scalar = Scalar(), matchesMask: List<Boolean>? = null, flags: Int = 0) {}
    fun drawKeypoints(img: Mat, keypoints: List<KeyPoint>, outImg: Mat, color: Scalar = Scalar(), flags: Int = 0) {}
    fun drawContours(image: Mat, contours: List<Mat>, contourIdx: Int, color: Scalar, thickness: Int = 1, lineType: Int = 8, hierarchy: Mat? = null, maxLevel: Int = 2147483647, offset: Point = Point()) {}
    fun fillPoly(img: Mat, pts: List<List<Point>>, color: Scalar, lineType: Int = 8, shift: Int = 0, offset: Point = Point()) {}
    fun fillConvexPoly(img: Mat, pts: Array<Point>, color: Scalar, lineType: Int = 8, shift: Int = 0) {}
    fun add(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null, dtype: Int = -1) {}
    fun subtract(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null, dtype: Int = -1) {}
    fun multiply(src1: Mat, src2: Mat, dst: Mat, scale: Double = 1.0, dtype: Int = -1) {}
    fun divide(src1: Mat, src2: Mat, dst: Mat, scale: Double = 1.0, dtype: Int = -1) {}
    fun mixChannels(src: List<Mat>, dst: List<Mat>, fromTo: IntArray) {}
    fun split(m: Mat, mv: List<Mat>) {}
    fun merge(mv: List<Mat>, dst: Mat) {}
    fun dft(m: Mat, d: Mat, flags: Int = 0, nonzeroRows: Int = 0) {}
    fun idft(m: Mat, d: Mat, flags: Int = 0, nonzeroRows: Int = 0) {}
    fun mulSpectrums(a: Mat, b: Mat, c: Mat, flags: Int, conjugate: Boolean = false) {}
    fun phaseCorrelate(src1: Mat, src2: Mat, response: FloatArray, window: Mat? = null): Point = Point()
    fun norm(src1: Mat, normType: Int = 0, mask: Mat? = null): Double = 0.0
    fun gemm(src1: Mat, src2: Mat, alpha: Double, src3: Mat, beta: Double, dst: Mat, flags: Int = 0) {}
    fun transform(src: Mat, dst: Mat, m: Mat) {}
    fun perspectiveTransform(src: Mat, dst: Mat, vm: Mat) {}
    fun invert(src: Mat, dst: Mat, flags: Int = 0): Double = 0.0
    fun svd(src1: Mat, w: Mat, u: Mat, vt: Mat, flags: Int = 0) {}
    fun eigen(src1: Mat, eigenvalues: Mat, eigenvectors: Mat? = null) {}
    fun mulTransposed(src1: Mat, dst: Mat, a: Boolean, delta: Mat? = null, scale: Double = 1.0, dtype: Int = -1) {}
    fun kmeans(data: Mat, k: Int, bestLabels: Mat, criteria: TermCriteria, attempts: Int, flags: Int, centers: Mat? = null): Int = 0
    fun randn(dst: Mat, mean: Scalar, stddev: Scalar) {}
    fun randu(dst: Mat, low: Scalar, high: Scalar) {}
    fun repeat(src1: Mat, ny: Int, nx: Int, dst: Mat) {}
    fun exp(src1: Mat, dst: Mat) {}
    fun log(src1: Mat, dst: Mat) {}
    fun pow(src1: Mat, power: Double, dst: Mat) {}
    fun sqrt(src1: Mat, dst: Mat) {}
    fun absdiff(src1: Mat, src2: Mat, dst: Mat) {}
    fun bitwise_and(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null) {}
    fun bitwise_or(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null) {}
    fun bitwise_xor(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null) {}
    fun bitwise_not(src1: Mat, dst: Mat, mask: Mat? = null) {}
    fun countNonZero(src1: Mat): Int = 0
    fun mean(src1: Mat, mask: Mat? = null): Scalar = Scalar()
    fun meanStdDev(src1: Mat, mean: Scalar, stddev: Scalar, mask: Mat? = null) {}
    fun minMaxIdx(src1: Mat, minVal: DoubleArray, maxVal: DoubleArray, minIdx: IntArray, maxIdx: IntArray, mask: Mat? = null) {}
    fun reduce(src1: Mat, dst: Mat, dim: Int, rtype: Int, dtype: Int = -1) {}
    fun rowSum(src1: Mat): Mat = Mat()
    fun colSum(src1: Mat): Mat = Mat()
    fun mahalanobis(v1: Mat, v2: Mat, icovar: Mat): Double = 0.0
    fun checkRange(src1: Mat, quiet: Boolean = true, minVal: Double = Double.NEGATIVE_INFINITY, maxVal: Double = Double.POSITIVE_INFINITY): Double = 0.0
    fun findNonZero(src1: Mat, idx: Mat) {}
    fun convexHull(points: Mat, hull: Mat? = null, clockwise: Boolean = false, returnPoints: Boolean = true) {}
    fun convexityDefects(contour: Mat, hull: Mat, result: Mat? = null) {}
    fun isContourConvex(contour: Mat): Boolean = false
    fun cornerMinEigenVal(src: Mat, dst: Mat, blockSize: Int, k: Double = 0.04, borderType: Int = 0) {}
    fun cornerHarris(src: Mat, dst: Mat, blockSize: Int, ksize: Int, k: Double, borderType: Int = 0) {}
    fun goodFeaturesToTrack(src: Mat, corners: Mat, maxCorners: Int, qualityLevel: Double, minDist: Mat, mask: Mat? = null, blockSize: Int = 3, useHarris: Boolean = false, k: Double = 0.04) {}
    fun createHanningWindow(dst: Mat, winSize: Size, type: Int) {}
    fun polarToCart(magnitude: Mat, angle: Mat, x: Mat, y: Mat, angleInDegrees: Boolean = false) {}
    fun cartToPolar(x: Mat, y: Mat, magnitude: Mat, angle: Mat, angleInDegrees: Boolean = false) {}
    fun initCameraMatrix2D(objectPoints: List<Mat>, imagePoints: List<Mat>, imageSize: Size, cameraMatrix: Mat? = null): Mat = Mat()
    fun calibrateCamera(objectPoints: List<Mat>, imagePoints: List<Mat>, imageSize: Size, cameraMatrix: Mat, distCoeffs: Mat, rvecs: Mat, tvecs: Mat, flags: Int = 0, criteria: TermCriteria = TermCriteria()): Double = 0.0
    fun solvePnP(objectPoints: Mat, imagePoints: Mat, cameraMatrix: Mat, distCoeffs: Mat, rvec: Mat, tvec: Mat, useExtrinsicGuess: Boolean = false, flags: Int = 0) {}
    fun projectPoints(objectPoints: Mat, rvec: Mat, tvec: Mat, cameraMatrix: Mat, distCoeffs: Mat, imagePoints: Mat, jacobian: Mat? = null, aspectRatio: Double = 0.0) {}
    fun undistortPoints(src: Mat, dst: Mat, cameraMatrix: Mat, distCoeffs: Mat, R: Mat = Mat(), P: Mat = Mat()) {}
    fun remap(src: Mat, dst: Mat, map1: Mat, map2: Mat, interpolation: Int, borderMode: Int = 0, borderValue: Scalar = Scalar()) {}
    fun createBackgroundSubtractorMOG2(history: Int = 500, varThreshold: Double = 16.0, detectShadows: Boolean = true): Any = Any()
    fun createBackgroundSubtractorKNN(history: Int = 500, dist2Threshold: Double = 400.0, detectShadows: Boolean = true): Any = Any()
}

// Core stub
object Core {
    val REDUCE_AVG = 0
    val REDUCE_SUM = 1
    val REDUCE_SUM_SQ = 2
    val REDUCE_MIN = 3
    val REDUCE_MAX = 4
    val NORM_INF = 0
    val NORM_L1 = 1
    val NORM_L2 = 2
    val NORM_L2SQR = 5
    val NORM_HAMMING = 6
    val NORM_HAMMING2 = 7
    fun add(src1: Mat, src2: Mat, dst: Mat = Mat(), mask: Mat? = null, dtype: Int = -1) {}
    fun subtract(src1: Mat, src2: Mat, dst: Mat = Mat(), mask: Mat? = null, dtype: Int = -1) {}
    fun multiply(src1: Mat, src2: Mat, dst: Mat = Mat(), scale: Double = 1.0, dtype: Int = -1) {}
    fun divide(src1: Mat, src2: Mat, dst: Mat = Mat(), scale: Double = 1.0, dtype: Int = -1) {}
    fun scaleAdd(src1: Mat, alpha: Double, src2: Mat, dst: Mat) {}
    fun addWeighted(src1: Mat, alpha: Double, src2: Mat, beta: Double, gamma: Double, dst: Mat, dtype: Int = -1) {}
    fun batchDistance(d1: Mat, d2: Mat, dist: Mat, dtype: Int, nidx: Mat, normType: Int = 0, K: Int = 0, mask: Mat? = null, update: Int = 0, crosscheck: Boolean = false) {}
    fun compare(src1: Mat, src2: Mat, dst: Mat, cmpop: Int) {}
    fun inRange(src: Mat, lowerB: Scalar, upperB: Scalar, dst: Mat) {}
    fun mixChannels(src: List<Mat>, dst: List<Mat>, fromTo: IntArray) {}
    fun split(m: Mat, mv: List<Mat>) {}
    fun merge(mv: List<Mat>, dst: Mat) {}
    fun extractChannel(m: Mat, dst: Mat, coi: Int) {}
    fun insertChannel(m: Mat, dst: Mat, coi: Int) {}
    fun dft(m: Mat, d: Mat, flags: Int = 0, nonzeroRows: Int = 0) {}
    fun idft(m: Mat, d: Mat, flags: Int = 0, nonzeroRows: Int = 0) {}
    fun mulSpectrums(a: Mat, b: Mat, c: Mat, flags: Int, conjugate: Boolean = false) {}
    fun phaseCorrelate(src1: Mat, src2: Mat, response: FloatArray, window: Mat? = null): Point = Point()
    fun norm(src1: Mat, normType: Int = 0, mask: Mat? = null): Double = 0.0
    fun gemm(src1: Mat, src2: Mat, alpha: Double, src3: Mat, beta: Double, dst: Mat, flags: Int = 0) {}
    fun transform(src: Mat, dst: Mat, m: Mat) {}
    fun perspectiveTransform(src: Mat, dst: Mat, vm: Mat) {}
    fun completeSymm(m: Mat, lowerToUpper: Boolean = false) {}
    fun invert(src: Mat, dst: Mat, flags: Int = 0): Double = 0.0
    fun svd(src1: Mat, w: Mat, u: Mat, vt: Mat, flags: Int = 0) {}
    fun eigen(src1: Mat, eigenvalues: Mat, eigenvectors: Mat? = null) {}
    fun mulTransposed(src1: Mat, dst: Mat, a: Boolean, delta: Mat? = null, scale: Double = 1.0, dtype: Int = -1) {}
    fun pcaCompute(src1: Mat, mean: Mat, eigenvectors: Mat, maxComponents: Int = 0) {}
    fun pcaProject(src1: Mat, mean: Mat, eigenvectors: Mat, result: Mat? = null) {}
    fun pcaReconstruct(eigenvectors: Mat, mean: Mat, coords: Mat, result: Mat? = null) {}
    fun kmeans(data: Mat, k: Int, bestLabels: Mat, criteria: TermCriteria, attempts: Int, flags: Int, centers: Mat? = null): Int = 0
    fun randn(dst: Mat, mean: Scalar, stddev: Scalar) {}
    fun randu(dst: Mat, low: Scalar, high: Scalar) {}
    fun repeat(src1: Mat, ny: Int, nx: Int, dst: Mat) {}
    fun exp(src1: Mat, dst: Mat) {}
    fun log(src1: Mat, dst: Mat) {}
    fun pow(src1: Mat, power: Double, dst: Mat) {}
    fun sqrt(src1: Mat, dst: Mat) {}
    fun abs(src1: Mat, dst: Mat) {}
    fun absdiff(src1: Mat, src2: Mat, dst: Mat) {}
    fun bitwise_and(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null) {}
    fun bitwise_or(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null) {}
    fun bitwise_xor(src1: Mat, src2: Mat, dst: Mat, mask: Mat? = null) {}
    fun bitwise_not(src1: Mat, dst: Mat, mask: Mat? = null) {}
    fun countNonZero(src1: Mat): Int = 0
    fun mean(src1: Mat, mask: Mat? = null): Scalar = Scalar()
    fun meanStdDev(src1: Mat, mean: Scalar, stddev: Scalar, mask: Mat? = null) {}
    fun minMaxLoc(src1: Mat, minVal: DoubleArray, maxVal: DoubleArray, minLoc: Point, maxLoc: Point, mask: Mat? = null) {}
    fun minMaxIdx(src1: Mat, minVal: DoubleArray, maxVal: DoubleArray, minIdx: IntArray, maxIdx: IntArray, mask: Mat? = null) {}
    fun reduce(src1: Mat, dst: Mat, dim: Int, rtype: Int, dtype: Int = -1) {}
    fun rowSum(src1: Mat): Mat = Mat()
    fun colSum(src1: Mat): Mat = Mat()
    fun mahalanobis(v1: Mat, v2: Mat, icovar: Mat): Double = 0.0
    fun checkRange(src1: Mat, quiet: Boolean = true, minVal: Double = Double.NEGATIVE_INFINITY, maxVal: Double = Double.POSITIVE_INFINITY): Double = 0.0
    fun floodFill(image: Mat, mask: Mat, seedPoint: Point, newVal: Scalar, rect: Rect, loDiff: Scalar = Scalar(), upDiff: Scalar = Scalar(), flags: Int = 0) {}
    fun findNonZero(src1: Mat, idx: Mat) {}
    fun convexHull(points: Mat, hull: Mat? = null, clockwise: Boolean = false, returnPoints: Boolean = true) {}
    fun convexityDefects(contour: Mat, hull: Mat, result: Mat? = null) {}
    fun isContourConvex(contour: Mat): Boolean = false
    fun matchShapes(contour1: Mat, contour2: Mat, method: Int, parameter: Double): Double = 0.0
    fun pointPolygonTest(contour: Mat, pt: Point, measureDist: Boolean): Double = 0.0
    fun moments(array: Array<Point>): Mat = Mat()
    fun moments(m: Mat): Mat = Mat()
    fun watershed(image: Mat, markers: Mat) {}
    fun fitLine(points: Mat, line: Mat, distType: Int, param: Double, reps: Double, aeps: Double) {}
    fun fitEllipse(points: Mat): RotatedRect = RotatedRect()
    fun minAreaRect(points: Mat): RotatedRect = RotatedRect()
    fun minEnclosingCircle(points: Mat, center: Point, radius: FloatArray) {}
}

// Video stub
object Video {
    fun createBackgroundSubtractorMOG2(history: Int = 500, varThreshold: Double = 16.0, detectShadows: Boolean = true): Any = Any()
    fun createBackgroundSubtractorKNN(history: Int = 500, dist2Threshold: Double = 400.0, detectShadows: Boolean = true): Any = Any()
}

// Imgcodecs stub
object Imgcodecs {
    val IMREAD_COLOR = 1
    val IMREAD_GRAYSCALE = 0
    val IMREAD_UNCHANGED = -1
    val IMWRITE_JPEG_QUALITY = 1
    val IMWRITE_PNG_COMPRESSION = 16
    fun imdecode(buf: Mat, flags: Int): Mat = Mat()
    fun imencode(ext: String, img: Mat, buf: Mat, params: IntArray? = null): Boolean = true
    fun imwrite(filename: String, img: Mat, params: IntArray? = null): Boolean = true
    fun imread(filename: String, flags: Int = 1): Mat = Mat()
}

// Highgui stub
object Highgui {
    fun imshow(winname: String, mat: Mat) {}
    fun waitKey(delay: Int = 0): Int = 0
    fun destroyAllWindows() {}
    fun createButton(text: String?, callback: Any?, userData: Any? = null, buttontype: Int = 0, toggleState: Int = 0) {}
}

// Videoio stub
object Videoio {
    val CAP_ANY = 0
    val CAP_V4L = 1000
    val CAP_V4L2 = 1000
    val CAP_FIREWIRE = 2000
    val CAP_FIREWARE = 2000
    val CAP_IEEE1394 = 2000
    val CAP_DC1394 = 2000
    val CAP_CMU = 3000
    val CAP_STEREO = 4000
    val CAP_TYZX = 4000
    val CAP_TYZX_V3 = 4000
    val CAP_TYZX_V2 = 4001
    val CAP_QT = 5000
    val CAP_ANDROID = 6000
    val CAP_WINRT = 6500
    val CAP_IMAGES = 7000
    val CAP_ARAVIS = 8000
    val CAP_OPENCV_MJPEG = 9000
    val CAP_GIGANETIX = 9500
    val CAP_MSMF = 10000
    val CAP_DIRECTSHOW = 11000
    val CAP_PVAPI = 12000
    val CAP_OPENNI = 20000
    val CAP_OPENNI_ASUS = 20100
    val CAP_OPENNI2 = 20200
    val CAP_OPENNI2_ASUS = 20200
    val CAP_OPENNI_DEPTH_MAP = 0
    val CAP_OPENNI_POINT_CLOUD_MAP = 1
    val CAP_OPENNI_VGA_30HZ = 0
    val CAP_OPENNI_SVGA_30HZ = 1
    val CAP_OPENNI_XGA_30HZ = 2
    val CAP_OPENNI_VGA_60HZ = 4
    val CAP_OPENNI_XGA_60HZ = 5
    val CAP_INTEL_MFX = 19000
    val CAP_HYDRA = 20000
    val CAP_UBLOX = 21000
    val CAP_GPHOTO2 = 22000
    val CAP_GSTREAMER = 23000
    val CAP_FFMPEG = 24000
    val CAP_GIGE = 26000
    val CAP_API_PROP_MAX = 3000
    val CAP_PROP_POS_MSEC = 0
    val CAP_PROP_POS_FRAMES = 1
    val CAP_PROP_POS_AVI_RATIO = 2
    val CAP_PROP_FRAME_WIDTH = 3
    val CAP_PROP_FRAME_HEIGHT = 4
    val CAP_PROP_FPS = 5
    val CAP_PROP_FOURCC = 6
    val CAP_PROP_FRAME_COUNT = 7
    val CAP_PROP_FORMAT = 8
    val CAP_PROP_MODE = 9
    val CAP_PROP_BRIGHTNESS = 10
    val CAP_PROP_CONTRAST = 11
    val CAP_PROP_SATURATION = 12
    val CAP_PROP_HUE = 13
    val CAP_PROP_GAIN = 14
    val CAP_PROP_EXPOSURE = 15
    val CAP_PROP_CONVERT_RGB = 16
    val CAP_PROP_ROLL = 17
    val CAP_PROP_BUFFERSIZE = 18
    val CAP_PROP_AUTOFOCUS = 19
    val CAP_PROP_FOCUS = 20
    val CAP_PROP_ZOOM = 21
    val CAP_PROP_EXPOSURE_IRIS = 22
    val CAP_PROP_PAN = 23
    val CAP_PROP_TILT = 24
    val CAP_PROP_RECTIFICATION = 25
    val CAP_PROP_ISO_SPEED = 26
    val CAP_PROP_BACKLIGHT = 27
    val CAP_PROP_FRAME_COUNT_CAMERA = 28
    val CAP_PROP_CHANNEL = 1002
    val CAP_PROP_WHITE_BALANCE_BLUE_V = 1003
    val CAP_PROP_RAINBOW = 1004
    val CAP_PROP_SETTINGS = 1005
    fun VideoWriter_fourcc(vararg chars: Any): Int = 0

    fun createVideoWriter(): VideoWriter = VideoWriter()
    class VideoWriter {
        constructor() {}
        constructor(filename: String, fourcc: Int, fps: Double, frameSize: Size, isColor: Boolean = true) {}
        fun isOpened(): Boolean = false
        fun write(frame: Mat) {}
        fun release() {}
    }
    fun createVideoCapture(): VideoCapture = VideoCapture()
    class VideoCapture {
        constructor() {}
        constructor(filename: String) {}
        constructor(index: Int) {}
        fun isOpened(): Boolean = false
        fun read(frame: Mat): Boolean = false
        fun grab(): Boolean = false
        fun retrieve(frame: Mat, flag: Int = 0): Boolean = false
        fun set(propId: Int, value: Double): Boolean = false
        fun get(propId: Int): Double = 0.0
        fun release() {}
    }
}

// ML stub
object ML {
    val TRAIN_DATA = 0
    val TEST_DATA = 1
    val ALL_DATA = 2
    val SAMPLES_ORDER = 3
    val RAW_OUTPUT = 4
    val RULE_SAMPLE = 5
    val RULE_TRAIN = 6
    val SAMPLES_AS_ROWS = 0
    val SAMPLES_AS_COLS = 1
    val LOGISTIC_REGRESSION = 101
    val KNearest = 102
    val SVM = 103
    val DTrees = 104
    val ANN_MLP = 105
    val Boost = 106
    val RTrees = 107
    val TBRTrees = 108
    val RandomForest = 109
    val EM = 110
    val NormalBayesClassifier = 111
    val ParamSVM = 112
    fun isTrainData(): Boolean = false
    fun getSample(idx: Int): Mat = Mat()
    fun getTrainSamples(): Mat = Mat()
    fun getTestSamples(): Mat = Mat()
    fun getTrainLabel(idx: Int): Int = 0
    fun getTestLabel(idx: Int): Int = 0
    fun getTrainLabels(): Mat = Mat()
    fun getTestLabels(): Mat = Mat()
    fun getNTrainSamples(): Int = 0
    fun getNTestSamples(): Int = 0
    fun getNVars(): Int = 0
    fun getNSamples(): Int = 0
    fun getType(): Int = 0
    fun getTrainTestSplitRatio(): Double = 0.0
    fun getNTrainTestSplit(): Int = 0
    fun isSubset(): Boolean = false
    fun isRowSample(): Boolean = false
    fun isSubsetOfTrain(): Boolean = false
    fun isSubsetOfTest(): Boolean = false
    fun isSubsetOfTrainOrTest(): Boolean = false
}
