package com.dyman.show3dmodel.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.dyman.show3dmodel.bean.ModelObject;
import com.dyman.show3dmodel.utils.MatrixState;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by dyman on 16/7/25.
 */
public class ModelView extends GLSurfaceView {

    private static final String TAG = "ModelView";

    public final float TOUCH_SCALE_FACTOR = 180.0f/320;//角度缩放比例
    public float mPreviousY;//上次的触控位置Y坐标
    public float mPreviousX;//上次的触控位置X坐标
    public float currScale = 1f;//初始缩放比例
    public float previousScale = 1f;//上次的缩放比例
    public float changeScale = 1f;//缩放改变的比例
    public float pinchStartDistance = 0.0f;

    /**
     * 触摸模式相关
     */
    public static final int TOUCH_NONE = 0;//无
    public static final int TOUCH_ZOOM = 1;//缩放
    public static final int TOUCH_DRAG = 2;//拖拽
    public int touchMode = TOUCH_NONE;

    /**
     * 打印进度计算相关
     */
    private float maxSize;  //有疑问，应该直接用x轴的数据就行了
    private float initHeight;
    public float printProgress = 0;


    public ModelRenderer mRenderer;

    public ModelView(Context context, ModelObject modelObject) {
        super(context);
        //设置OpenGL的版本号2.0
        this.setEGLContextClientVersion(2);
        mRenderer = new ModelRenderer(modelObject);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        //初始化模型的显示大小，自适应
        initModelScale(modelObject);
    }

    /**
     *  初始化模型缩放倍数，使其完全显示在屏幕上
     * @param modelObject
     */
    private void initModelScale(ModelObject modelObject) {
        float maxSize = modelObject.maxX - modelObject.minX;
        if (maxSize < modelObject.maxY-modelObject.minY){
            maxSize = modelObject.maxY-modelObject.minY;
        }
        if (maxSize < modelObject.maxZ-modelObject.minZ){
            maxSize = modelObject.maxZ-modelObject.minZ;
        }
        Log.e(TAG, "initModelSize: --------------------------size="+maxSize);
        if (maxSize > 20f) {    //大于20f，缩小模型
            currScale = 18f/maxSize;
        } else if(maxSize < 10f) {  //小于10f，放大模型
            currScale = 15f/maxSize;
        }
        Log.e(TAG, "initModelSize: --------------------------currScale="+currScale);
    }


    /**
     * 渲染器，真正绘制模型的类
     */
    class ModelRenderer implements Renderer{
        public float yAngle;
        public float zAngle;
        private ModelObject modelObject;

        public ModelRenderer(ModelObject modelObject) { this.modelObject = modelObject; }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            //设置平模背景色RGBA
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            //打开深度检测
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            // 打开背面剪裁
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            // 初始化变换矩阵
            MatrixState.setInitStack();
            // 初始化光源位置
            MatrixState.setLightLocation(60, 15 ,30);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            //设置视窗大小及位置
            GLES20.glViewport(0, 0, width, height);
            //计算GLSurfaceView的宽高比
            float ratio = (float) width/height;
            //调用次方法计算产生透视投影矩阵
            MatrixState.setProjectFrustum(-ratio, ratio, -1, 1, 2, 100);
            //调用此方法产生摄像机9参数位置矩阵
            MatrixState.setCamera(0,0,0, 0f,0f,-1f, 0f,1.0f,0.0f);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            //  清除深度缓冲与颜色缓冲
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            //坐标系推远
            MatrixState.pushMatrix();
            MatrixState.translate(0, -2f, -25f);
            // 绕Y轴、Z轴旋转
            MatrixState.rotate(yAngle, 0, 1, 0);
            MatrixState.rotate(zAngle, 1, 0, 0);
            MatrixState.scale(currScale,currScale,currScale);

            //绘制模型
            if (modelObject != null){
                //  绘制基础模型
                if (modelObject.drawWay == ModelObject.DRAW_MODEL){
                    modelObject.drawSelf(ModelView.this);

                //  绘制带进度显示的模型
                } else if(modelObject.drawWay == ModelObject.DRAW_PROGRESS){
                    //因为两种格式的模型的中心点坐标不同，分开处理
                    if (modelObject.modelType.equals("stl")) {
                        float maxHeight = modelObject.maxY-modelObject.minY;
                        float finishHeight = (maxHeight) * printProgress - maxHeight/2;
                        float clipPlaneUp[] = {0, 1, 0, modelObject.maxY};
                        float clipPlaneDown[] = {0, -1, 0, finishHeight};
                        modelObject.drawSelfWithProgress(clipPlaneDown, GLES20.GL_TRIANGLES, ModelView.this);
                        modelObject.drawSelfWithProgress(clipPlaneUp, GLES20.GL_LINE_LOOP, ModelView.this);

                    } else if (modelObject.modelType.equals("obj")){
                        float finishHeight = (modelObject.maxY-modelObject.minY) * printProgress + modelObject.minY;
                        float clipPlaneUp[] = {0, 1, 0, modelObject.maxY};
                        float clipPlaneDown[] = {0, -1, 0, finishHeight};
                        modelObject.drawSelfWithProgress(clipPlaneDown, GLES20.GL_TRIANGLES, ModelView.this);
                        modelObject.drawSelfWithProgress(clipPlaneUp, GLES20.GL_LINE_LOOP, ModelView.this);
                    }
                }
            }
            MatrixState.popMatrix();
        }
    }


}