# CameraTest

此分支上的CameraTest在camera_mirror的基础上进行了改进，使用MediaCodec对需要发送的视频帧进行编解码。体验如下：

![](http://7xprgn.com1.z0.glb.clouddn.com/IMG_8514aa.JPG)

使用MediaCodec框架的好处多多，视频流更加流畅且数据包大大缩小。

## How to use

 1. 确保双方手机在同一局域网
 2. 双方都输入对方的ip，接着一方点击initiator，一方点击responder
 3. 实时传输预览，效果类似QQ视频