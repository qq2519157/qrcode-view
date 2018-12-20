# qrcode-view
[![Apache License 2.0][1]][2]
[![Release Version][5]][6]
[![API][3]][4]
[![PRs Welcome][7]][8]
 * ### 介绍
    这是一个基于ZXing的二维码扫描框架,kotlin代码编写,提供View作为扫描控件,适用于Activity和Fragment,样式可自定义.
***
 * ### 依赖方式
   #### Gradle:
    在你的module的build.gradle文件

    ```gradle
    implementation 'com.log1992:qrcodelib:0.0.1'
    ```
    ### Maven:
    ```maven
   <dependency>
    <groupId>com.log1992</groupId>
    <artifactId>qrcodelib</artifactId>
     <version>0.0.1</version>
     <type>pom</type>
    </dependency>
    ```
    ### Lvy
    ```lvy
    <dependency org='com.log1992' name='qrcodelib' rev='0.0.1'>
     <artifact name='qrcodelib' ext='pom' ></artifact>
    </dependency>
    ```
    ###### 如果Gradle出现compile失败的情况，可以在Project的build.gradle里面添加如下仓库地址：
    ```gradle
    allprojects {
    repositories {
        maven {url 'https://dl.bintray.com/qq2519157/maven'}
     }
    }
    ```
    ***
 * ### 引入的库：
    ```gradle
    compileOnly 'com.android.support:appcompat-v7:27.1.1'
    api 'com.google.zxing:core:3.3.3'
    ```
 * ### 使用方法
    ```xml
     <com.sencent.qrcodelib.CaptureView
            android:id="@+id/cv_qrcode"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:cv_text="@string/tips_scan_code"
            app:cv_textSize="14sp"
            app:cv_frameColor="#32b16c"
            app:cv_cornerColor="#32b16c"
            app:cv_topOffset="-200"
            app:cv_resultPointColor="@color/colorAccent"
            app:cv_textLocation="bottom"
            app:cv_laserColor="#32b16c"
    />
    ```
    ```text
        cv_text             扫描框底部提示文字
        cv_textSize         扫描框底部文字大小
        cv_maskColor        边框阴影区域颜色
        cv_frameColor       扫描区域边框颜色
        cv_cornerColor      四角颜色
        cv_laserColor       扫描线颜色
        cv_resultPointColor 结果点的颜色
        cv_topOffset        扫描框向上偏移量(在不同容器中,扫描区域位置可能有所偏移)
    ```
    ##### 独立Activity/Fragment中使用
    ```
    class TestActivity : AppCompatActivity(), ScanListener{
         override fun onFinish() {

            }

         override fun onSuccess(result: String) {

            }

         override fun onError(errorCode: Int) {

            }

        override fun onResume() {
            captureView?.onResume()
            super.onResume()
            }

        override fun onPause() {
            captureView?.onPause()
            super.onPause()
            }

         override fun onDestroy() {
            captureView?.onDestroy()
            super.onDestroy()
            }
    }
    ```

    ##### viewpager嵌套Fragment使用
    ```
        class ScanFragment : Fragment(), ScanListener {

             override fun onDestroy() {
                mCaptureView?.onDestroy()
                super.onDestroy()
                }

             override fun setUserVisibleHint(isVisibleToUser: Boolean) {
                 super.setUserVisibleHint(isVisibleToUser)
                    if (isVisibleToUser) {
                    mCaptureView?.onResume()
                    Log.i(TAG, "onResume")
                     } else {
                    mCaptureView?.onPause()
                    Log.i(TAG, "onPause")
                    }
                }

             override fun onFinish() {

            }

             override fun onSuccess(result: String) {

            }

             override fun onError(errorCode: Int) {

            }
        }
    ```
 * ### 注意事项
    * 必须实现Scanlistener接口,回调来处理扫描的结果
    * 需要自己处理相机权限,特别是6.0+动态权限处理
    * onError中的errorCode目前只有两个:CaptureView.CAMERA_ERROR和CaptureView.BEEP_MANAGER_ERROR,我的处理如下(仅供参考)
        ```
         override fun onError(errorCode: Int) {
             when (errorCode) {
                CaptureView.CAMERA_ERROR -> Toast.makeText(context,"相机加载异常，无法正常使用",Toast.LENGTH_SHORT).show()
                CaptureView.BEEP_MANAGER_ERROR -> {
            }
            else -> activity?.finish()
             }
            activity?.finish()
        }
        ```
 * ### 感谢
    [jenly1314](https://github.com/jenly1314)的[ZXingLite](https://github.com/jenly1314/ZXingLite)

    [ZXing](https://github.com/zxing/zxing)

[1]:https://img.shields.io/:license-apache-blue.svg
[2]:https://www.apache.org/licenses/LICENSE-2.0.html
[3]:https://img.shields.io/badge/API-15%2B-red.svg?style=flat
[4]:https://android-arsenal.com/api?level=15
[5]:https://img.shields.io/badge/release-0.0.1-red.svg
[6]:https://github.com/qq2519157/qrcode-view/releases
[7]:https://img.shields.io/badge/PRs-welcome-brightgreen.svg
[8]:https://github.com/qq2519157/qrcode-view/pulls