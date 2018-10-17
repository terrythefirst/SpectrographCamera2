#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <opencv2/highgui.hpp>
#include <android/bitmap.h>
#include <android/log.h>

char APPNAME[] = {"app"};
using namespace cv;

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_terry_spectrographcamera_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_terry_spectrographcamera2_MainActivity_getBitmapSpetrograph(JNIEnv *env,
                                                                             jobject instance,
                                                                             jobject bitmap) {

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Scaning getMagicColorBitmap");
    int ret;
    AndroidBitmapInfo info;
    void *pixels = 0;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,
                            "AndroidBitmap_getInfo() failed ! error=%d", ret);
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Bitmap format is not RGBA_8888!");
        return nullptr;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,
                            "AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    Mat mbgra(info.height, info.width, CV_8UC4, pixels);
    Mat dst = mbgra.clone();

    float data[dst.cols];
    int half = dst.rows/2;
    for(int k=0;k<dst.cols;k++){
//        dst.at<Vec3b>(half,k)[0]=dst.at<Vec3b>(half,k)[0];
//        dst.at<Vec3b>(half,k)[1]=dst.at<Vec3b>(half,k)[1];
//        dst.at<Vec3b>(half,k)[2]=dst.at<Vec3b>(half,k)[2];
        data[k] = dst.at<Vec3b>(half,k)[0]*0.3f+dst.at<Vec3b>(half,k)[1]*0.59f+dst.at<Vec3b>(half,k)[2]*0.11;
//        int cnt=0;
//        for(int i=k*10;i<dst.cols&&i<(k+1)*10;i++){
//            data[k] += dst.at<Vec3b>(half,i)[0]*0.3f+dst.at<Vec3b>(half,i)[1]*0.59f+dst.at<Vec3b>(half,i)[2]*0.11;
//            cnt++;
//        }

        //data[k]/=cnt;
    }
    jfloatArray result = env->NewFloatArray(dst.cols);
    env->SetFloatArrayRegion(result, 0, dst.cols, data);
    return result;

}